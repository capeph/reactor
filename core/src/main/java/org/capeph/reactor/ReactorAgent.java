package org.capeph.reactor;

import io.aeron.Subscription;
import io.aeron.logbuffer.FragmentHandler;
import org.agrona.concurrent.Agent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ReactorAgent implements Agent {

    private final Logger log = LogManager.getLogger(Dispatcher.class);


    private final Subscription subscription;
    private final String description;
    private final FragmentHandler assembler;

    public ReactorAgent(Subscription subscription, ICodec codec, MessagePool pool, Dispatcher dispatcher, String description) {
        this.description = description;
        this.subscription = subscription;
        this.assembler = new MessageHandler(codec, pool, dispatcher, log);
    }


    @Override
    public int doWork() {
        return subscription.poll(assembler, 100);
    }

    @Override
    public String roleName() {
        return description;
    }

}
