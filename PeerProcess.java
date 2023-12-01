

import src.PeerManager;

public class PeerProcess {
	public static void main(String[] args) {
		String peerID = args[0];
		new PeerManager(peerID);
	}
}
