package org.capeph.reactor;


import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.capeph.lookup.LookupService;
import org.capeph.pool.MessagePool;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/*
    To be able to run add this option to the VM options
    --add-opens java.base/sun.nio.ch=ALL-UNNAMED

    To make it possible to debug, increase the timeout to 10 minutes using this:
    -Daeron.debug.timeout-600s
 */
class ReactorTest {




  //  @ReactorMessage
    public static class TestMessage implements ReusableMessage {

        private String content;

        @Override
        public void clear() {

        }

        public String getContent() {
            return content;
        }

        public void setContent(String str) {
            content = str;
        }
    }

    @FunctionalInterface
    interface TriFunction<T, U, V, R> {
        R apply(T t, U u, V v);
    }

    public static class TestCodec implements ICodec {

        private static final int VERSION = 1;

        private Map<Class<? extends ReusableMessage>, Function<ReusableMessage, Integer>> lengthFuns = new HashMap<>();
        private Map<Class<? extends ReusableMessage>, TriFunction<ReusableMessage, MutableDirectBuffer, Integer, Integer>> encodeFuns = new HashMap<>();
        private Map<Integer, TriFunction<DirectBuffer, Integer, MessagePool, ? extends ReusableMessage>> decodeFun = new HashMap<>();

        public TestCodec() {
            lengthFuns.put(TestMessage.class, msg -> getTestMessageEncodedLength((TestMessage) msg));
            encodeFuns.put(TestMessage.class, (msg, buffer, offset) -> encodeTestMessage((TestMessage) msg, buffer, offset));
            decodeFun.put(1, this::decodeTestMessage);
        }

        private int getTestMessageEncodedLength(TestMessage msg) {
            return msg.getContent().length() + Integer.BYTES;
        }

        private int encodeTestMessage(TestMessage msg, MutableDirectBuffer buffer, int offset) {
            int contentOffset = Header.writeHeader(buffer, offset, 1, VERSION);
            return buffer.putStringAscii(contentOffset, msg.getContent());
        }

        private TestMessage decodeTestMessage(DirectBuffer buffer, int offset, MessagePool pool) {
            TestMessage msg = (TestMessage) pool.getMessageTemplate(TestMessage.class);
            int contentOffset = offset + Header.length();
            msg.setContent(buffer.getStringAscii(contentOffset));
            return msg;
        }

        @Override
        public Class<? extends ReusableMessage> getClassFor(int id) {
            return TestMessage.class;
        }

        @Override
        public int encodedLength(ReusableMessage msg) {
            Function<ReusableMessage, Integer> fun = lengthFuns.get(msg.getClass());
            if (fun != null) {
                return fun.apply(msg) + Header.length();
            }
            else {
                throw new IllegalArgumentException("No message length function matching " + msg.getClass());
            }
        }

        @Override
        public int encode(ReusableMessage msg, MutableDirectBuffer buffer, int offset) {
            TriFunction<ReusableMessage, MutableDirectBuffer, Integer, Integer> fun = encodeFuns.get(msg.getClass());
            if (fun != null) {
                return fun.apply(msg, buffer, offset);
            }
            else {
                throw new IllegalArgumentException("No encoder function matching " + msg.getClass());
            }
        }

        @Override
        public ReusableMessage decode(DirectBuffer buffer, int offset, MessagePool messagePool) {
            int messageType = Header.getMessageType(buffer, offset);
            return decodeFun.get(messageType).apply(buffer, offset, messagePool);
        }
    }

    @Test
    public void testRegister() throws InterruptedException {
        ICodec testCodec = new TestCodec();
        LookupService.main(new String[]{"lookup"});
        Reactor reactora = new Reactor("reactora", "localhost:10000", false, testCodec);
        Reactor reactorb = new Reactor("reactorb", "localhost:10010", false, testCodec);
        final String[] received = {""};
        reactorb.react(TestMessage.class, msg -> received[0] = ((TestMessage)msg).getContent());
        TestMessage msg = new TestMessage();
        msg.setContent("hello");
        reactora.signal(msg, "reactorb");
        Thread.sleep(1000);
        assertEquals("hello", received[0]);

    }


}