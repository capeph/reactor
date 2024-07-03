package org.capeph.reactor;

import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.SleepingIdleStrategy;
import org.capeph.pool.MessagePool;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DispatcherTest {

    public static class TestMessage implements ReusableMessage {

        public int cleared = 0;
        public boolean flag = false;
        public int value = 0;

        public TestMessage() {
        }

        @Override
        public void clear() {
            cleared++;
            flag = false;
        }
    }

    @Test
    public void testNoMessageHandlerNoClear() {
        IdleStrategy strategy = new SleepingIdleStrategy();
        MessagePool pool = new MessagePool(m -> {});
        Dispatcher dispatch = new Dispatcher(strategy, pool, true);
        TestMessage msg = new TestMessage();
        assertThrows(IllegalStateException.class, ()-> dispatch.accept(msg));
        assertEquals(0, msg.cleared);   // no clear up function
    }


    @Test
    public void testNoMessageHandlerButClear() {
        IdleStrategy strategy = new SleepingIdleStrategy();
        MessagePool pool = new MessagePool(m -> ((TestMessage)m).clear());
        Dispatcher dispatch = new Dispatcher(strategy, pool, true);
        TestMessage msg = new TestMessage();
        assertThrows(IllegalStateException.class, ()-> dispatch.accept(msg));
        assertEquals(1, msg.cleared);   // we clear up the message anyway!
    }

    @Test
    public void testHasMessageHandlerNoClearUp() {
        IdleStrategy strategy = new SleepingIdleStrategy();
        MessagePool pool = new MessagePool(m -> {});
        Dispatcher dispatch = new Dispatcher(strategy, pool, true);
        dispatch.addMessageHandler(
                TestMessage.class,
                m -> ((TestMessage)m).flag = true);
        TestMessage msg = new TestMessage();
        assertDoesNotThrow(()-> dispatch.accept(msg));
        assertEquals(0, msg.cleared);
        assertTrue(msg.flag);
    }

    @Test
    public void testMultipleMessageHandlers() {
        IdleStrategy strategy = new SleepingIdleStrategy();
        MessagePool pool = new MessagePool(m -> {});
        Dispatcher dispatch = new Dispatcher(strategy, pool, true);
        dispatch.addMessageHandler(
                TestMessage.class,
                m -> ((TestMessage)m).value += 1);
        dispatch.addMessageHandler(
                TestMessage.class,
                m -> ((TestMessage)m).value += 5);
        TestMessage msg = new TestMessage();
        assertDoesNotThrow(()-> dispatch.accept(msg));
        assertEquals(6, msg.value);
    }

    @Test
    public void testHasMessageHandlerThreaded() throws InterruptedException {
        IdleStrategy strategy = new SleepingIdleStrategy();
        MessagePool pool = new MessagePool(m -> {});
        Dispatcher dispatch = new Dispatcher(strategy, pool, false);
        dispatch.addMessageHandler(
                TestMessage.class,
                m -> ((TestMessage)m).flag = true);
        TestMessage msg = new TestMessage();
        assertDoesNotThrow(()-> dispatch.accept(msg));

        // TODO: better wait
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() < startTime + 10000) {
            if (msg.flag) {
                break;
            }
            Thread.sleep(1);
        }
        if (System.currentTimeMillis() >= startTime + 10000) {
            fail();
        }

    }

    // TODO test multiple handlers

}