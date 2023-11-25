package src;

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
	 
	    public synchronized void genTCPConnLogSender(String peer) {
//	        this.clientLogger.log(Level.INFO, "[" + currTime + "]: Peer [" + this.peerId + "] makes a connection to Peer " + "[" + peer + "].");
//	        this.clientLogger.log(Level.INFO, "[{}]: Peer [{}] makes a connection to Peer [{}].", utility.getCurrentTime(), this.peerId, peer);
	        this.clientLogger.logp(Level.INFO, "[{}]: Peer [{}] makes a connection to Peer [{}].", utility.getCurrentTime(), this.peerId, peer);
	    }
	    
	    public synchronized void storeTCPConnectionCreatedLog(String peerId) {
//	    	this.clientLogger.log(Level.INFO, "Peer [" + this.peerId + " + ] has established a connection with Peer [" + peer + "] at [" + this.getCurrentTime() +"]");
	    }

}
