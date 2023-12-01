package src;

public class Constants {
        public enum MessageType {
            CHOKE('0'), UNCHOKE('1'), INTERESTED('2'), NOT_INTERESTED('3'),
            HAVE('4'), BITFIELD('5'), REQUEST('6'), PIECE('7');

            private final char code;

            MessageType(char code) {
                this.code = code;
            }

            public char getCode() {
                return code;
            }

            public static MessageType fromCode(char code) {
                for (MessageType messageType : MESSAGE_TYPES) {
                    if (messageType.getCode() == code) {
                        return messageType;
                    }
                }
                throw new IllegalArgumentException("No MessageType with code: " + code);
            }

            private static final MessageType[] MESSAGE_TYPES = MessageType.values();
        }

        // Add other constants or enums as needed
        public static final String NUMBER_OF_PREFERRED_NEIGHBORS = "NumberOfPreferredNeighbors";
        public static final String UNCHOKING_INTERVAL = "UnchokingInterval";
        public static final String OPTIMISTIC_UNCHOKING_INTERVAL = "OptimisticUnchokingInterval";
        public static final String FILE_NAME = "FileName";
        public static final String FILE_SIZE = "FileSize";
        public static final String PIECE_SIZE = "PieceSize";
        public static final String HANDSHAKE_HEADER = "P2PFILESHARINGPROJ";
}
