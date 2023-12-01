package src;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class PeerServer implements Runnable {
	private String peerID;
	private ServerSocket listener;
	private PeerManager peerManager;
	private boolean dead;

	public PeerServer(String peerID, ServerSocket listener, PeerManager peerManager) {
		this.peerID = peerID;
		this.listener = listener;
		this.peerManager = peerManager;
		this.dead = false;
	}

	public void run() {
		while (!this.dead) {
			try {
				Socket neighbour = this.listener.accept();
				PeerController neighbourHandler = new PeerController(neighbour, this.peerManager);
				new Thread(neighbourHandler).start();
				String addr = neighbour.getInetAddress().toString();
				int port = neighbour.getPort();
			}
			catch (SocketException e) {
				break;
			}
			catch (Exception e) {
				e.printStackTrace();
				break;
			}
		}
	}
}
