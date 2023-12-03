

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
