/*
 * Copyright 2024 Peter Danielsson
 */
package org.capeph.reactor;

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;

public interface ICodec {

    int encodedLength(ReusableMessage msg);

    int encode(ReusableMessage msg, MutableDirectBuffer buffer, int offset);

    ReusableMessage decode(DirectBuffer buffer, int offset, MessagePool messagePool);

}
