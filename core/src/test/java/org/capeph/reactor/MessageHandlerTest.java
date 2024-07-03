package org.capeph.reactor;

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.SleepingIdleStrategy;
import org.capeph.pool.MessagePool;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class MessageHandlerTest {

    public static class HandleMsg implements ReusableMessage {

        public boolean value = false;

        @Override
        public void clear() {
            value = false;
        }
    }

    public HandleMsg getMessage(MessagePool pool) {
        HandleMsg msg = (HandleMsg) pool.getMessageTemplate(HandleMsg.class);
        if (msg.value) {
            fail();
        }
        msg.value = true;
        return msg;
    }

    private class TestCodec implements ICodec {

        @Override
        public Class<? extends ReusableMessage> getClassFor(int id) {
            return null;
        }

        @Override
        public int encodedLength(ReusableMessage msg) {
            return 0;
        }

        @Override
        public int encode(ReusableMessage msg, MutableDirectBuffer buffer, int offset) {
            return 0;
        }

        @Override
        public void clear(ReusableMessage msg) {
            ((HandleMsg)msg).clear();
        }

        @Override
        public ReusableMessage decode(DirectBuffer buffer, int offset, MessagePool messagePool) {
            return getMessage(messagePool);
        }
    }

    @Test
    public void testMessageHandler() {
        MessagePool pool = new MessagePool(m -> ((HandleMsg)m).clear());
        pool.addMessagePool(HandleMsg.class);
        ICodec codec = new TestCodec();
        IdleStrategy strategy = new SleepingIdleStrategy();
        Dispatcher dispatcher = new Dispatcher(strategy, pool, true);
        dispatcher.addMessageHandler(HandleMsg.class, m -> assertTrue(((HandleMsg)m).value));
        MessageHandler handler = new MessageHandler(codec, pool, dispatcher);

        for (int i = 0; i < 100000; i++) {
            handler.onFragment(null, 0, 0, null);
        }

        int its = 1000000;
        long start = System.nanoTime();
        for (int i = 0; i < its; i++) {
            handler.onFragment(null, 0, 0, null);
        }
        long end = System.nanoTime();
        System.out.println("Avg time = " + ((double)(end - start))/its + "ns");
    }

}