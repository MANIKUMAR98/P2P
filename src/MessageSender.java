package src;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.BitSet;

public class MessageSender {

    private PeerHandler peerHandler;

    public MessageSender(PeerHandler peerHandler) {
        this.peerHandler = peerHandler;
    }

    public void sendChokedMessage() {
        try {
            sendMessage(new ActualMessage('0').buildActualMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendUnChokedMessage() {
        try {
            sendMessage(new ActualMessage('1').buildActualMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendInterestedMessage() {
        try {
            sendMessage(new ActualMessage('2').buildActualMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendNotInterestedMessage() {
        try {
            sendMessage(new ActualMessage('3').buildActualMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendHaveMessage(int pieceIndex) {
        try {
            byte[] bytes = ByteBuffer.allocate(4).putInt(pieceIndex).array();
            sendMessage(new ActualMessage('4', bytes).buildActualMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendBitFieldMessage(BitSet bitSet) {
        try {
            sendMessage(new ActualMessage('5', bitSet.toByteArray()).buildActualMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendRequestMessage(int pieceIndex) {
        try {
            byte[] bytes = ByteBuffer.allocate(4).putInt(pieceIndex).array();
            sendMessage(new ActualMessage('6', bytes).buildActualMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendPieceMessage(int pieceIndex, byte[] payload) {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            byte[] bytes = ByteBuffer.allocate(4).putInt(pieceIndex).array();
            stream.write(bytes);
            stream.write(payload);
            sendMessage(new ActualMessage('7', stream.toByteArray()).buildActualMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendBitField() {
        try {
            BitSet myAvailability = this.peerHandler.getCoordinator().getAvailabilityOf(this.peerHandler.getCoordinator().getPeerID());
            ActualMessage am = new ActualMessage('5', myAvailability.toByteArray());
            this.peerHandler.send(am.buildActualMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(byte[] message) {
        peerHandler.send(message);
    }
}
