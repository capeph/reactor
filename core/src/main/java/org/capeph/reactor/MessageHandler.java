package org.capeph.reactor;

import io.aeron.logbuffer.FragmentHandler;
import io.aeron.logbuffer.Header;
import org.agrona.DirectBuffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.capeph.pool.MessagePool;

import java.util.Objects;

public final class MessageHandler implements FragmentHandler {

    private final Logger log = LogManager.getLogger(MessageHandler.class);

    private final ICodec codec;
    private final MessagePool messagePool;
    private final Dispatcher dispatcher;

    public MessageHandler(ICodec codec, MessagePool messagePool, Dispatcher dispatcher) {
        this.codec = codec;
        this.messagePool = messagePool;
        this.dispatcher = dispatcher;
    }

    @Override
    public void onFragment(DirectBuffer buffer, int offset, int length, Header header) {
        ReusableMessage msg = getMessage(buffer, offset);
        if (msg != null) {
            try {
                dispatcher.accept(msg);
            } catch (Throwable th) {
                log.error("Dispatcher threw error ", th);
            } finally {
                // reuse the threadPool in dispatcher to not dispatch messages prematurely
                dispatcher.dispatchTask(() -> messagePool.reuseMessage(msg));
            }
        }
    }

    private ReusableMessage getMessage(DirectBuffer buffer, int offset) {
        return codec.decode(buffer, offset, messagePool);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (MessageHandler) obj;
        return Objects.equals(this.codec, that.codec) &&
                Objects.equals(this.messagePool, that.messagePool) &&
                Objects.equals(this.dispatcher, that.dispatcher) &&
                Objects.equals(this.log, that.log);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codec, messagePool, dispatcher, log);
    }

    @Override
    public String toString() {
        return "MessageHandler[" +
                "codec=" + codec + ", " +
                "messagePool=" + messagePool + ", " +
                "dispatcher=" + dispatcher + ", " +
                "log=" + log + ']';
    }

}
