

import java.io.File;
import java.io.RandomAccessFile;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

@SuppressWarnings("deprecation")
public class PeerManager {
	private String localPeerID;
	private PeerInformation localConfiguration;
	private Map<String, PeerInformation> allPeerDetailsMap;
	private List<String> allPeerList;
	private volatile HashMap<String, PeerController> connectedPeers;
	private volatile HashMap<String, Thread> connectedPeerThreads;
	private volatile ServerSocket localChannel;
	private CooperativeServer cooperativeServer;
	private CommonConfiguration commonConfig;
	private PeerInformationConfiguration peerInformationConfiguration;
	private volatile ClientLogger clientLogger;
	private volatile HashMap<String, BitSet> chunkAvailabilityMap;
	private volatile String[] requestedInfo;
	private volatile Set<String> unChokedPeerList;
	private volatile Set<String> interestedPeerList;
	private volatile String optUnchockedPeer;
	private int chunkCount;
	private volatile RandomAccessFile filePointer;
	private volatile ChokeController chokeController;
	private volatile OptimisticUnchokeController optimisticUnchokeController;
	private volatile ShutdownProcessor shutdownProcessor;
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
		this.peerInformationConfiguration = new PeerInformationConfiguration();
		this.clientLogger = new ClientLogger(peerID);
		this.localFileDownloadComplete = false;
		this.unChokedPeerList = new HashSet<>();
		this.interestedPeerList = new HashSet<>();
		this.initializeLocalPeer();
		this.chokeController = new ChokeController(this);
		this.chunkDownloadRateInfo = new HashMap<>();
		this.optimisticUnchokeController = new OptimisticUnchokeController(this);
		this.shutdownProcessor = new ShutdownProcessor(this);
		this.chokeController.initilizeTheJob();
		this.optimisticUnchokeController.initilizeTheJob();
	}

	public void initializeLocalPeer() {
		try {
			//Initialize the local server, configurations, connect to neighbooring peers, start all the threads and schedulers
			this.commonConfig.InitilizeCommonConfiguration();;
			this.peerInformationConfiguration.initilizePeerInformationFile();
			this.chunkCount = this.calcChunkCount();
			this.requestedInfo = new String[this.chunkCount];
			this.localConfiguration = this.peerInformationConfiguration.getPeerInfoConfiguration(localPeerID);
			this.allPeerDetailsMap = this.peerInformationConfiguration.getRemotePeerInformationMap();
			this.allPeerList = this.peerInformationConfiguration.getPeerInformation();
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
			// Creates the socket and bind the port and starts the local server thread to listen and accept connections
			this.localChannel = new ServerSocket(this.localConfiguration.peerPort);
			this.cooperativeServer = new CooperativeServer(this.localPeerID, this.localChannel, this);
			this.localServerThread = new Thread(this.cooperativeServer);
			this.localServerThread.start();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void connectToOtherPeers() {
		try {
			// Connect to peers which have already started
			Thread.sleep(5000); // wait for other servers to start
			for (String peerControllerID : this.allPeerList) {
				if (peerControllerID.equals(this.localPeerID)) {
					break;
				}
				else {
					PeerInformation peerInformation = this.allPeerDetailsMap.get(peerControllerID);
					Socket temp = new Socket(peerInformation.peerAddress, peerInformation.peerPort);
					PeerController peerControllerObj = new PeerController(temp, this);
					peerControllerObj.setPeerControllerId(peerControllerID);
					this.addConnectedPeer(peerControllerObj, peerControllerID);
					Thread peerControllerThread = new Thread(peerControllerObj);
					this.addJoinedThreads(peerControllerID, peerControllerThread);
					peerControllerThread.start(); // start peer thread, accept and send messages
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setChunkAvailabilityMap() {
		//create BitSet where each bit represent a chunk , and if the peer has the file, mark all the bit set
		for (String peerControllerID : this.allPeerDetailsMap.keySet()) {
			BitSet chunkAvailabliltyMap = new BitSet(this.chunkCount);
			if (this.allPeerDetailsMap.get(peerControllerID).containsFile == 1) {
				chunkAvailabliltyMap.set(0, this.chunkCount);
				this.chunkAvailabilityMap.put(peerControllerID, chunkAvailabliltyMap);
			}
			else {
				chunkAvailabliltyMap.clear();
				this.chunkAvailabilityMap.put(peerControllerID, chunkAvailabliltyMap);
			}
		}
	}

	public synchronized void outputToFileSync(byte[] chunk, int chunkIndex) {
		try {
			//write the chink to file at proper position
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
			//read a chunk from the file
			int pointer = this.getChunkSize() * chunkIndex;
			int length = this.getChunkSize();
			if (chunkIndex == getChunkCount() - 1) {
				length = this.getSourceFileSize() % this.getChunkSize();
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
		//Gets the number of chunk downloded from each peer
		HashMap<String, Integer> rates = new HashMap<>();
		for (String key : this.connectedPeers.keySet()) {
			rates.put(key, this.connectedPeers.get(key).getChunkDownloadRateRate());
		}
		return rates;
	}

	public synchronized void sendHave(int chunkIndex) {
		// Broadcast have message
		for (String peerControllerID : this.connectedPeers.keySet()) {
			this.connectedPeers.get(peerControllerID).messageSender.issueHaveMessage(chunkIndex);
		}
	}

	public synchronized void updateChunkAvailability(String peerControllerID, int chunkIndex) {
		//Set the chunk as persent for the peer
		this.chunkAvailabilityMap.get(peerControllerID).set(chunkIndex);
	}

	public synchronized void updateChunkDownloadRate(String endpeerid) {
		// Increase cunk downloaded for the pee by 1
		this.chunkDownloadRateInfo.put(endpeerid, this.chunkDownloadRateInfo.get(endpeerid) + 1);
	}

	public synchronized void updateChunkBitsetAvailability(String peerControllerID, BitSet chunkIndexMap) {
		// Remove existing BitSet for peer and replace it with new BitSet object
		this.chunkAvailabilityMap.remove(peerControllerID);
		this.chunkAvailabilityMap.put(peerControllerID, chunkIndexMap);
	}

	public synchronized void addConnectedPeer(PeerController peerControllerObj, String peerControllerID) {
		//Adds neww peer to the connectedPeers map
		this.connectedPeers.put(peerControllerID, peerControllerObj);
	}

	public synchronized void addJoinedThreads(String peerControllerID, Thread thread) {
		//add peer that connected to the server
		this.connectedPeerThreads.put(peerControllerID, thread);
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
		// Check if the peer has the chunk and if the same chunk is not available in the local server if so returns true else false
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
		// sets the chunkIndex as requested so that no multiple requests for the same chunk is made
		this.requestedInfo[chunkIndex] = peerControllerID;
	}

	public synchronized int checkChunksRequested(String peerControllerID, int chunkIndex) {
		//checks if the peer has a chunk that is not not available in local server and also the chink which is not yet requested to other peers
		BitSet end = this.getChunkAvailabilityOf(peerControllerID);
		BitSet mine = this.getChunkAvailabilityOf(this.localPeerID);
		if(chunkIndex != -1 && chunkIndex < this.chunkCount){
			if (end.get(chunkIndex) && !mine.get(chunkIndex) && this.requestedInfo[chunkIndex] == null) {
				setChunkRequestedInfo(chunkIndex, peerControllerID);
				return chunkIndex;
			}
		}
		// for 10 times randomly check for a chunk
		for (int i = 0; i < 10; i++) {
			int randomIndex = randObj.nextInt(this.chunkCount);

			if (end.get(randomIndex) && !mine.get(randomIndex) && this.requestedInfo[randomIndex] == null) {
				setChunkRequestedInfo(randomIndex, peerControllerID);
				return randomIndex;
			}
		}
		// if not found do a sequential search
		for (int i = 0; i < end.size() && i < this.chunkCount; i++) {
			if (end.get(i) == true && mine.get(i) == false && this.requestedInfo[i] == null) {
				setChunkRequestedInfo(i, peerControllerID);
				return i;
			}
		}
		return -1;
	}

	public synchronized void resetRequestedChunkInfo(String endPeerControllerID) {
		//Unset all the chinks requested to a peer
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
		// Total number of chunk required for download to me marked complete
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
		// Check if all the peers have the complete files
		for (String peerControllerID : this.chunkAvailabilityMap.keySet()) {
			if (this.chunkAvailabilityMap.get(peerControllerID).cardinality() != this.chunkCount) {
				return false;
			}
		}
		return true;
	}

	public synchronized OptimisticUnchokeController getOptimisticUnchokeController() {
		return this.optimisticUnchokeController;
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
			// Stop all the scheduler and local thread
			this.getOptimisticUnchokeController().abortJob();
			this.getChokeController().abortJob();
			this.resetUnChokedPeerList();
			this.setOptimisticUnChokedPeer(null);
			this.resetInterestedPeerList();
			this.getFilePointer().close();
			this.getClientLogger().closeTheClientLogger();
			this.getLocalChannel().close();
			this.getLocalServerThread().stop();
			this.localFileDownloadComplete = true;
			this.shutdownProcessor.initilizeJob(6);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
