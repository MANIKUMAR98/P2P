

import src.PeerManager;

public class PeerProcess {
	public static void main(String[] args) throws InterruptedException {
		String peerID = args[0];
		new PeerManager(peerID);
	}
}
