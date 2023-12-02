package src;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.BitSet;

public class PeerController implements Runnable {
	private Socket communicationChannel;
	private PeerManager coordinator;

	public boolean choked = true;
	private String peerControllerId;
	private boolean  channelEstablished = false;
	private boolean  intialized = false;
	private HandshakeMessage handshakeMessage;
	private volatile int chunkDownloadRate = 0;
	private volatile ObjectOutputStream out_stream;
	private volatile ObjectInputStream input_stream;


	public MessageSender messageSender;


	public PeerController(Socket communicationChannel, PeerManager coordinator) {
		this.communicationChannel = communicationChannel;
		this.coordinator = coordinator;
		this.messageSender = new MessageSender(this);
		initializeIOStreams();
		this.handshakeMessage = new HandshakeMessage(this.coordinator.getLocalPeerID());

	}

	public PeerManager getCoordinator() {
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
			byte[] msg = this.handshakeMessage.generateHandshakeMessage();
			this.out_stream.write(msg);
			this.out_stream.flush();
			while (true) {
				if (!this.channelEstablished) {
					byte[] receivedData = new byte[32];
					this.input_stream.readFully(receivedData);
					this.processHandShakeMessage(receivedData);
					if (this.coordinator.hasSourceFile() || this.coordinator.getChunkAvailabilityOf(this.coordinator.getLocalPeerID()).cardinality() > 0) {
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
				this.coordinator.resetRequestedChunkInfo(this.peerControllerId);
				this.coordinator.getChunkAvailabilityOf(this.peerControllerId).set(0, this.coordinator.getChunkCount());

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
		this.coordinator.resetRequestedChunkInfo(this.peerControllerId);
		this.coordinator.getClientLogger().storeChokingNeighborLog(this.peerControllerId);
	}

	private void handleUnchokeMessage() {
		this.coordinator.getClientLogger().storeUnchokedNeighborLog(this.peerControllerId);
		this.choked = false;
		int chunkIndex = this.coordinator.checkChunksRequested(this.peerControllerId, -1);
		if (chunkIndex == -1) {
			if(!this.coordinator.checkIfInterested(this.peerControllerId, -1)) {
				this.messageSender.issueNotInterestedMessage();
			}
			else {
				this.messageSender.issueInterestedMessage();
			}
		} else {
			this.coordinator.setChunkRequestedInfo(chunkIndex, this.peerControllerId);
			this.messageSender.issueRequestMessage(chunkIndex);
		}
	}

	private void handleInterestedMessage() {
		this.coordinator.appendToInterestedList(this.peerControllerId);
		this.coordinator.getClientLogger().storeInterestedLog(this.peerControllerId);
	}

	private void handleNotInterestedMessage() {
		this.coordinator.setPeerAsNotInterested(this.peerControllerId);
		this.coordinator.getClientLogger().storeNotInterestedLog(this.peerControllerId);
	}

	private void handleHaveMessage(int pieceIndex) {
		this.coordinator.updateChunkAvailability(this.peerControllerId, pieceIndex);
		if (this.coordinator.areAllPeersDone()) {
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
		if (!this.coordinator.hasSourceFile()) {
			if (this.coordinator.checkIfInterested(this.peerControllerId, -1)) {
				this.messageSender.issueInterestedMessage();
			} else {
				this.messageSender.issueNotInterestedMessage();
			}
		}
	}

	private void handleRequestMessage(ActualMessage msg) {
		if (this.coordinator.getUnChokedPeerList().contains(this.peerControllerId)
				|| (this.coordinator.getOptimisticUnChokedPeer() != null && this.coordinator.getOptimisticUnChokedPeer().compareTo(this.peerControllerId) == 0)) {
			int chunkIndex = msg.getPieceIndexFromPayload();
			this.messageSender.transmitPieceMessage(chunkIndex, this.coordinator.inputFromFileSync(chunkIndex));
		}
	}

	private void handlePieceMessage(ActualMessage msg) {
		int chunkIndex = msg.getPieceIndexFromPayload();
		byte[] chunk = msg.getPieceFromPayload();
		this.coordinator.outputToFileSync(chunk, chunkIndex);
		this.coordinator.updateChunkAvailability(this.coordinator.getLocalPeerID(), chunkIndex);
		this.chunkDownloadRate++;
		Boolean allPeersAreDone = this.coordinator.areAllPeersDone();
		this.coordinator.getClientLogger().storeDownloadedPieceLog(this.peerControllerId, chunkIndex, this.coordinator.getAvailableChunkCount());
		this.coordinator.setChunkRequestedInfo(chunkIndex, null);
		this.coordinator.sendHave(chunkIndex);
		if (this.coordinator.getChunkAvailabilityOf(this.coordinator.getLocalPeerID()).cardinality() != this.coordinator
				.getChunkCount()) {
			int nextIndex = this.coordinator.checkChunksRequested(this.peerControllerId, chunkIndex);
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
					this.coordinator.setChunkRequestedInfo(nextIndex, null);
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
			this.handshakeMessage.parseHandshakeMessage(message);
			this.peerControllerId = this.handshakeMessage.getPeerId();
			this.coordinator.addConnectedPeer(this, this.peerControllerId);
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
		this.coordinator.updateChunkBitsetAvailability(this.peerControllerId, b);
	}

	public int getChunkDownloadRateRate() {
		return this.chunkDownloadRate;
	}

	public void resetDownloadRate() {
		this.chunkDownloadRate = 0;
	}

}
