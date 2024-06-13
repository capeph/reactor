package org.capeph.reactor;

import io.aeron.logbuffer.FragmentHandler;
import org.agrona.DirectBuffer;
import org.apache.logging.log4j.Logger;

public record MessageHandler(ICodec codec, MessagePool messagePool, Dispatcher dispatcher,
                             Logger log) implements FragmentHandler {

    @Override
    public void onFragment(DirectBuffer buffer, int offset, int length, io.aeron.logbuffer.Header header) {
        ReusableMessage msg = getMessage(buffer, offset);
        if (msg != null) {
            try {
                dispatcher.accept(msg);
            } catch (Throwable th) {
                log.error("Dispatcher threw error ", th);
            } finally {
                // reuse the threadPool in dispatcher to not dispatch messages prematurely
//                messagePool.reuseMessage(msg);
                dispatcher.dispatchTask(() -> messagePool.reuseMessage(msg));
            }
        }
    }

    private ReusableMessage getMessage(DirectBuffer buffer, int offset) {
        return codec.decode(buffer, offset, messagePool);
    }
}
