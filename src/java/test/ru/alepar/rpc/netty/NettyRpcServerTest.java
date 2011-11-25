package ru.alepar.rpc.netty;

import java.io.Serializable;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.alepar.rpc.Remote;
import ru.alepar.rpc.Inject;
import ru.alepar.rpc.RpcClient;
import ru.alepar.rpc.RpcServer;
import ru.alepar.rpc.exception.ProtocolException;
import ru.alepar.rpc.exception.RemoteException;
import ru.alepar.rpc.exception.TransportException;

import static java.lang.Thread.sleep;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static ru.alepar.rpc.netty.Config.BIND_ADDRESS;
import static ru.alepar.rpc.netty.Config.TIMEOUT;

@RunWith(JMock.class)
public class NettyRpcServerTest {

    private final Mockery mockery = new JUnit4Mockery();

    @Test(timeout = TIMEOUT)
    public void serverShutdownDoesNotHangIfThereAreStillClientsConnected() throws Exception {
        // it can hang if server does not close client channels before releasing bootstrap resources
        final RpcServer server = new NettyRpcServer(BIND_ADDRESS);
        final RpcClient client = new NettyRpcClient(BIND_ADDRESS);

        server.shutdown();
        client.shutdown();
    }

    @Test(timeout = TIMEOUT)
    public void invokesMethodsWithNoParamsAndVoidReturnType() throws Exception {
        final NoParamsVoidReturn impl = mockery.mock(NoParamsVoidReturn.class);

        mockery.checking(new Expectations() {{
            one(impl).go();
        }});

        final RpcServer server = new NettyRpcServer(BIND_ADDRESS);
        server.addImplementation(NoParamsVoidReturn.class, impl);

        final RpcClient client = new NettyRpcClient(BIND_ADDRESS);
        final NoParamsVoidReturn proxy = client.getImplementation(NoParamsVoidReturn.class);

        try {
            proxy.go();
            giveTimeForMessagesToBeProcessed();
        } finally {
            client.shutdown();
            server.shutdown();
        }
    }

    @Test(timeout = TIMEOUT, expected = ProtocolException.class)
    public void doNotAllowMethodsWithNonVoidReturnTypeToBeProxied() throws Exception {
        final NoParamsIntegerReturn impl = mockery.mock(NoParamsIntegerReturn.class);

        final RpcServer server = new NettyRpcServer(BIND_ADDRESS);
        server.addImplementation(NoParamsIntegerReturn.class, impl);

        final RpcClient client = new NettyRpcClient(BIND_ADDRESS);
        final NoParamsIntegerReturn proxy = client.getImplementation(NoParamsIntegerReturn.class);

        try {
            proxy.go();
        } finally {
            client.shutdown();
            server.shutdown();
        }
    }

    @Test(timeout = TIMEOUT)
    public void subsequentCallsWork() throws Exception {
        final NoParamsVoidReturn impl = mockery.mock(NoParamsVoidReturn.class);

        mockery.checking(new Expectations() {{
            one(impl).go();
            one(impl).go();
        }});

        final RpcServer server = new NettyRpcServer(BIND_ADDRESS);
        server.addImplementation(NoParamsVoidReturn.class, impl);

        final RpcClient client = new NettyRpcClient(BIND_ADDRESS);
        final NoParamsVoidReturn proxy = client.getImplementation(NoParamsVoidReturn.class);

        try {
            proxy.go();
            proxy.go();
            giveTimeForMessagesToBeProcessed();
        } finally {
            client.shutdown();
            server.shutdown();
        }
    }

    @Test(timeout = TIMEOUT)
    public void methodParametersArePassedToRemoteSideProperly() throws Exception {
        final IntegerParam impl = mockery.mock(IntegerParam.class);
        final Integer param = 5;

        mockery.checking(new Expectations() {{
            one(impl).go(with(equalTo(param)));
        }});

        final RpcServer server = new NettyRpcServer(BIND_ADDRESS);
        server.addImplementation(IntegerParam.class, impl);

        final RpcClient client = new NettyRpcClient(BIND_ADDRESS);
        final IntegerParam proxy = client.getImplementation(IntegerParam.class);

        try {
            proxy.go(param);
            giveTimeForMessagesToBeProcessed();
        } finally {
            client.shutdown();
            server.shutdown();
        }
    }

    @Test(timeout = TIMEOUT)
    public void primitiveMethodParametersArePassedToRemoteSideProperly() throws Exception {
        final IntLongParam impl = mockery.mock(IntLongParam.class);
        final int paramInt = 5;
        final long paramLong = 10l;

        mockery.checking(new Expectations() {{
            one(impl).go(with(equalTo(paramInt)), with(equalTo(paramLong)));
        }});

        final RpcServer server = new NettyRpcServer(BIND_ADDRESS);
        server.addImplementation(IntLongParam.class, impl);

        final RpcClient client = new NettyRpcClient(BIND_ADDRESS);
        final IntLongParam proxy = client.getImplementation(IntLongParam.class);

        try {
            proxy.go(paramInt, paramLong);
            giveTimeForMessagesToBeProcessed();
        } finally {
            client.shutdown();
            server.shutdown();
        }
    }

    @Test(timeout = TIMEOUT)
    public void choosesOverloadedMethodByCompileTimeTypesAsOpposedToRuntimeTypes() throws Exception {
        final OverloadedString impl = mockery.mock(OverloadedString.class);
        final String s = "some string";

        mockery.checking(new Expectations() {{
            one(impl).go(with(any(Serializable.class)));
        }});

        final RpcServer server = new NettyRpcServer(BIND_ADDRESS);
        server.addImplementation(OverloadedString.class, impl);

        final RpcClient client = new NettyRpcClient(BIND_ADDRESS);
        final OverloadedString proxy = client.getImplementation(OverloadedString.class);

        try {
            proxy.go((Serializable) s);
            giveTimeForMessagesToBeProcessed();
        } finally {
            client.shutdown();
            server.shutdown();
        }
    }

    @Test(timeout = TIMEOUT, expected = ProtocolException.class)
    public void throwsProtocolExceptionIfCannotSerializeParams() throws Exception {
        final NonSerializable impl = mockery.mock(NonSerializable.class);

        final RpcServer server = new NettyRpcServer(BIND_ADDRESS);
        server.addImplementation(NonSerializable.class, impl);

        final RpcClient client = new NettyRpcClient(BIND_ADDRESS);
        final NonSerializable proxy = client.getImplementation(NonSerializable.class);

        try {
            proxy.param("", new Object());
        } finally {
            client.shutdown();
            server.shutdown();
        }
    }

    @Test(timeout = TIMEOUT)
    public void throwsTransportExceptionIfConnectionIsAbruptlyTerminated() throws Throwable {
        final InfinteWaiter impl = new InfinteWaiter() {
            @SuppressWarnings({"InfiniteLoopStatement"})
            @Override
            public void hang() {
                try {
                    sleep(1000L);
                } catch (InterruptedException ignored) {}
            }
        };

        final RpcServer server = new NettyRpcServer(BIND_ADDRESS);
        server.addImplementation(InfinteWaiter.class, impl);

        final RpcClient client = new NettyRpcClient(BIND_ADDRESS, 30l);
        final InfinteWaiter proxy = client.getImplementation(InfinteWaiter.class);

        final ExceptionSavingListener listener = new ExceptionSavingListener();
        client.addExceptionListener(listener);

        try {
            proxy.hang();
            server.shutdown();
            giveTimeForMessagesToBeProcessed();
            assertThat(listener.lastException(), notNullValue());
            assertThat(listener.lastException().getClass(), equalTo((Class)TransportException.class));
        } finally {
            client.shutdown();
        }
    }

    @Test(timeout = TIMEOUT)
    public void exceptionsFromServerSideImplementationArePassedToClient() throws Throwable {
        final ThrowableThrower impl = new ThrowableThrower() {
            @Override
            public void go() throws Throwable {
                throw new IllegalAccessException("some exception");
            }
        };
        final RpcClient.ExceptionListener listener = mockery.mock(RpcClient.ExceptionListener.class);
        mockery.checking(new Expectations(){{
            one(listener).onExceptionCaught(with(any(RemoteException.class)));
        }});

        final RpcServer server = new NettyRpcServer(BIND_ADDRESS);
        server.addImplementation(ThrowableThrower.class, impl);

        final RpcClient client = new NettyRpcClient(BIND_ADDRESS);
        final ThrowableThrower proxy = client.getImplementation(ThrowableThrower.class);
        client.addExceptionListener(listener);

        try {
            proxy.go();
            giveTimeForMessagesToBeProcessed();
        } finally {
            client.shutdown();
            server.shutdown();
        }
    }

    @Test(timeout = TIMEOUT)
    public void exceptionsFromClientSideImplementationArePassedToServer() throws Throwable {
        final ThrowableThrower impl = new ThrowableThrower() {
            @Override
            public void go() throws Throwable {
                throw new IllegalAccessException("some exception");
            }
        };
        final RpcServer.ExceptionListener listener = mockery.mock(RpcServer.ExceptionListener.class);
        mockery.checking(new Expectations() {{
            one(listener).onExceptionCaught(with(any(Remote.class)), with(any(RemoteException.class)));
        }});

        final RpcServer server = new NettyRpcServer(BIND_ADDRESS);
        server.addExceptionListener(listener);
        server.addClass(NoParamsVoidReturn.class, CallClientBack.class);

        final RpcClient client = new NettyRpcClient(BIND_ADDRESS);
        client.addImplementation(ThrowableThrower.class, impl);

        final NoParamsVoidReturn proxy = client.getImplementation(NoParamsVoidReturn.class);

        try {
            proxy.go();
            giveTimeForMessagesToBeProcessed();
        } finally {
            client.shutdown();
            server.shutdown();
        }
    }

    @Test(timeout = TIMEOUT)
    public void feedbackToClientWorks() throws Exception {
        final String MSG = "echoed-hi";

        final RpcServer server = new NettyRpcServer(BIND_ADDRESS);
        server.addClass(SomeServerApi.class, SomeServerImpl.class);

        final ClientApi mockClient = mockery.mock(ClientApi.class);
        mockery.checking(new Expectations() {{
            one(mockClient).feedback(MSG);
        }});

        final RpcClient client = new NettyRpcClient(BIND_ADDRESS);
        client.addImplementation(ClientApi.class, mockClient);
        
        final SomeServerApi proxy = client.getImplementation(SomeServerApi.class);

        try {
            proxy.go(MSG);
            giveTimeForMessagesToBeProcessed();
        } finally {
            client.shutdown();
            server.shutdown();
        }
    }

    @Test(timeout = TIMEOUT)
    public void serverImplementationsAreCachedIeStatePersistsAcrossClientCalls() throws Exception {
        final String MSG = "some state";

        final RpcServer server = new NettyRpcServer(BIND_ADDRESS);
        server.addClass(State.class, ServerState.class);

        final RpcClient client = new NettyRpcClient(BIND_ADDRESS);
        final ClientState clientState = new ClientState();
        client.addImplementation(State.class, clientState);
        
        final State serverStateProxy = client.getImplementation(State.class);

        try {
            serverStateProxy.set(MSG);
            serverStateProxy.get();
            giveTimeForMessagesToBeProcessed();
            assertThat(clientState.state, equalTo(MSG));
        } finally {
            client.shutdown();
            server.shutdown();
        }
    }

    @Test(timeout = TIMEOUT)
    public void differentClientsHaveSeparateStateOnServerSide() throws Exception {
        final String MSG1 = "one state";
        final String MSG2 = "second state";

        final RpcServer server = new NettyRpcServer(BIND_ADDRESS);
        server.addClass(State.class, ServerState.class);

        final RpcClient clientOne = new NettyRpcClient(BIND_ADDRESS);
        final ClientState clientOneState = new ClientState();
        clientOne.addImplementation(State.class, clientOneState);
        final State proxyOne = clientOne.getImplementation(State.class);
        
        final RpcClient clientTwo = new NettyRpcClient(BIND_ADDRESS);
        final ClientState clientTwoState = new ClientState();
        clientTwo.addImplementation(State.class, clientTwoState);
        final State proxyTwo = clientTwo.getImplementation(State.class);

        try {
            proxyOne.set(MSG1);
            proxyTwo.set(MSG2);
            proxyOne.get();
            proxyTwo.get();
            giveTimeForMessagesToBeProcessed();
            assertThat(clientOneState.state, equalTo(MSG1));
            assertThat(clientTwoState.state, equalTo(MSG2));
        } finally {
            clientOne.shutdown();
            clientTwo.shutdown();
            server.shutdown();
        }
    }

    @Test(timeout = TIMEOUT)
    public void serverNotifiesAboutClientConnectsAndDisconnects() throws Exception {
        final RpcServer server = new NettyRpcServer(BIND_ADDRESS);
        final RpcServer.ClientListener mock = mockery.mock(RpcServer.ClientListener.class);
        server.addClientListener(mock);

        mockery.checking(new Expectations(){{
            one(mock).onClientConnect(with(any(Remote.class))); // don't know the id yet
        }});

        final RpcClient client = new NettyRpcClient(BIND_ADDRESS);

        mockery.checking(new Expectations(){{
            one(mock).onClientDisconnect(with(any(Remote.class))); // matcher, matching remote.getClientId()
        }});

        client.shutdown();
        giveTimeForMessagesToBeProcessed();
        server.shutdown();
    }

    @Test
    public void clientGetsIdOnAcknowledgement() throws Exception {
        final RpcServer server = new NettyRpcServer(BIND_ADDRESS);
        final RpcClient client = new NettyRpcClient(BIND_ADDRESS);

        try {
            assertThat(server.getClient(client.getClientId()), not(nullValue()));
        } finally {
            client.shutdown();
            server.shutdown();
        }
    }

    private interface NoParamsVoidReturn {
        void go();
    }
    private interface NoParamsIntegerReturn {
        Integer go();
    }

    private interface IntegerParam {
        void go(Integer i);
    }
    private interface IntLongParam {
        void go(int i, long l);
    }

    private interface OverloadedString {
        void go(String s); // though unused, is vital for correctnes of corresponding unit test
        void go(Serializable s);
    }
    private interface NonSerializable {
        void param(String s, Object o);
    }
    private interface InfinteWaiter {
        void hang();
    }
    private interface ThrowableThrower {
        void go() throws Throwable;
    }

    private interface SomeServerApi {
        void go(String msg);
    }

    public static class SomeServerImpl implements SomeServerApi {

        private final Remote remote;

        public SomeServerImpl(@Inject Remote remote) {
            this.remote = remote;
        }

        @Override
        public void go(String msg) {
            ClientApi clientProxy = remote.getProxyFactory().getProxy(ClientApi.class);
            clientProxy.feedback(msg);
        }
    }

    private interface ClientApi {
        void feedback(String msg);
    }

    private static void giveTimeForMessagesToBeProcessed() {
        try {
            sleep(100l);
        } catch (InterruptedException ignored) {}
    }

    private static class CallClientBack implements NoParamsVoidReturn {
        
        private final Remote remote;

        public CallClientBack(@Inject Remote remote) {
            this.remote = remote;
        }

        @Override
        public void go() {
            try {
                remote.getProxyFactory().getProxy(ThrowableThrower.class).go();
            } catch (Throwable ignored) {
                ignored.printStackTrace();
            }
        }
    }

    private interface State {
        void set(String state);
        void get();
    }
    
    private static class ServerState implements State {

        private final Remote remote;
        private String state;

        public ServerState(@Inject Remote remote) {
            this.remote = remote;
        }

        @Override
        public void set(String state) {
            this.state = state;
        }

        @Override
        public void get() {
            remote.getProxyFactory().getProxy(State.class).set(state);
        }
    }

    private static class ClientState implements State {

        private String state;

        @Override
        public void set(String state) {
            this.state = state;
        }

        @Override
        public void get() { }
    }

    private static class ExceptionSavingListener implements RpcClient.ExceptionListener {
        private volatile Exception lastException;

        @Override
        public void onExceptionCaught(Exception e) {
            this.lastException = e;
        }

        public Exception lastException() {
            return lastException;
        }
    }
}
