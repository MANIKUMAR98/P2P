

import java.util.*;

import java.io.*;
import java.nio.*;

public class ActualMessage {
    private int messageLength;
    private char messageType;
    private byte[] messagePayload;

    public ActualMessage() {

    }

    public ActualMessage(char messageType) {
        this.messageType = messageType;
        this.messageLength = 1;
        this.messagePayload = new byte[0];
    }

    public ActualMessage(char messageType, byte[] messagePayload) {
        this.messageType = messageType;
        this.messagePayload = messagePayload;
        this.messageLength = this.messagePayload.length + 1;
    }

    public byte[] generateActualMessage() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            byte[] bytes = ByteBuffer.allocate(4).putInt(this.messageLength).array();
            stream.write(bytes);
            stream.write((byte) this.messageType);
            stream.write(this.messagePayload);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return stream.toByteArray();
    }

    public void parseMessage(int len, byte[] message) {
        this.messageLength = len;
        this.messageType = messageType(message, 0);
        this.messagePayload = getMessageBody(message, 1);
    }

    public int extractIntFromByteArray(byte[] message, int start) {
        byte[] len = new byte[4];
        for (int i = 0; i < 4; i++) {
            len[i] = message[i + start];
        }
        ByteBuffer bb = ByteBuffer.wrap(len);
        return bb.getInt();
    }

    public char messageType(byte[] message, int index) {
        return (char) message[index];
    }

    public byte[] getMessageBody(byte[] message, int index) {
        byte[] resp = new byte[this.messageLength - 1];
        System.arraycopy(message, index, resp, 0, this.messageLength - 1);
        return resp;
    }

    public BitSet getBitFieldMessage() {
        BitSet bits = new BitSet();
        bits = BitSet.valueOf(this.messagePayload);
        return bits;
    }

    public int getIndexFromMessageBody() {
        return extractIntFromByteArray(this.messagePayload, 0);
    }

    public byte[] getPieceMessageFromBody() {
        int size = this.messageLength - 5;
        byte[] piece = new byte[size];
        for (int i = 0; i < size; i++) {
            piece[i] = this.messagePayload[i + 4];
        }
        return piece;
    }

    public char getMessageType() {
        return this.messageType;
    }

}
