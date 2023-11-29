package src;

import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.nio.*;
import java.util.BitSet;
import java.lang.*;
import java.util.HashMap;
import java.util.List;

public class PeerHandler implements Runnable {
	private Socket listener;
	private PeerAdmin peerAdmin;

	public boolean choked = true;
	private String endPeerID;
	private boolean connectionEstablished = false;
	private boolean initializer = false;
	private HandshakeMessage hsm;
	private volatile int downloadRate = 0;
	private volatile ObjectOutputStream out;
	private volatile ObjectInputStream in;


	public MessageSender messageSender;


	public PeerHandler(Socket listener, PeerAdmin admin) {
		this.listener = listener;
		this.peerAdmin = admin;
		this.messageSender = new MessageSender(this);
		initStreams();
		this.hsm = new HandshakeMessage(this.peerAdmin.getPeerID());

	}

	public PeerAdmin getPeerAdmin() {
		return this.peerAdmin;
	}

	public void initStreams() {
		try {
			this.out = new ObjectOutputStream(this.listener.getOutputStream());
			this.out.flush();
			this.in = new ObjectInputStream(this.listener.getInputStream());
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public synchronized Socket getListener() {
		return this.listener;
	}

	public void setEndPeerID(String pid) {
		this.endPeerID = pid;
		this.initializer = true;
	}

	public void run() {
		try {
			byte[] msg = this.hsm.buildHandShakeMessage();
			this.out.write(msg);
			this.out.flush();
			while (1 != 2) {
				if (!this.connectionEstablished) {
					byte[] response = new byte[32];
					this.in.readFully(response);
					this.processHandShakeMessage(response);
					if (this.peerAdmin.hasFile() || this.peerAdmin.getAvailabilityOf(this.peerAdmin.getPeerID()).cardinality() > 0) {
						this.messageSender.sendBitField();
					}
				}
				else {
					while (this.in.available() < 4) {
					}
					int respLen = this.in.readInt();
					byte[] response = new byte[respLen];
					this.in.readFully(response);
					char messageType = (char) response[0];
					ActualMessage am = new ActualMessage();
					am.readActualMessage(respLen, response);
					processMessageType(Constants.MessageType.fromCode(messageType), am);

				}
			}
		}
		catch (SocketException e) {
			System.out.println("Socket exception");
			e.printStackTrace();
			try {
				this.peerAdmin.resetRequested(this.endPeerID);
				this.peerAdmin.getAvailabilityOf(this.endPeerID).set(0, this.peerAdmin.getPieceCount());

			}
			catch (Exception err){
				err.printStackTrace();


			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void processMessageType(Constants.MessageType msgType, ActualMessage am) {
		switch (msgType) {
			case CHOKE:
				handleChokeMessage();
				break;
			case UNCHOKE:
				handleUnchokeMessage();
				break;
			case INTERESTED:
				handleInterestedMessage();
				break;
			case NOT_INTERESTED:
				handleNotInterestedMessage();
				break;
			case HAVE:
				handleHaveMessage(am.getPieceIndexFromPayload());
				break;
			case BITFIELD:
				handleBitFieldMessage(am.getBitFieldMessage());
				break;
			case REQUEST:
				handleRequestMessage(am);
				break;
			case PIECE:
				handlePieceMessage(am);
				break;
			default:
				System.out.println("Received other message");
		}
	}

	private void handleChokeMessage() {
		this.choked = true;
		this.peerAdmin.resetRequested(this.endPeerID);
		this.peerAdmin.getClientLogger().storeChokingNeighborLog(this.endPeerID);
	}

	private void handleUnchokeMessage() {
		this.peerAdmin.getClientLogger().storeUnchokedNeighborLog(this.endPeerID);
		this.choked = false;
		int requestindex = this.peerAdmin.checkForRequested(this.endPeerID);
		if (requestindex == -1) {
			if(!this.peerAdmin.checkIfInterested(this.endPeerID)) {
				this.messageSender.sendNotInterestedMessage();
			}
			else {
				this.messageSender.sendInterestedMessage();
			}
		} else {
			this.peerAdmin.setRequestedInfo(requestindex, this.endPeerID);
			this.messageSender.sendRequestMessage(requestindex);
			this.peerAdmin.getLogger().sendRequest(this.endPeerID, requestindex );
		}
		this.peerAdmin.getClientLogger().storeUnchokedNeighborLog(this.endPeerID);
	}

	private void handleInterestedMessage() {
		this.peerAdmin.addToInterestedList(this.endPeerID);
		this.peerAdmin.getClientLogger().storeInterestedLog(this.endPeerID);
	}

	private void handleNotInterestedMessage() {
		this.peerAdmin.removeFromInterestedList(this.endPeerID);
		this.peerAdmin.getClientLogger().storeNotInterestedLog(this.endPeerID);
	}

	private void handleHaveMessage(int pieceIndex) {
		this.peerAdmin.updatePieceAvailability(this.endPeerID, pieceIndex);
		if (this.peerAdmin.checkIfAllPeersAreDone()) {
			this.peerAdmin.cancelChokes();
		}
		if (this.peerAdmin.checkIfInterested(this.endPeerID)) {
			this.messageSender.sendInterestedMessage();
		} else {
			this.messageSender.sendNotInterestedMessage();
		}
		this.peerAdmin.getClientLogger().storeHaveLog(this.endPeerID, pieceIndex);
	}

	private void handleBitFieldMessage(BitSet bset) {
		this.processBitFieldMessage(bset);
		if (!this.peerAdmin.hasFile()) {
			if (this.peerAdmin.checkIfInterested(this.endPeerID)) {
				this.messageSender.sendInterestedMessage();
			} else {
				this.messageSender.sendNotInterestedMessage();
			}
		}
	}

	private void handleRequestMessage(ActualMessage am) {
		if (this.peerAdmin.getUnchokedList().contains(this.endPeerID)
				|| (this.peerAdmin.getOptimisticUnchokedPeer() != null && this.peerAdmin.getOptimisticUnchokedPeer().compareTo(this.endPeerID) == 0)) {
			int pieceIndex = am.getPieceIndexFromPayload();
			this.messageSender.sendPieceMessage(pieceIndex, this.peerAdmin.readFromFile(pieceIndex));
		}
	}

	private void handlePieceMessage(ActualMessage am) {
		int pieceIndex = am.getPieceIndexFromPayload();
		byte[] piece = am.getPieceFromPayload();
		this.peerAdmin.writeToFile(piece, pieceIndex);
		this.peerAdmin.updatePieceAvailability(this.peerAdmin.getPeerID(), pieceIndex);
		this.downloadRate++;
		Boolean alldone = this.peerAdmin.checkIfAllPeersAreDone();
		this.peerAdmin.getClientLogger().storeDownloadedPieceLog(this.endPeerID, pieceIndex, this.peerAdmin.getCompletedPieceCount());
		this.peerAdmin.setRequestedInfo(pieceIndex, null);
		this.peerAdmin.broadcastHave(pieceIndex);
		if (this.peerAdmin.getAvailabilityOf(this.peerAdmin.getPeerID()).cardinality() != this.peerAdmin
				.getPieceCount()) {
			int requestindex = this.peerAdmin.checkForRequested(this.endPeerID);
			if(!this.choked){
				if (requestindex != -1) {
					this.messageSender.sendRequestMessage(requestindex);
					this.peerAdmin.getLogger().sendRequest(this.endPeerID, requestindex);
				} else {
					if(!this.peerAdmin.checkIfInterested(this.endPeerID))
						this.messageSender.sendNotInterestedMessage();
					else
						this.messageSender.sendInterestedMessage();
				}}
			else{
				if(requestindex != -1)
					this.peerAdmin.setRequestedInfo(requestindex, null);
			}
		} else {
			this.peerAdmin.getClientLogger().storeTheDownloadCompleteLog();
			if (alldone) {
				this.peerAdmin.cancelChokes();
			}
			else{
				this.messageSender.sendNotInterestedMessage();}
		}
	}
	public synchronized void send(byte[] obj) {
		try {
			this.out.write(obj);
			this.out.flush();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	public void processHandShakeMessage(byte[] message) {
		try {
			this.hsm.readHandShakeMessage(message);
			this.endPeerID = this.hsm.getPeerID();
			this.peerAdmin.addJoinedPeer(this, this.endPeerID);
			this.peerAdmin.addJoinedThreads(this.endPeerID, Thread.currentThread());
			this.connectionEstablished = true;
			if (this.initializer) {
				this.peerAdmin.getClientLogger().tcpConnectionLogSenderGenerator(this.endPeerID);
			}
			else {
				this.peerAdmin.getClientLogger().tcpConnectionLogReceiverGenerator(this.endPeerID);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void processBitFieldMessage(BitSet b) {
		this.peerAdmin.updateBitset(this.endPeerID, b);
	}

	public int getDownloadRate() {
		return this.downloadRate;
	}

	public void resetDownloadRate() {
		this.downloadRate = 0;
	}

}
