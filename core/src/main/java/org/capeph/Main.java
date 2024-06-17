/*
 * Copyright 2024 Peter Danielsson
 */

package org.capeph;

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.capeph.pool.MessagePool;
import org.capeph.reactor.*;

public class Main
{

    public static class HandleMsg implements ReusableMessage {

        public boolean value = false;

        @Override
        public void clear() {
            value = false;
        }

    }

    private static class TestCodec implements ICodec {

        private final MessagePool pool;

        public TestCodec(MessagePool pool) {
            this.pool = pool;
        }

        @Override
        public Class<? extends ReusableMessage> getClassFor(int id) {
            return HandleMsg.class;
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
        public ReusableMessage decode(DirectBuffer buffer, int offset, MessagePool messagePool) {
            HandleMsg msg = (HandleMsg) pool.getMessageTemplate(HandleMsg.class);
            if (msg.value) {
                throw new RuntimeException("Blurk!");
            }
            msg.value = true;
            return msg;
        }
    }

    public static void runLocalTest() {
        System.out.println("Reactor");

        Logger log = LogManager.getLogger();
        MessagePool pool = new MessagePool();
        ICodec codec = new TestCodec(pool);
        pool.addMessagePool(HandleMsg.class);
        Dispatcher dispatcher = new Dispatcher(true);
        dispatcher.addMessageHandler(HandleMsg.class, m -> {
            if(!((HandleMsg)m).value) {
                throw new RuntimeException("Error in the message Pool");
            }
        } );
        MessageHandler handler = new MessageHandler(codec, pool, dispatcher);
        System.out.println("starting loop");

        for (int i = 0; i < 100000; i++) {
            handler.onFragment(null, 0, 0, null);
        }

        long start = System.nanoTime();
        int its = 100000000;
        for (int i = 0; i < its; i++) {
            handler.onFragment(null, 0, 0, null);
        }
        long stop = System.nanoTime();
        System.out.println("Shutting down, total time: " + (stop-start) + " dispatch time " +  (stop-start)/its );
        dispatcher.stop();

    }



    public static void main(final String[] args) throws InterruptedException {
        System.out.println("Reactor");
        runLocalTest();
    }

}