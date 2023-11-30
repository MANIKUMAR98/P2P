package src;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.BitSet;

public class PeerController implements Runnable {
	private Socket communicationChannel;
	private PeerAdmin coordinator;

	public boolean choked = true;
	private String peerControllerId;
	private boolean  channelEstablished = false;
	private boolean  intialized = false;
	private HandshakeMessage estMessage;
	private volatile int chunkDownloadRate = 0;
	private volatile ObjectOutputStream out_stream;
	private volatile ObjectInputStream input_stream;


	public MessageSender messageSender;


	public PeerController(Socket communicationChannel, PeerAdmin coordinator) {
		this.communicationChannel = communicationChannel;
		this.coordinator = coordinator;
		this.messageSender = new MessageSender(this);
		initializeIOStreams();
		this.estMessage = new HandshakeMessage(this.coordinator.getPeerID());

	}

	public PeerAdmin getCoordinator() {
		return this.coordinator;
	}

	public void initializeIOStreams() {
		try {
			this.out_stream = new ObjectOutputStream(this.communicationChannel.getOutputStream());
			this.out_stream.flush();
			this.input_stream = new ObjectInputStream(this.communicationChannel.getInputStream());
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public synchronized Socket getCommunicationChannel() {
		return this.communicationChannel;
	}

	public void setPeerControllerId(String pid) {
		this.peerControllerId = pid;
		this.intialized = true;
	}

	public void run() {
		try {
			byte[] msg = this.estMessage.buildHandShakeMessage();
			this.out_stream.write(msg);
			this.out_stream.flush();
			while (true) {
				if (!this.channelEstablished) {
					byte[] receivedData = new byte[32];
					this.input_stream.readFully(receivedData);
					this.processHandShakeMessage(receivedData);
					if (this.coordinator.hasFile() || this.coordinator.getAvailabilityOf(this.coordinator.getPeerID()).cardinality() > 0) {
						this.messageSender.issueBitField();
					}
				}
				else {
					while (this.input_stream.available() < 4) {
					}
					int payloadLength = this.input_stream.readInt();
					byte[] response = new byte[payloadLength];
					this.input_stream.readFully(response);
					char msgTypeValue = (char) response[0];
					ActualMessage msgObj = new ActualMessage();
					msgObj.readActualMessage(payloadLength, response);
					processMessageType(Constants.MessageType.fromCode(msgTypeValue), msgObj);

				}
			}
		}
		catch (SocketException e) {
			System.out.println("Socket exception");
			e.printStackTrace();
			try {
				this.coordinator.resetRequested(this.peerControllerId);
				this.coordinator.getAvailabilityOf(this.peerControllerId).set(0, this.coordinator.getPieceCount());

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
		this.coordinator.resetRequested(this.peerControllerId);
		this.coordinator.getClientLogger().storeChokingNeighborLog(this.peerControllerId);
	}

	private void handleUnchokeMessage() {
		this.coordinator.getClientLogger().storeUnchokedNeighborLog(this.peerControllerId);
		this.choked = false;
		int chunkIndex = this.coordinator.checkForRequested(this.peerControllerId, -1);
		if (chunkIndex == -1) {
			if(!this.coordinator.checkIfInterested(this.peerControllerId, -1)) {
				this.messageSender.issueNotInterestedMessage();
			}
			else {
				this.messageSender.issueInterestedMessage();
			}
		} else {
			this.coordinator.setRequestedInfo(chunkIndex, this.peerControllerId);
			this.messageSender.issueRequestMessage(chunkIndex);
		}
	}

	private void handleInterestedMessage() {
		this.coordinator.addToInterestedList(this.peerControllerId);
		this.coordinator.getClientLogger().storeInterestedLog(this.peerControllerId);
	}

	private void handleNotInterestedMessage() {
		this.coordinator.removeFromInterestedList(this.peerControllerId);
		this.coordinator.getClientLogger().storeNotInterestedLog(this.peerControllerId);
	}

	private void handleHaveMessage(int pieceIndex) {
		this.coordinator.updatePieceAvailability(this.peerControllerId, pieceIndex);
		if (this.coordinator.checkIfAllPeersAreDone()) {
			this.coordinator.cancelChokes();
		}
		if (this.coordinator.checkIfInterested(this.peerControllerId, pieceIndex)) {
			this.messageSender.issueInterestedMessage();
		} else {
			this.messageSender.issueNotInterestedMessage();
		}
		this.coordinator.getClientLogger().storeHaveLog(this.peerControllerId, pieceIndex);
	}

	private void handleBitFieldMessage(BitSet bset) {
		this.processBitFieldMessage(bset);
		if (!this.coordinator.hasFile()) {
			if (this.coordinator.checkIfInterested(this.peerControllerId, -1)) {
				this.messageSender.issueInterestedMessage();
			} else {
				this.messageSender.issueNotInterestedMessage();
			}
		}
	}

	private void handleRequestMessage(ActualMessage msg) {
		if (this.coordinator.getUnchokedList().contains(this.peerControllerId)
				|| (this.coordinator.getOptimisticUnchokedPeer() != null && this.coordinator.getOptimisticUnchokedPeer().compareTo(this.peerControllerId) == 0)) {
			int chunkIndex = msg.getPieceIndexFromPayload();
			this.messageSender.transmitPieceMessage(chunkIndex, this.coordinator.readFromFile(chunkIndex));
		}
	}

	private void handlePieceMessage(ActualMessage msg) {
		int chunkIndex = msg.getPieceIndexFromPayload();
		byte[] chunk = msg.getPieceFromPayload();
		this.coordinator.writeToFile(chunk, chunkIndex);
		this.coordinator.updatePieceAvailability(this.coordinator.getPeerID(), chunkIndex);
		this.chunkDownloadRate++;
		Boolean allPeersAreDone = this.coordinator.checkIfAllPeersAreDone();
		this.coordinator.getClientLogger().storeDownloadedPieceLog(this.peerControllerId, chunkIndex, this.coordinator.getCompletedPieceCount());
		this.coordinator.setRequestedInfo(chunkIndex, null);
		this.coordinator.broadcastHave(chunkIndex);
		if (this.coordinator.getAvailabilityOf(this.coordinator.getPeerID()).cardinality() != this.coordinator
				.getPieceCount()) {
			int nextIndex = this.coordinator.checkForRequested(this.peerControllerId, chunkIndex);
			if(!this.choked){
				if (nextIndex != -1) {
					this.messageSender.issueRequestMessage(nextIndex);
				} else {
					if(!this.coordinator.checkIfInterested(this.peerControllerId, -1))
						this.messageSender.issueNotInterestedMessage();
					else
						this.messageSender.issueInterestedMessage();
				}}
			else{
				if(nextIndex != -1)
					this.coordinator.setRequestedInfo(nextIndex, null);
			}
		} else {
			this.coordinator.getClientLogger().storeTheDownloadCompleteLog();
			if (allPeersAreDone) {
				this.coordinator.cancelChokes();
			}
			else{
				this.messageSender.issueNotInterestedMessage();}
		}
	}
	public synchronized void transmitMessage(byte[] obj) {
		try {
			this.out_stream.write(obj);
			this.out_stream.flush();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	public void processHandShakeMessage(byte[] message) {
		try {
			this.estMessage.readHandShakeMessage(message);
			this.peerControllerId = this.estMessage.getPeerID();
			this.coordinator.addJoinedPeer(this, this.peerControllerId);
			this.coordinator.addJoinedThreads(this.peerControllerId, Thread.currentThread());
			this.channelEstablished = true;
			if (this.intialized) {
				this.coordinator.getClientLogger().tcpConnectionLogSenderGenerator(this.peerControllerId);
			}
			else {
				this.coordinator.getClientLogger().tcpConnectionLogReceiverGenerator(this.peerControllerId);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void processBitFieldMessage(BitSet b) {
		this.coordinator.updateBitset(this.peerControllerId, b);
	}

	public int getChunkDownloadRateRate() {
		return this.chunkDownloadRate;
	}

	public void resetDownloadRate() {
		this.chunkDownloadRate = 0;
	}

}
