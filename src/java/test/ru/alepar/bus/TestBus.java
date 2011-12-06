package ru.alepar.bus;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JMock.class)
public class TestBus {

    private final Mockery mockery = new JUnit4Mockery();

    @Test
    public void deliversMessagesBoundToClassOnlyKey() throws Exception {
        final Bus bus = new Bus();
        final Key<TestMessage> key = new Key<TestMessage>(TestMessage.class);

        final MessageListener<TestMessage> listener = mockery.mock(MessageListener.class);
        mockery.checking(new Expectations() {{
            one(listener).onMessage(with(any(TestMessage.class)));
        }});
        
        bus.addListener(key, listener);
        bus.send(key, new TestMessage());
    }

    @Test
    public void doesNotDeliverMessagesToDifferentClassListeners() throws Exception {
        final Bus bus = new Bus();
        final Key<TestMessage> key = new Key<TestMessage>(TestMessage.class);
        final Key<AnotherTestMessage> anotherKey = new Key<AnotherTestMessage>(AnotherTestMessage.class);

        final MessageListener<TestMessage> listener = mockery.mock(MessageListener.class);

        bus.addListener(key, listener);
        bus.send(anotherKey, new AnotherTestMessage());
    }



    private static class TestMessage implements Message {}
    private static class AnotherTestMessage implements Message {}
}
