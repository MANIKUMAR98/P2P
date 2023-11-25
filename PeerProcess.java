

import src.PeerAdmin;

public class PeerProcess {
	public static void main(String[] args) {
		String peerID = args[0];
		PeerAdmin admin = new PeerAdmin(peerID);
	}
}
