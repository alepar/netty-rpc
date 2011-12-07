package ru.alepar.bus;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.alepar.bus.api.*;
import ru.alepar.bus.impl.FullBroadcastingBus;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertThat;

@RunWith(JMock.class)
public class TestBus {

    private final Mockery mockery = new JUnit4Mockery();

    @Test
    public void deliversMessagesBoundToClassOnlyKey() throws Exception {
        final Bus bus = new FullBroadcastingBus();
        final Key<TestMessage> key = new Key<TestMessage>(TestMessage.class);

        final MessageListener<TestMessage> listener = mockery.mock(MessageListener.class);
        mockery.checking(new Expectations() {{
            one(listener).onMessage(with(any(Context.class)));
        }});
        
        bus.addListener(key, listener);
        bus.send(key, new TestMessage());
    }

    @Test
    public void doesNotDeliverMessagesToDifferentClassListeners() throws Exception {
        final Bus bus = new FullBroadcastingBus();
        final Key<TestMessage> key = new Key<TestMessage>(TestMessage.class);
        final Key<AnotherTestMessage> anotherKey = new Key<AnotherTestMessage>(AnotherTestMessage.class);

        final MessageListener<TestMessage> listener = mockery.mock(MessageListener.class);

        bus.addListener(key, listener);
        bus.send(anotherKey, new AnotherTestMessage());
    }

    @Test
    public void waitsForResponseProperly() throws Exception {
        final long DELAY = 500l;

        final Bus bus = new FullBroadcastingBus();
        final Key<TestMessage> key = new Key<TestMessage>(TestMessage.class);

        bus.addListener(key, new MessageListener<TestMessage>() {
            @Override
            public void onMessage(final Context<TestMessage> ctx) {
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        ctx.respondWith(AnotherTestMessage.class, new AnotherTestMessage());
                    }
                }, DELAY);
            }
        });

        final Date start = new Date();
        final AnotherTestMessage resp = bus.scheduleSend(key, new TestMessage()).waitForResponse(AnotherTestMessage.class);
        final Date end = new Date();

        assertThat(Math.abs(end.getTime() - start.getTime() - DELAY), lessThanOrEqualTo(50l));
    }

    private static class TestMessage implements Message {}
    private static class AnotherTestMessage implements Message {}
}
