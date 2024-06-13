package org.capeph.reactor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

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

    @Test
    public void testMessageHandler() {
        Logger log = LogManager.getLogger("TestLogger");
        ICodec codec = Mockito.mock(ICodec.class);
        MessagePool pool = new MessagePool();
        pool.addMessagePool(HandleMsg.class);
        when(codec.decode(any(), anyInt(), any())).thenAnswer((Answer<HandleMsg>) invocationOnMock -> getMessage(pool));
        Dispatcher dispatcher = new Dispatcher(false);
        dispatcher.addMessageHandler(HandleMsg.class, m -> assertTrue(((HandleMsg)m).value));
        MessageHandler handler = new MessageHandler(codec, pool, dispatcher, log);

        for (int i = 0; i < 1000000; i++) {
            handler.onFragment(null, 0, 0, null);
        }
    }

}