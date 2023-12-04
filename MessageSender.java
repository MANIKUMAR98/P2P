

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.BitSet;

public class MessageSender {

    private PeerController peerController;

    public MessageSender(PeerController peerController) {
        this.peerController = peerController;
    }

    public void issueChokeMessage() {
        // send choke message
        try {
            sendMessage(new ApplicationMessage('0').generateActualMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void issueUnChokeMessage() {
        // send unchike message
        try {
            sendMessage(new ApplicationMessage('1').generateActualMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void issueInterestedMessage() {
        // send interested message
        try {
            sendMessage(new ApplicationMessage('2').generateActualMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void issueNotInterestedMessage() {
        //send not interested message
        try {
            sendMessage(new ApplicationMessage('3').generateActualMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void issueHaveMessage(int pieceIndex) {
        //send have message
        try {
            byte[] data = ByteBuffer.allocate(4).putInt(pieceIndex).array();
            sendMessage(new ApplicationMessage('4', data).generateActualMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void issueBitFieldMessage(BitSet bitSet) {
        //send BItFiles message
        try {
            sendMessage(new ApplicationMessage('5', bitSet.toByteArray()).generateActualMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void issueRequestMessage(int pieceIndex) {
        // send request message for a chunk
        try {
            byte[] data = ByteBuffer.allocate(4).putInt(pieceIndex).array();
            sendMessage(new ApplicationMessage('6', data).generateActualMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void transmitPieceMessage(int chunkIndex, byte[] payload) {
        //send chunk
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
        //send BitFiled Message
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
