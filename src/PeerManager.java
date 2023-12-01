package src;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;
import java.util.HashSet;


public class PeerManager {
	private String localPeerID;
	private RemotePeerInfo localConfiguration;
	private HashMap<String, RemotePeerInfo> allPeerDetailsMap;
	private ArrayList<String> allPeerList;
	private volatile HashMap<String, PeerController> connectedPeers;
	private volatile HashMap<String, Thread> connectedPeerThreads;
	private volatile ServerSocket localChannel;
	private PeerServer localServer;
	private CommonConfiguration commonConfig;
	private PeerInfoConfig peerConfigurationInfo;
	private volatile ClientLogger clientLogger;
	private volatile HashMap<String, BitSet> chunkAvailabilityMap;
	private volatile String[] requestedInfo;
	private volatile Set<String> unChokedPeerList;
	private volatile Set<String> interestedPeerList;
	private volatile String optUnchockedPeer;
	private int chunkCount;
	private volatile RandomAccessFile filePointer;
	private volatile ChokeController chokeController;
	private volatile OptimisticUnchokeHandler optimisticUnChokeController;
	private volatile TerminateHandler cleanupHandler;
	private volatile HashMap<String, Integer> chunkDownloadRateInfo;
	private Thread localServerThread;
	private volatile Boolean localFileDownloadComplete;

	private Random randObj = new Random();

	public PeerManager(String peerID) {
		this.localPeerID = peerID;
		this.allPeerDetailsMap = new HashMap<>();
		this.chunkAvailabilityMap = new HashMap<>();
		this.allPeerList = new ArrayList<>();
		this.connectedPeers = new HashMap<>();
		this.connectedPeerThreads = new HashMap<>();
		this.commonConfig = new CommonConfiguration();
		this.peerConfigurationInfo = new PeerInfoConfig();
		this.clientLogger = new ClientLogger(peerID);
		this.localFileDownloadComplete = false;
		this.unChokedPeerList = new HashSet<>();
		this.interestedPeerList = new HashSet<>();
		this.initializeLocalPeer();
		this.chokeController = new ChokeController(this);
		this.chunkDownloadRateInfo = new HashMap<>();
		this.optimisticUnChokeController = new OptimisticUnchokeHandler(this);
		this.cleanupHandler = new TerminateHandler(this);
		this.chokeController.startJob();
		this.optimisticUnChokeController.startJob();
	}

	public void initializeLocalPeer() {
		try {
			this.commonConfig.InitilizeCommonConfiguration();;
			this.peerConfigurationInfo.loadConfigFile();
			this.chunkCount = this.calcChunkCount();
			this.requestedInfo = new String[this.chunkCount];
			this.localConfiguration = this.peerConfigurationInfo.getPeerConfig(this.localPeerID);
			this.allPeerDetailsMap = this.peerConfigurationInfo.getPeerInfoMap();
			this.allPeerList = this.peerConfigurationInfo.getPeerList();
			String fileLocation = "peer_" + this.localPeerID;
			File fileHandler = new File(fileLocation);
			fileHandler.mkdir();
			String filename = fileLocation + "/" + getSourceFileName();
			fileHandler = new File(filename);
			if (!hasSourceFile()) {
				fileHandler.createNewFile();
			}
			this.filePointer = new RandomAccessFile(fileHandler, "rw");
			if (!hasSourceFile()) {
				this.filePointer.setLength(this.getSourceFileSize());
			}
			this.setChunkAvailabilityMap();
			this.startLocalCoordinator();
			this.connectToOtherPeers();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void startLocalCoordinator() {
		try {
			this.localChannel = new ServerSocket(this.localConfiguration.peerPort);
			this.localServer = new PeerServer(this.localPeerID, this.localChannel, this);
			this.localServerThread = new Thread(this.localServer);
			this.localServerThread.start();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void connectToOtherPeers() {
		try {
			Thread.sleep(5000);
			for (String peerControllerID : this.allPeerList) {
				if (peerControllerID.equals(this.localPeerID)) {
					break;
				}
				else {
					RemotePeerInfo peer = this.allPeerDetailsMap.get(peerControllerID);
					Socket temp = new Socket(peer.peerAddress, peer.peerPort);
					PeerController peerControllerObj = new PeerController(temp, this);
					peerControllerObj.setPeerControllerId(peerControllerID);
					this.addConnectedPeer(peerControllerObj, peerControllerID);
					Thread peerControllerThread = new Thread(peerControllerObj);
					this.addJoinedThreads(peerControllerID, peerControllerThread);
					peerControllerThread.start();
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setChunkAvailabilityMap() {
		for (String pid : this.allPeerDetailsMap.keySet()) {
			BitSet chunkAvailabliltyMap = new BitSet(this.chunkCount);
			if (this.allPeerDetailsMap.get(pid).containsFile == 1) {
				chunkAvailabliltyMap.set(0, this.chunkCount);
				this.chunkAvailabilityMap.put(pid, chunkAvailabliltyMap);
			}
			else {
				chunkAvailabliltyMap.clear();
				this.chunkAvailabilityMap.put(pid, chunkAvailabliltyMap);
			}
		}
	}

	public synchronized void outputToFileSync(byte[] chunk, int chunkIndex) {
		try {
			int pointer = this.getChunkSize() * chunkIndex;
			this.filePointer.seek(pointer);
			this.filePointer.write(chunk);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public synchronized byte[] inputFromFileSync(int chunkIndex) {
		try {
			int pointer = this.getChunkSize() * chunkIndex;
			int length = this.getChunkSize();
			if (chunkIndex == getChunkCount() - 1) {
				length = this.getChunkSize() % this.getChunkSize();
			}
			this.filePointer.seek(pointer);
			byte[] chunk = new byte[length];
			this.filePointer.read(chunk);
			return chunk;
		}
		catch (Exception e) {
			e.printStackTrace();

		}
		return new byte[0];
	}

	public HashMap<String, Integer> getChunkDownloadRates() {
		HashMap<String, Integer> rates = new HashMap<>();
		for (String key : this.connectedPeers.keySet()) {
			rates.put(key, this.connectedPeers.get(key).getChunkDownloadRateRate());
		}
		return rates;
	}

	public synchronized void sendHave(int chunkIndex) {
		for (String peerControllerID : this.connectedPeers.keySet()) {
			this.connectedPeers.get(peerControllerID).messageSender.issueHaveMessage(chunkIndex);
		}
	}

	public synchronized void updateChunkAvailability(String peerControllerID, int chunkIndex) {
		this.chunkAvailabilityMap.get(peerControllerID).set(chunkIndex);
	}

	public synchronized void updateChunkDownloadRate(String endpeerid) {
		this.chunkDownloadRateInfo.put(endpeerid, this.chunkDownloadRateInfo.get(endpeerid) + 1);
	}

	public synchronized void updateChunkBitsetAvailability(String peerControllerID, BitSet chunkIndex) {
		this.chunkAvailabilityMap.remove(peerControllerID);
		this.chunkAvailabilityMap.put(peerControllerID, chunkIndex);
	}

	public synchronized void addConnectedPeer(PeerController peerControllerObj, String peerControllerID) {
		this.connectedPeers.put(peerControllerID, peerControllerObj);
	}

	public synchronized void addJoinedThreads(String epeerid, Thread th) {
		this.connectedPeerThreads.put(epeerid, th);
	}

	public synchronized HashMap<String, Thread> getConnectedPeerThreads() {
		return this.connectedPeerThreads;
	}

	public PeerController getPeerController(String peerid) {
		return this.connectedPeers.get(peerid);
	}

	public BitSet getChunkAvailabilityOf(String pid) {
		return this.chunkAvailabilityMap.get(pid);
	}

	public synchronized boolean checkIfInterested(String peerControllerID, int chunkIndex) {
		BitSet peerChunkAvailabilty = this.getChunkAvailabilityOf(peerControllerID);
		BitSet mine = this.getChunkAvailabilityOf(this.localPeerID);
		if(chunkIndex != -1 && chunkIndex < this.chunkCount){
			if (peerChunkAvailabilty.get(chunkIndex) && !mine.get(chunkIndex)) {
				return true;
			}
		}
		for (int i = 0; i < peerChunkAvailabilty.size() && i < this.chunkCount; i++) {
			if (peerChunkAvailabilty.get(i) == true && mine.get(i) == false) {
				return true;
			}

		}
		return false;
	}

	public synchronized void setChunkRequestedInfo(int chunkIndex, String peerControllerID) {
		this.requestedInfo[chunkIndex] = peerControllerID;
	}

	public synchronized int checkChunksRequested(String peerControllerID, int chunkIndex) {
		BitSet end = this.getChunkAvailabilityOf(peerControllerID);
		BitSet mine = this.getChunkAvailabilityOf(this.localPeerID);
		if(chunkIndex != -1 && chunkIndex < this.chunkCount){
			if (end.get(chunkIndex) && !mine.get(chunkIndex) && this.requestedInfo[chunkIndex] == null) {
				setChunkRequestedInfo(chunkIndex, peerControllerID);
				return chunkIndex;
			}
		}
		for (int i = 0; i < 10; i++) {
			int randomIndex = randObj.nextInt(this.chunkCount);

			if (end.get(randomIndex) && !mine.get(randomIndex) && this.requestedInfo[randomIndex] == null) {
				setChunkRequestedInfo(randomIndex, peerControllerID);
				return randomIndex;
			}
		}
		for (int i = 0; i < end.size() && i < this.chunkCount; i++) {
			if (end.get(i) == true && mine.get(i) == false && this.requestedInfo[i] == null) {
				setChunkRequestedInfo(i, peerControllerID);
				return i;
			}
		}
		return -1;
	}

	public synchronized void resetRequestedChunkInfo(String endPeerControllerID) {
		for (int i = 0; i < this.requestedInfo.length; i++) {
			if (this.requestedInfo[i] != null && this.requestedInfo[i].compareTo(endPeerControllerID) == 0) {
				setChunkRequestedInfo(i, null);
			}
		}
	}

	public String getLocalPeerID() {
		return this.localPeerID;
	}

	public ClientLogger getClientLogger() {
		return this.clientLogger;
	}

	public boolean hasSourceFile() {
		return this.localConfiguration.containsFile == 1;
	}

	public int getPreferredNeighborNumber() {
		return this.commonConfig.numberOfPreferredNeighbors;
	}

	public int getUnChokeFrequency() {
		return this.commonConfig.unchokingInterval;
	}

	public int getOptimisticUnChokeFrequency() {
		return this.commonConfig.optimisticUnchokingInterval;
	}

	public String getSourceFileName() {
		return this.commonConfig.fileName;
	}

	public int getSourceFileSize() {
		return this.commonConfig.fileSize;
	}

	public int getChunkSize() {
		return this.commonConfig.pieceSize;
	}

	public int calcChunkCount() {
		int len = (getSourceFileSize() / getChunkSize());
		if (getSourceFileSize() % getChunkSize() != 0) {
			len += 1;
		}
		return len;
	}

	public int getChunkCount() {
		return this.chunkCount;
	}

	public int getAvailableChunkCount() {
		return this.chunkAvailabilityMap.get(this.localPeerID).cardinality();
	}

	public synchronized void appendToInterestedList(String endPeerId) {
		this.interestedPeerList.add(endPeerId);
	}

	public synchronized void setPeerAsNotInterested(String endPeerId) {
		if (this.interestedPeerList != null) {
			this.interestedPeerList.remove(endPeerId);
		}
	}

	public synchronized void resetInterestedPeerList() {
		this.interestedPeerList.clear();
	}

	public synchronized Set<String> getInterestedPeerList() {
		return this.interestedPeerList;
	}

	public synchronized boolean addToUnChokedPeerList(String peerid) {
		return this.unChokedPeerList.add(peerid);
	}

	public synchronized Set<String> getUnChokedPeerList() {
		return this.unChokedPeerList;
	}

	public synchronized void resetUnChokedPeerList() {
		this.unChokedPeerList.clear();
	}

	public synchronized void updateUnChokedPeerList(Set<String> newUnChokedPeerList) {
		this.unChokedPeerList = newUnChokedPeerList;
	}

	public synchronized void setOptimisticUnChokedPeer(String peerControllerID) {
		this.optUnchockedPeer = peerControllerID;
	}

	public synchronized String getOptimisticUnChokedPeer() {
		return this.optUnchockedPeer;
	}

	public synchronized boolean areAllPeersDone() {
		for (String peerControllerID : this.chunkAvailabilityMap.keySet()) {
			if (this.chunkAvailabilityMap.get(peerControllerID).cardinality() != this.chunkCount) {
				return false;
			}
		}
		return true;
	}

	public synchronized OptimisticUnchokeHandler getOptController() {
		return this.optimisticUnChokeController;
	}

	public synchronized ChokeController getChokeController() {
		return this.chokeController;
	}

	public synchronized RandomAccessFile getFilePointer() {
		return this.filePointer;
	}

	public synchronized ServerSocket getLocalChannel() {
		return this.localChannel;
	}

	public synchronized Thread getLocalServerThread() {
		return this.localServerThread;
	}

	public synchronized Boolean checkIfDone() {
		return this.localFileDownloadComplete;
	}

	public synchronized void stopAllPeerControllers() {
		for (String peerControllerID : this.connectedPeerThreads.keySet()) {
			this.connectedPeerThreads.get(peerControllerID).stop();
		}
	}

	public synchronized void cancelChokes() {
		try {
			this.getOptController().cancelJob();
			this.getChokeController().cancelJob();
			this.resetUnChokedPeerList();
			this.setOptimisticUnChokedPeer(null);
			this.resetInterestedPeerList();
			this.getFilePointer().close();
			this.getClientLogger().closeTheClientLogger();
			this.getLocalChannel().close();
			this.getLocalServerThread().stop();
			this.localFileDownloadComplete = true;
			this.cleanupHandler.startJob(6);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
