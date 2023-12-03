

public class PeerInformation {
	public String peerId;
	public String peerAddress;
	public int peerPort;
	public int containsFile;

	public PeerInformation(String peerId, String peerAddress, String peerPort, String containsFile) {
		this.peerId = peerId;
		this.peerAddress = peerAddress;
		this.peerPort = Integer.parseInt(peerPort);
		this.containsFile = Integer.parseInt(containsFile);
	}
	
}
