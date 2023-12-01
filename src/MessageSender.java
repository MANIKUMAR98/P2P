package src;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.BitSet;

public class MessageSender {

    private PeerController peerController;

    public MessageSender(PeerController peerController) {
        this.peerController = peerController;
    }

    public void issueChokeMessage() {
        try {
            sendMessage(new ActualMessage('0').buildActualMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void issueUnChokeMessage() {
        try {
            sendMessage(new ActualMessage('1').buildActualMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void issueInterestedMessage() {
        try {
            sendMessage(new ActualMessage('2').buildActualMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void issueNotInterestedMessage() {
        try {
            sendMessage(new ActualMessage('3').buildActualMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void issueHaveMessage(int pieceIndex) {
        try {
            byte[] data = ByteBuffer.allocate(4).putInt(pieceIndex).array();
            sendMessage(new ActualMessage('4', data).buildActualMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void issueBitFieldMessage(BitSet bitSet) {
        try {
            sendMessage(new ActualMessage('5', bitSet.toByteArray()).buildActualMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void issueRequestMessage(int pieceIndex) {
        try {
            byte[] data = ByteBuffer.allocate(4).putInt(pieceIndex).array();
            sendMessage(new ActualMessage('6', data).buildActualMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void transmitPieceMessage(int chunkIndex, byte[] payload) {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            byte[] data = ByteBuffer.allocate(4).putInt(chunkIndex).array();
            stream.write(data);
            stream.write(payload);
            sendMessage(new ActualMessage('7', stream.toByteArray()).buildActualMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void issueBitField() {
        try {
            BitSet coordinatorBitField = this.peerController.getCoordinator().getChunkAvailabilityOf(this.peerController.getCoordinator().getLocalPeerID());
            ActualMessage msgObj = new ActualMessage('5', coordinatorBitField.toByteArray());
            this.peerController.transmitMessage(msgObj.buildActualMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(byte[] message) {
        peerController.transmitMessage(message);
    }
}
