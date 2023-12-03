

import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class CooperativeServer implements Runnable {
	private String peerId;
	private ServerSocket serverSocket;
	private PeerManager peerManager;
	private boolean killed;

	public CooperativeServer(String peerId, ServerSocket serverSocket, PeerManager peerManager) {
		this.peerId = peerId;
		this.serverSocket = serverSocket;
		this.peerManager = peerManager;
		this.killed = false;
	}

	/**
	 * Listens for incoming connection requests in a separate thread.
	 * Accepts incoming socket connections, creates a new PeerController for each connection,
	 * and starts a new thread to handle the communication with the connected peer.
	 * Continues running until explicitly killed.
	 */
	public void run() {
		while (!this.killed) {
			try {
				Socket socket = this.serverSocket.accept();
				PeerController peerController = new PeerController(socket, this.peerManager);
				new Thread(peerController).start();
				String peerAddress = socket.getInetAddress().toString();
				int peerPort = socket.getPort();
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
