package ru.alepar.rpc.netty;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.alepar.rpc.Inject;
import ru.alepar.rpc.ProxyFactory;
import ru.alepar.rpc.RpcClient;
import ru.alepar.rpc.RpcServer;
import ru.alepar.rpc.exception.*;

import java.io.Serializable;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.alepar.rpc.netty.Config.*;

@RunWith(JMock.class)
public class NettyRpcServerTest {

    private final Mockery mockery = new JUnit4Mockery();

    @Test(timeout = TIMEOUT)
    public void invokesMethodsWithNoParamsAndVoidReturnType() throws Exception {
        final NoParamsVoidReturn impl = mockery.mock(NoParamsVoidReturn.class);

        mockery.checking(new Expectations() {{
            one(impl).go();
        }});

        RpcServer server = new NettyRpcServer(BIND_ADDRESS);
        server.addImplementation(NoParamsVoidReturn.class, impl);

        RpcClient client = new NettyRpcClient(BIND_ADDRESS);
        final NoParamsVoidReturn proxy = client.getImplementation(NoParamsVoidReturn.class);

        try {
            proxy.go();
        } finally {
            client.shutdown();
            server.shutdown();
        }
    }

    @Test(timeout = TIMEOUT)
    public void returnedObjectIsPassedToClientSide() throws Exception {
        final NoParamsIntegerReturn impl = mockery.mock(NoParamsIntegerReturn.class);
        final Integer returnValue = 5;

        mockery.checking(new Expectations() {{
            one(impl).go();
            will(returnValue(returnValue));
        }});

        RpcServer server = new NettyRpcServer(BIND_ADDRESS);
        server.addImplementation(NoParamsIntegerReturn.class, impl);

        RpcClient client = new NettyRpcClient(BIND_ADDRESS);
        final NoParamsIntegerReturn proxy = client.getImplementation(NoParamsIntegerReturn.class);

        try {
            assertThat(proxy.go(), equalTo(returnValue));
        } finally {
            client.shutdown();
            server.shutdown();
        }
    }

    @Test(timeout = TIMEOUT)
    public void subsequentCallsWork() throws Exception {
        final NoParamsIntegerReturn impl = mockery.mock(NoParamsIntegerReturn.class);
        final Integer returnValueOne = 5;
        final Integer returnValueTwo = 10;

        mockery.checking(new Expectations() {{
            one(impl).go();
            will(returnValue(returnValueOne));
            one(impl).go();
            will(returnValue(returnValueTwo));
        }});

        RpcServer server = new NettyRpcServer(BIND_ADDRESS);
        server.addImplementation(NoParamsIntegerReturn.class, impl);

        RpcClient client = new NettyRpcClient(BIND_ADDRESS);
        final NoParamsIntegerReturn proxy = client.getImplementation(NoParamsIntegerReturn.class);

        try {
            assertThat(proxy.go(), equalTo(returnValueOne));
            assertThat(proxy.go(), equalTo(returnValueTwo));
        } finally {
            client.shutdown();
            server.shutdown();
        }
    }

    @Test(timeout = TIMEOUT)
    public void methodsWithPrimitiveReturnTypeWork() throws Exception {
        final NoParamsPrimitiveReturn impl = mockery.mock(NoParamsPrimitiveReturn.class);
        final long returnValue = 5;

        mockery.checking(new Expectations() {{
            one(impl).go();
            will(returnValue(returnValue));
        }});

        RpcServer server = new NettyRpcServer(BIND_ADDRESS);
        server.addImplementation(NoParamsPrimitiveReturn.class, impl);

        RpcClient client = new NettyRpcClient(BIND_ADDRESS);
        final NoParamsPrimitiveReturn proxy = client.getImplementation(NoParamsPrimitiveReturn.class);

        try {
            assertThat(proxy.go(), equalTo(returnValue));
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

        RpcServer server = new NettyRpcServer(BIND_ADDRESS);
        server.addImplementation(IntegerParam.class, impl);

        RpcClient client = new NettyRpcClient(BIND_ADDRESS);
        final IntegerParam proxy = client.getImplementation(IntegerParam.class);

        try {
            proxy.go(param);
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

        RpcServer server = new NettyRpcServer(BIND_ADDRESS);
        server.addImplementation(IntLongParam.class, impl);

        RpcClient client = new NettyRpcClient(BIND_ADDRESS);
        final IntLongParam proxy = client.getImplementation(IntLongParam.class);

        try {
            proxy.go(paramInt, paramLong);
        } finally {
            client.shutdown();
            server.shutdown();
        }
    }

    @Test(expected = IllegalStateException.class, timeout = TIMEOUT)
    public void passesServersideExceptions() throws Exception {
        final ExceptionThrower impl = new ExceptionThrower() {
            @Override
            public void go() throws IllegalStateException {
                throw new IllegalStateException("expected exception");
            }
        };

        RpcServer server = new NettyRpcServer(BIND_ADDRESS);
        server.addImplementation(ExceptionThrower.class, impl);

        RpcClient client = new NettyRpcClient(BIND_ADDRESS);
        final ExceptionThrower proxy = client.getImplementation(ExceptionThrower.class);

        try {
            proxy.go();
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

        RpcServer server = new NettyRpcServer(BIND_ADDRESS);
        server.addImplementation(OverloadedString.class, impl);

        RpcClient client = new NettyRpcClient(BIND_ADDRESS);
        final OverloadedString proxy = client.getImplementation(OverloadedString.class);

        try {
            proxy.go((Serializable) s);
        } finally {
            client.shutdown();
            server.shutdown();
        }
    }

    @Test(timeout = TIMEOUT, expected = ProtocolException.class)
    public void throwsProtocolExceptionIfCannotSerializeParams() throws Exception {
        final NonSerializable impl = mockery.mock(NonSerializable.class);

        RpcServer server = new NettyRpcServer(BIND_ADDRESS);
        server.addImplementation(NonSerializable.class, impl);

        RpcClient client = new NettyRpcClient(BIND_ADDRESS);
        final NonSerializable proxy = client.getImplementation(NonSerializable.class);

        try {
            proxy.param("", new Object());
        } finally {
            client.shutdown();
            server.shutdown();
        }
    }

    @Test(timeout = TIMEOUT, expected = ProtocolException.class)
    public void throwsProtocolExceptionIfCannotSerializeReturnValue() throws Exception {
        final NonSerializable impl = mockery.mock(NonSerializable.class);

        mockery.checking(new Expectations() {{
            allowing(impl).ret();
            will(returnValue(new Object()));
        }});

        RpcServer server = new NettyRpcServer(BIND_ADDRESS);
        server.addImplementation(NonSerializable.class, impl);

        RpcClient client = new NettyRpcClient(BIND_ADDRESS);
        final NonSerializable proxy = client.getImplementation(NonSerializable.class);

        try {
            proxy.ret();
        } finally {
            client.shutdown();
            server.shutdown();
        }
    }

    @Test(timeout = TIMEOUT, expected = TransportException.class) @Ignore("we don't handle this yet - but we're planning to, yeah")
    public void throwsTransportExceptionIfConnectionIsAbruptlyTerminated() throws Throwable {
        final InfinteWaiter impl = new InfinteWaiter() {
            @SuppressWarnings({"InfiniteLoopStatement"})
            @Override
            public void hang() {
                while(true) {
                    try {
                        Thread.sleep(1000L);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        };

        RpcServer server = new NettyRpcServer(BIND_ADDRESS);
        server.addImplementation(InfinteWaiter.class, impl);

        RpcClient client = new NettyRpcClient(BIND_ADDRESS);
        final InfinteWaiter proxy = client.getImplementation(InfinteWaiter.class);

        try {
            TestSupport.ErrorCatchingRunnable target = new TestSupport.ErrorCatchingRunnable(new Runnable() {
                @Override
                public void run() {
                    proxy.hang();
                }
            });
            Thread thread = new Thread(target);
            thread.start();
            server.shutdown();
            thread.join();
            if (target.getError() != null) {
                throw target.getError();
            }
        } finally {
            client.shutdown();
        }
    }

    @Test(timeout = TIMEOUT, expected = IllegalAccessException.class)
    public void javaLangExceptionsAreNotWrappedInRemoteException() throws Throwable {
        final ThrowableThrower impl = new ThrowableThrower() {
            @Override
            public void go() throws Throwable {
                throw new IllegalAccessException("some java.lang exception");
            }
        };

        RpcServer server = new NettyRpcServer(BIND_ADDRESS);
        server.addImplementation(ThrowableThrower.class, impl);

        RpcClient client = new NettyRpcClient(BIND_ADDRESS);
        final ThrowableThrower proxy = client.getImplementation(ThrowableThrower.class);

        try {
            proxy.go();
        } finally {
            client.shutdown();
            server.shutdown();
        }
    }

    @Test(timeout = TIMEOUT, expected = SafeRuntimeException.class)
    public void safeRuntimeExceptionsAreNotWrappedInRemoteException() throws Throwable {
        final ThrowableThrower impl = new ThrowableThrower() {
            @Override
            public void go() throws Throwable {
                throw new SafeRuntimeException();
            }
        };

        RpcServer server = new NettyRpcServer(BIND_ADDRESS);
        server.addImplementation(ThrowableThrower.class, impl);

        RpcClient client = new NettyRpcClient(BIND_ADDRESS);
        final ThrowableThrower proxy = client.getImplementation(ThrowableThrower.class);

        try {
            proxy.go();
        } finally {
            client.shutdown();
            server.shutdown();
        }
    }

    @Test(timeout = TIMEOUT, expected = SafeCheckedException.class)
    public void safeCheckedExceptionsAreNotWrappedInRemoteException() throws Throwable {
        final ThrowableThrower impl = new ThrowableThrower() {
            @Override
            public void go() throws Throwable {
                throw new SafeCheckedException();
            }
        };

        RpcServer server = new NettyRpcServer(BIND_ADDRESS);
        server.addImplementation(ThrowableThrower.class, impl);

        RpcClient client = new NettyRpcClient(BIND_ADDRESS);
        final ThrowableThrower proxy = client.getImplementation(ThrowableThrower.class);

        try {
            proxy.go();
        } finally {
            client.shutdown();
            server.shutdown();
        }
    }

    @Test(timeout = TIMEOUT, expected = RemoteException.class)
    public void arbitraryExceptionsAreWrappedInRemoteException() throws Throwable {
        final ThrowableThrower impl = new ThrowableThrower() {
            @Override
            public void go() throws Throwable {
                throw new MyException();
            }
        };

        RpcServer server = new NettyRpcServer(BIND_ADDRESS);
        server.addImplementation(ThrowableThrower.class, impl);

        RpcClient client = new NettyRpcClient(BIND_ADDRESS);
        final ThrowableThrower proxy = client.getImplementation(ThrowableThrower.class);

        try {
            proxy.go();
        } finally {
            client.shutdown();
            server.shutdown();
        }
    }

    @Test(timeout = TIMEOUT)
    public void feedbackToClientWorks() throws Exception {
        final String MSG = "echoed-hi";

        RpcServer server = new NettyRpcServer(BIND_ADDRESS);
        server.addClass(SomeServerApi.class, SomeServerImpl.class);

        final ClientApi mockClient = mockery.mock(ClientApi.class);
        mockery.checking(new Expectations() {{
            one(mockClient).feedback(MSG);
        }});

        RpcClient client = new NettyRpcClient(BIND_ADDRESS);
        client.addImplementation(ClientApi.class, mockClient);
        SomeServerApi proxy = client.getImplementation(SomeServerApi.class);

        try {
            proxy.go(MSG);
        } finally {
            client.shutdown();
            server.shutdown();
        }
    }

    @Test(timeout = TIMEOUT)
    public void serverImplementationsAreCachedIeStatePersistsAcrossClientCalls() throws Exception {
        final String MSG = "some stateful data stored on server-side";

        RpcServer server = new NettyRpcServer(BIND_ADDRESS);
        server.addClass(StatefulServerApi.class, StatefulServerImpl.class);

        RpcClient client = new NettyRpcClient(BIND_ADDRESS);
        StatefulServerApi proxy = client.getImplementation(StatefulServerApi.class);

        try {
            proxy.save(MSG);
            String response = proxy.load();
            assertThat(response, equalTo(MSG));
        } finally {
            client.shutdown();
            server.shutdown();
        }
    }

    @Test(timeout = TIMEOUT)
    public void differentClientsHaveSeparateStateOnServerSide() throws Exception {
        final String MSG = "some stateful data stored on server-side";

        RpcServer server = new NettyRpcServer(BIND_ADDRESS);
        server.addClass(StatefulServerApi.class, StatefulServerImpl.class);

        RpcClient clientOne = new NettyRpcClient(BIND_ADDRESS);
        StatefulServerApi proxyOne = clientOne.getImplementation(StatefulServerApi.class);
        RpcClient clientTwo = new NettyRpcClient(BIND_ADDRESS);
        StatefulServerApi proxyTwo = clientTwo.getImplementation(StatefulServerApi.class);

        try {
            proxyOne.save(MSG);
            String response = proxyTwo.load();
            assertThat(response, equalTo(new StatefulServerImpl().load()));
        } finally {
            clientOne.shutdown();
            clientTwo.shutdown();
            server.shutdown();
        }
    }

    private interface NoParamsVoidReturn {
        void go();
    }
    private interface NoParamsIntegerReturn {
        Integer go();
    }
    private interface NoParamsPrimitiveReturn {
        long go();
    }
    private interface IntegerParam {
        void go(Integer i);
    }
    private interface IntLongParam {
        void go(int i, long l);
    }
    private interface ExceptionThrower {
        void go() throws IllegalStateException;
    }
    private interface OverloadedString {
        void go(String s); // though unused, is vital for correctnes of corresponding unit test
        void go(Serializable s);
    }
    private interface NonSerializable {
        void param(String s, Object o);
        Object ret();
    }
    private interface InfinteWaiter {
        void hang();
    }
    private interface ThrowableThrower {
        void go() throws Throwable;
    }
    private static class MyException extends Exception { }

    private interface SomeServerApi {
        void go(String msg);
    }

    public static class SomeServerImpl implements SomeServerApi {

        private final ProxyFactory feedback;

        public SomeServerImpl(@Inject ProxyFactory feedback) {
            this.feedback = feedback;
        }

        @Override
        public void go(String msg) {
            ClientApi clientProxy = feedback.getImplementation(ClientApi.class);
            clientProxy.feedback(msg);
        }
    }

    private interface ClientApi {
        void feedback(String msg);
    }

    private interface StatefulServerApi {
        void save(String msg);
        String load();
    }

    private static class StatefulServerImpl implements StatefulServerApi {
        private String state = "value default for new instance - this is not what i expect";

        public StatefulServerImpl() {}

        @Override
        public void save(String msg) {
            this.state = msg;
        }

        @Override
        public String load() {
            return state;
        }
    }
}
