package org.capeph.reactor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DispatcherTest {

    private class TestMessage implements ReusableMessage {

        public int cleared = 0;
        public boolean flag = false;
        public int value = 0;

        @Override
        public void clear() {
            cleared++;
            flag = false;
        }
    }

    @Test
    public void testNoMessageHandler() {
        Dispatcher dispatch = new Dispatcher(true);
        TestMessage msg = new TestMessage();
        assertThrows(IllegalStateException.class, ()-> dispatch.accept(msg));
        assertEquals(0, msg.cleared);
    }

    @Test
    public void testHasMessageHandler() {
        Dispatcher dispatch = new Dispatcher(true);
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
        Dispatcher dispatch = new Dispatcher(true);
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
        Dispatcher dispatch = new Dispatcher(false);
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