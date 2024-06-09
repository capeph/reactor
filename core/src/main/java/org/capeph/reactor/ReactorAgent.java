package org.capeph.reactor;

import io.aeron.Subscription;
import io.aeron.logbuffer.FragmentHandler;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.Agent;

public class ReactorAgent implements Agent {


    private final Subscription subscription;
    private final String description;
    private final FragmentHandler assembler;

    public ReactorAgent(Subscription subscription, ICodec codec, MessagePool pool, Dispatcher dispatcher, String description) {
        this.description = description;
        this.subscription = subscription;
        this.assembler = new MessageHandler(codec, pool, dispatcher);
    }


    @Override
    public int doWork() {
        return subscription.poll(assembler, 100);
    }

    @Override
    public String roleName() {
        return description;
    }

    private record MessageHandler(ICodec codec, MessagePool messagePool, Dispatcher dispatcher) implements FragmentHandler {

        @Override
        public void onFragment(DirectBuffer buffer, int offset, int length, io.aeron.logbuffer.Header header) {
            ReusableMessage msg = getMessage(buffer, offset);
            if (msg != null) {
                dispatcher.accept(msg);
                messagePool.reuseMessage(msg);
            }
        }

        public ReusableMessage getMessage(DirectBuffer buffer, int offset) {
            return codec.decode(buffer, offset, messagePool);
        }
    }

}
