package src;

import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class ClientLogger {
	
	 private String loggingFile;
	 private String peerId;
	 private FileHandler logFileHandlerForPeer;
	 private Utility utility;
	 private Logger clientLogger = Logger.getLogger("ClientLogger");
	 
	 public ClientLogger() {
		try {
			 utility = new Utility();
			 this.loggingFile = "log_peer_" + this.peerId + ".log";
	         this.logFileHandlerForPeer = new FileHandler(this.loggingFile, false);
	         System.setProperty("java.util.logging.SimpleFormatter.format", "%5$s %n");
	         this.logFileHandlerForPeer.setFormatter(new SimpleFormatter());
	         this.clientLogger.setUseParentHandlers(false);
	         this.clientLogger.addHandler(this.logFileHandlerForPeer);
		} catch (Exception e) {
			System.err.println("Exception occurred while instantiating the Client Logger: " + e);
		}
	 }
	 
	 
	    public void setPeerId(String peerId) {
		   this.peerId = peerId;
	    }
	 
	    public synchronized void tcpConnectionLogSenderGenerator(String peer) {
	        this.clientLogger.log(Level.INFO, "[" + utility.getCurrentTime() + "]: Peer [" + this.peerId + "] makes a connection to Peer " + "[" + peer + "].");
	    }
	    
	    public synchronized void tcpConnectionLogReceiverGenerator(String peer) {
	    	this.clientLogger.log(Level.INFO, "[" + utility.getCurrentTime() + "]: Peer [" + this.peerId + "] is connected from Peer " + "[" + peer + "].");
	    }
	    
	    public synchronized void updatePreferredNeighbors(List<String> neighbors) {
	        this.clientLogger.log(Level.INFO, "[" + utility.getCurrentTime() + "]: Peer [" + this.peerId + "] has the preferred neighbors [" + utility.getStringFromList(neighbors) + "].");
	    }
	    
	    public synchronized void storeUnchokedNeighborLog(String peer) {
	        this.clientLogger.log(Level.INFO,  "[" + utility.getCurrentTime() + "]: Peer [" + this.peerId + "] is unchoked by [" + peer + "].");
	    }
	    
	    public synchronized void storeChokingNeighborLog(String peer) {
	        this.clientLogger.log(Level.INFO, "[" +  utility.getCurrentTime() + "]: Peer [" + this.peerId + "] is choked by [" + peer + "].");
	    }
	    
	    public synchronized void storeHaveLog(String peer, int dataIndex) {
	        this.clientLogger.log(Level.INFO, "[" + utility.getCurrentTime()  + "]: Peer [" + this.peerId + "] received the ‘have’ message from [" + peer + "] for the piece [" + String.valueOf(dataIndex) + "].");
	    }
	    
	    public synchronized void storeInterestedLog(String peer) {
	        this.clientLogger.log(Level.INFO, "[" + utility.getCurrentTime() + "]: Peer [" + this.peerId + "] received the ‘interested’ message from [" + peer + "].");
	    }
	    
	    public synchronized void storeNotInterestedLog(String peer) {
	        this.clientLogger.log(Level.INFO, "[" + utility.getCurrentTime() + "]: Peer [" + this.peerId  + "] received the ‘not interested’ message from [" + peer + "].");
	    }
	    
	    public synchronized void storeDownloadedPieceLog(String peer, int ind, int data) {
	        this.clientLogger.log(Level.INFO,  "[" + utility.getCurrentTime() + "]: Peer [" + this.peerId + "] has downloaded the piece [" + String.valueOf(ind)
	                        + "] from [" + peer + "]. Now the number of pieces it has is [" + String.valueOf(data) + "].");
	    }
	    
	    public synchronized void storeTheDownloadCompleteLog() {
	        this.clientLogger.log(Level.INFO, "[" + utility.getCurrentTime() + "]: Peer [" + this.peerId + "] has downloaded the complete file.");
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
