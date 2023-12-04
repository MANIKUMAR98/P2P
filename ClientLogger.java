

import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class ClientLogger {
	
	 private String loggingFile;
	 private String peerId;
	 private FileHandler logFileHandlerForPeer;
	 Utility utility;
	 private Logger clientLogger = Logger.getLogger("ClientLogger");
	 
	 public ClientLogger(String peerId) {
		try {
			this.peerId = peerId;
			 utility = new Utility();
			 this.loggingFile = "log_peer_" + this.peerId + ".log";
	         this.logFileHandlerForPeer = new FileHandler(this.loggingFile, false);
	         initilizeFormat();
		} catch (Exception e) {
			System.err.println("Exception occurred while instantiating the Client Logger: " + e);
		}
	 }
	 
	 /**
	  * Initializes the logging format for the client's logger.
	  * Configures the logging format using the SimpleFormatter and sets it as the
	  * formatter for the logFileHandlerForPeer. Additionally, disables the use of
	  * parent handlers for the clientLogger and adds the logFileHandlerForPeer
	  * as a handler for the clientLogger.
	  */
	    private void initilizeFormat() {
	    	System.setProperty("java.util.logging.SimpleFormatter.format", Constants.SIIMPLE_FORMAT_FOR_LOGGER);
	        this.logFileHandlerForPeer.setFormatter(new SimpleFormatter());
	        this.clientLogger.setUseParentHandlers(false);
	        this.clientLogger.addHandler(this.logFileHandlerForPeer);
	    }
	    
	    /**
	     * Generates a log entry for a TCP connection initiation.
	     * Logs the connection initiation from the current peer to the specified peer,
	     * including the timestamp and peer IDs involved.
	     *
	     * @param peer The peer to which the connection is made.
	     */
		public synchronized void tcpConnectionLogSenderGenerator(String peer) {
	        this.clientLogger.log(Level.INFO, "[" + utility.getCurrentTime() + "]: Peer [" + this.peerId + "] makes a connection to Peer " + "[" + peer + "].");
	    }
	    
		/**
		 * Generates a log entry for a received TCP connection.
		 * Logs the information when the current peer is connected from the specified peer,
		 * including the timestamp and peer IDs involved.
		 *
		 * @param peer The peer from which the connection is received.
		 */
	    public synchronized void tcpConnectionLogReceiverGenerator(String peer) {
	    	this.clientLogger.log(Level.INFO, "[" + utility.getCurrentTime() + "]: Peer [" + this.peerId + "] is connected from Peer " + "[" + peer + "].");
	    }
	    
	    /**
	     * Updates and logs the list of preferred neighbors for the current peer.
	     * Logs the information about the update, including the timestamp, current peer ID,
	     * and the list of preferred neighbors.
	     *
	     * @param neighbors The list of peer IDs representing the preferred neighbors.
	     */
	    public synchronized void updatePreferredNeighbors(List<String> neighbors) {
	        this.clientLogger.log(Level.INFO, "[" + utility.getCurrentTime() + "]: Peer [" + this.peerId + "] has the preferred neighbors [" + utility.getStringFromList(neighbors) + "].");
	    }
	    
	    /**
	     * Logs the information when the current peer is unchoked by a neighboring peer.
	     * Records the unchoking event with timestamp, current peer ID, and the peer ID
	     * that unchoked the current peer.
	     *
	     * @param peer The peer ID that unchoked the current peer.
	     */
	    public synchronized void storeUnchokedNeighborLog(String peer) {
	        this.clientLogger.log(Level.INFO,  "[" + utility.getCurrentTime() + "]: Peer [" + this.peerId + "] is unchoked by [" + peer + "].");
	    }
	    
	    /**
	     * Logs the information when the current peer is choked by a neighboring peer.
	     * Records the choking event with timestamp, current peer ID, and the peer ID
	     * that choked the current peer.
	     *
	     * @param peer The peer ID that choked the current peer.
	     */
	    public synchronized void storeChokingNeighborLog(String peer) {
	        this.clientLogger.log(Level.INFO, "[" +  utility.getCurrentTime() + "]: Peer [" + this.peerId + "] is choked by [" + peer + "].");
	    }
	    
	    /**
	     * Logs the information when the current peer receives a 'have' message from a neighboring peer.
	     * Records the 'have' message event with timestamp, current peer ID, the peer ID
	     * from which the message is received, and the index of the received piece.
	     *
	     * @param peer The peer ID from which the 'have' message is received.
	     * @param dataIndex The index of the piece received in the 'have' message.
	     */
	    public synchronized void storeHaveLog(String peer, int dataIndex) {
	        this.clientLogger.log(Level.INFO, "[" + utility.getCurrentTime()  + "]: Peer [" + this.peerId + "] received the ‘have’ message from [" + peer + "] for the piece [" + String.valueOf(dataIndex) + "].");
	    }
	    
	    /**
	     * Logs the information when the current peer receives an 'interested' message from a neighboring peer.
	     * Records the 'interested' message event with timestamp, current peer ID, and the peer ID
	     * from which the message is received.
	     *
	     * @param peer The peer ID from which the 'interested' message is received.
	     */
	    public synchronized void storeInterestedLog(String peer) {
	        this.clientLogger.log(Level.INFO, "[" + utility.getCurrentTime() + "]: Peer [" + this.peerId + "] received the ‘interested’ message from [" + peer + "].");
	    }
	    
	    /**
	     * Logs the information when the current peer receives a 'not interested' message from a neighboring peer.
	     * Records the 'not interested' message event with timestamp, current peer ID, and the peer ID
	     * from which the message is received.
	     *
	     * @param peer The peer ID from which the 'not interested' message is received.
	     */
	    public synchronized void storeNotInterestedLog(String peer) {
	        this.clientLogger.log(Level.INFO, "[" + utility.getCurrentTime() + "]: Peer [" + this.peerId  + "] received the ‘not interested’ message from [" + peer + "].");
	    }
	    
	    /**
	     * Logs the information when the current peer downloads a piece from a neighboring peer.
	     * Records the downloaded piece event with timestamp, current peer ID, the index of the downloaded piece,
	     * the peer ID from which the piece is downloaded, and the updated number of pieces the current peer has.
	     *
	     * @param peer The peer ID from which the piece is downloaded.
	     * @param ind The index of the downloaded piece.
	     * @param data The updated number of pieces the current peer has.
	     */
	    public synchronized void storeDownloadedPieceLog(String peer, int ind, int data) {
	        this.clientLogger.log(Level.INFO,  "[" + utility.getCurrentTime() + "]: Peer [" + this.peerId + "] has downloaded the piece [" + String.valueOf(ind)
	                        + "] from [" + peer + "]. Now the number of pieces it has is [" + String.valueOf(data) + "].");
	    }
	    
	    /**
	     * Logs the information when the current peer completes the download of the entire file.
	     * Records the download completion event with timestamp and current peer ID.
	     */
	    public synchronized void storeTheDownloadCompleteLog() {
	        this.clientLogger.log(Level.INFO, "[" + utility.getCurrentTime() + "]: Peer [" + this.peerId + "] has downloaded the complete file.");
	    }
	    
	    /**
	     * Logs the information when the current peer updates its optimistically unchoked neighbor.
	     * Records the update of the optimistically unchoked neighbor with timestamp, current peer ID,
	     * and the peer ID of the newly optimistically unchoked neighbor.
	     *
	     * @param neighbourId The peer ID of the optimistically unchoked neighbor.
	     */
	    public synchronized void updateOptimisticallyUnchokedNeighbor(String neighbourId) {
	    	this.clientLogger.log(Level.INFO, "[" + utility.getCurrentTime() + "]: Peer [" + this.peerId  + "] has the optimistically unchoked neighbor [" + neighbourId + "]. [" + neighbourId + "]  is the peer ID of the optimistically unchoked neighbor.");
	    }
	    
	    public void closeTheClientLogger() {
	        try {
	            if (this.logFileHandlerForPeer != null) {
	                this.logFileHandlerForPeer.close();
	            }
	        } 
	        catch (Exception e) {
	            e.printStackTrace();
	        }
	    }

}
