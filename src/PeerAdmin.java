package src;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Random;
import java.util.HashSet;


public class PeerAdmin {
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
	private volatile HashSet<String> unChokedPeerList;
	private volatile HashSet<String> interestedPeerList;
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

	public PeerAdmin(String peerID) {
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
		this.initPeer();
		this.chokeController = new ChokeController(this);
		this.chunkDownloadRateInfo = new HashMap<>();
		this.optimisticUnChokeController = new OptimisticUnchokeHandler(this);
		this.cleanupHandler = new TerminateHandler(this);
		this.chokeController.startJob();
		this.optimisticUnChokeController.startJob();
	}

	public void initPeer() {
		try {
			this.commonConfig.InitilizeCommonConfiguration();;
			this.peerConfigurationInfo.loadConfigFile();
			this.chunkCount = this.calcPieceCount();
			this.requestedInfo = new String[this.chunkCount];
			this.localConfiguration = this.peerConfigurationInfo.getPeerConfig(this.localPeerID);
			this.allPeerDetailsMap = this.peerConfigurationInfo.getPeerInfoMap();
			this.allPeerList = this.peerConfigurationInfo.getPeerList();
			String filepath = "peer_" + this.localPeerID;
			File file = new File(filepath);
			file.mkdir();
			String filename = filepath + "/" + getFileName();
			file = new File(filename);
			if (!hasFile()) {
				file.createNewFile();
			}
			this.filePointer = new RandomAccessFile(file, "rw");
			if (!hasFile()) {
				this.filePointer.setLength(this.getFileSize());
			}
			this.initializePieceAvailability();
			this.startServer();
			this.createNeighbourConnections();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void startServer() {
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

	public void createNeighbourConnections() {
		try {
			Thread.sleep(5000);
			for (String pid : this.allPeerList) {
				if (pid.equals(this.localPeerID)) {
					break;
				}
				else {
					RemotePeerInfo peer = this.allPeerDetailsMap.get(pid);
					Socket temp = new Socket(peer.peerAddress, peer.peerPort);
					PeerController p = new PeerController(temp, this);
					p.setPeerControllerId(pid);
					this.addJoinedPeer(p, pid);
					Thread t = new Thread(p);
					this.addJoinedThreads(pid, t);
					t.start();
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void initializePieceAvailability() {
		for (String pid : this.allPeerDetailsMap.keySet()) {
			BitSet availability = new BitSet(this.chunkCount);
			if (this.allPeerDetailsMap.get(pid).containsFile == 1) {
				availability.set(0, this.chunkCount);
				this.chunkAvailabilityMap.put(pid, availability);
			}
			else {
				availability.clear();
				this.chunkAvailabilityMap.put(pid, availability);
			}
		}
	}

	public synchronized void writeToFile(byte[] data, int pieceindex) {
		try {
			int position = this.getPieceSize() * pieceindex;
			this.filePointer.seek(position);
			this.filePointer.write(data);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public synchronized byte[] readFromFile(int pieceindex) {
		try {
			int position = this.getPieceSize() * pieceindex;
			int size = this.getPieceSize();
			if (pieceindex == getPieceCount() - 1) {
				size = this.getFileSize() % this.getPieceSize();
			}
			this.filePointer.seek(position);
			byte[] data = new byte[size];
			this.filePointer.read(data);
			return data;
		}
		catch (Exception e) {
			e.printStackTrace();

		}
		return new byte[0];
	}

	public HashMap<String, Integer> getDownloadRates() {
		HashMap<String, Integer> rates = new HashMap<>();
		for (String key : this.connectedPeers.keySet()) {
			rates.put(key, this.connectedPeers.get(key).getChunkDownloadRateRate());
		}
		return rates;
	}

	public synchronized void broadcastHave(int pieceIndex) {
		for (String key : this.connectedPeers.keySet()) {
			this.connectedPeers.get(key).messageSender.issueHaveMessage(pieceIndex);
		}
	}

	public synchronized void updatePieceAvailability(String peerID, int index) {
		this.chunkAvailabilityMap.get(peerID).set(index);
	}

	public synchronized void updateDownloadRate(String endpeerid) {
		this.chunkDownloadRateInfo.put(endpeerid, this.chunkDownloadRateInfo.get(endpeerid) + 1);
	}

	public synchronized void updateBitset(String peerID, BitSet b) {
		this.chunkAvailabilityMap.remove(peerID);
		this.chunkAvailabilityMap.put(peerID, b);
	}

	public synchronized void addJoinedPeer(PeerController p, String endpeerid) {
		this.connectedPeers.put(endpeerid, p);
	}

	public synchronized void addJoinedThreads(String epeerid, Thread th) {
		this.connectedPeerThreads.put(epeerid, th);
	}

	public synchronized HashMap<String, Thread> getJoinedThreads() {
		return this.connectedPeerThreads;
	}

	public PeerController getPeerHandler(String peerid) {
		return this.connectedPeers.get(peerid);
	}

	public BitSet getAvailabilityOf(String pid) {
		return this.chunkAvailabilityMap.get(pid);
	}

	public synchronized boolean checkIfInterested(String endpeerid, int index) {
		BitSet end = this.getAvailabilityOf(endpeerid);
		BitSet mine = this.getAvailabilityOf(this.localPeerID);
		if(index != -1 && index < this.chunkCount){
			if (end.get(index) && !mine.get(index)) {
				return true;
			}
		}
		for (int i = 0; i < end.size() && i < this.chunkCount; i++) {
			if (end.get(i) == true && mine.get(i) == false) {
				return true;
			}
		}
		return false;
	}

	public synchronized void setRequestedInfo(int id, String peerID) {
		this.requestedInfo[id] = peerID;
	}

	public synchronized int checkForRequested(String endpeerid, int index) {
		BitSet end = this.getAvailabilityOf(endpeerid);
		BitSet mine = this.getAvailabilityOf(this.localPeerID);
		if(index != -1 && index < this.chunkCount){
			if (end.get(index) && !mine.get(index) && this.requestedInfo[index] == null) {
				setRequestedInfo(index, endpeerid);
				return index;
			}
		}
		for (int i = 0; i < 10; i++) {
			int randomIndex = randObj.nextInt(this.chunkCount);

			if (end.get(randomIndex) && !mine.get(randomIndex) && this.requestedInfo[randomIndex] == null) {
				setRequestedInfo(randomIndex, endpeerid);
				return randomIndex;
			}
		}
		for (int i = 0; i < end.size() && i < this.chunkCount; i++) {
			if (end.get(i) == true && mine.get(i) == false && this.requestedInfo[i] == null) {
				setRequestedInfo(i, endpeerid);
				return i;
			}
		}
		return -1;
	}

	public synchronized void resetRequested(String endpeerid) {
		for (int i = 0; i < this.requestedInfo.length; i++) {
			if (this.requestedInfo[i] != null && this.requestedInfo[i].compareTo(endpeerid) == 0) {
				setRequestedInfo(i, null);
			}
		}
	}

	public String getPeerID() {
		return this.localPeerID;
	}

	public ClientLogger getClientLogger() {
		return this.clientLogger;
	}

	public boolean hasFile() {
		return this.localConfiguration.containsFile == 1;
	}

	public int getNoOfPreferredNeighbors() {
		return this.commonConfig.numberOfPreferredNeighbors;
	}

	public int getUnchockingInterval() {
		return this.commonConfig.unchokingInterval;
	}

	public int getOptimisticUnchockingInterval() {
		return this.commonConfig.optimisticUnchokingInterval;
	}

	public String getFileName() {
		return this.commonConfig.fileName;
	}

	public int getFileSize() {
		return this.commonConfig.fileSize;
	}

	public int getPieceSize() {
		return this.commonConfig.pieceSize;
	}

	public int calcPieceCount() {
		int len = (getFileSize() / getPieceSize());
		if (getFileSize() % getPieceSize() != 0) {
			len += 1;
		}
		return len;
	}

	public int getPieceCount() {
		return this.chunkCount;
	}

	public int getCompletedPieceCount() {
		return this.chunkAvailabilityMap.get(this.localPeerID).cardinality();
	}

	public synchronized void addToInterestedList(String endPeerId) {
		this.interestedPeerList.add(endPeerId);
	}

	public synchronized void removeFromInterestedList(String endPeerId) {
		if (this.interestedPeerList != null) {
			this.interestedPeerList.remove(endPeerId);
		}
	}

	public synchronized void resetInterestedList() {
		this.interestedPeerList.clear();
	}

	public synchronized HashSet<String> getInterestedList() {
		return this.interestedPeerList;
	}

	public synchronized boolean addUnchokedPeer(String peerid) {
		return this.unChokedPeerList.add(peerid);
	}

	public synchronized HashSet<String> getUnchokedList() {
		return this.unChokedPeerList;
	}

	public synchronized void resetUnchokedList() {
		this.unChokedPeerList.clear();
	}

	public synchronized void updateUnchokedList(HashSet<String> newSet) {
		this.unChokedPeerList = newSet;
	}

	public synchronized void setOptimisticUnchokdPeer(String peerid) {
		this.optUnchockedPeer = peerid;
	}

	public synchronized String getOptimisticUnchokedPeer() {
		return this.optUnchockedPeer;
	}

	public synchronized boolean checkIfAllPeersAreDone() {
		for (String peer : this.chunkAvailabilityMap.keySet()) {
			if (this.chunkAvailabilityMap.get(peer).cardinality() != this.chunkCount) {
				return false;
			}
		}
		return true;
	}

	public synchronized OptimisticUnchokeHandler getoptHandler() {
		return this.optimisticUnChokeController;
	}

	public synchronized ChokeController getchHandler() {
		return this.chokeController;
	}

	public synchronized RandomAccessFile getRefFile() {
		return this.filePointer;
	}

	public synchronized ServerSocket getListener() {
		return this.localChannel;
	}

	public synchronized Thread getServerThread() {
		return this.localServerThread;
	}

	public synchronized Boolean checkIfDone() {
		return this.localFileDownloadComplete;
	}

	public synchronized void closeHandlers() {
		for (String peer : this.connectedPeerThreads.keySet()) {
			this.connectedPeerThreads.get(peer).stop();
		}
	}

	public synchronized void cancelChokes() {
		try {
			this.getoptHandler().cancelJob();
			this.getchHandler().cancelJob();
			this.resetUnchokedList();
			this.setOptimisticUnchokdPeer(null);
			this.resetInterestedList();
			this.getRefFile().close();
			this.getClientLogger().closeTheClientLogger();
			this.getListener().close();
			this.getServerThread().stop();
			this.localFileDownloadComplete = true;
			this.cleanupHandler.startJob(6);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
