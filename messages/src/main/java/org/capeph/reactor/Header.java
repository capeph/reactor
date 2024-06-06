/*
 * Copyright 2024 Peter Danielsson
 */
package org.capeph.reactor;

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;

public class Header {

    public static int length() {
        return 2 * Integer.BYTES;
    }

    /**
     * Encodes message header to buffer, returns new offset
     * @param buffer target buffer
     * @param offset where in buffer to write
     * @param type message type
     * @param version codec version
     * @return new offset
     */
    public static int writeHeader(MutableDirectBuffer buffer, int offset, int type, int version) {
        buffer.putInt(offset, type);
        buffer.putInt(offset + Integer.BYTES, version);
        return offset + length();
    }

    public static int getMessageType(DirectBuffer buffer, int offset) {
        return buffer.getInt(offset);
    }
}
