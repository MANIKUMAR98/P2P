

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
            sendMessage(new ApplicationMessage('0').generateActualMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void issueUnChokeMessage() {
        try {
            sendMessage(new ApplicationMessage('1').generateActualMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void issueInterestedMessage() {
        try {
            sendMessage(new ApplicationMessage('2').generateActualMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void issueNotInterestedMessage() {
        try {
            sendMessage(new ApplicationMessage('3').generateActualMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void issueHaveMessage(int pieceIndex) {
        try {
            byte[] data = ByteBuffer.allocate(4).putInt(pieceIndex).array();
            sendMessage(new ApplicationMessage('4', data).generateActualMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void issueBitFieldMessage(BitSet bitSet) {
        try {
            sendMessage(new ApplicationMessage('5', bitSet.toByteArray()).generateActualMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void issueRequestMessage(int pieceIndex) {
        try {
            byte[] data = ByteBuffer.allocate(4).putInt(pieceIndex).array();
            sendMessage(new ApplicationMessage('6', data).generateActualMessage());
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
            sendMessage(new ApplicationMessage('7', stream.toByteArray()).generateActualMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void issueBitField() {
        try {
            BitSet coordinatorBitField = this.peerController.getCoordinator().getChunkAvailabilityOf(this.peerController.getCoordinator().getLocalPeerID());
            ApplicationMessage msgObj = new ApplicationMessage('5', coordinatorBitField.toByteArray());
            this.peerController.transmitMessage(msgObj.generateActualMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(byte[] message) {
        peerController.transmitMessage(message);
    }
}
