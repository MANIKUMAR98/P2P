package src;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class HandshakeMessage {
    private String handshakeHeader;
    private String peerId;

    public HandshakeMessage(String peerId) {
        this.handshakeHeader = Constants.HANDSHAKE_HEADER;
        this.peerId = peerId;
    }

    public byte[] generateHandshakeMessage() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            stream.write(this.handshakeHeader.getBytes(StandardCharsets.UTF_8));
            stream.write(new byte[10]);
            stream.write(this.peerId.getBytes(StandardCharsets.UTF_8));
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        return stream.toByteArray();
    }

    public String getHandshakeHeader() {
        return handshakeHeader;
    }

    public void setHandshakeHeader(String handshakeHeader) {
        this.handshakeHeader = handshakeHeader;
    }

    public String getPeerId() {
        return peerId;
    }

    public void setPeerId(String peerId) {
        this.peerId = peerId;
    }

    public void pareseHandshakeMessage(byte[] data){
        String handshakeMessage = new String(data, StandardCharsets.UTF_8);
        this.peerId = handshakeMessage.substring(28,32);
    }
}
