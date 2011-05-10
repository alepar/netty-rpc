package ru.alepar.rpc.netty;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.alepar.rpc.RpcClient;
import ru.alepar.rpc.RpcServer;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.*;
import static ru.alepar.rpc.netty.Config.BIND_ADDRESS;
import static ru.alepar.rpc.netty.TestSupport.executeWithTimeout;

@RunWith(JMock.class)
public class NettyRpcServerTest {

    private final Mockery mockery = new JUnit4Mockery();

    @Test
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
            executeWithTimeout(new Runnable() {
                @Override
                public void run() {
                    proxy.go();
                }
            });
        } finally {
            client.shutdown();
            server.shutdown();
        }
    }

    @Test
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
            executeWithTimeout(new Runnable() {
                @Override
                public void run() {
                    assertThat(proxy.go(), equalTo(returnValue));
                }
            });
        } finally {
            client.shutdown();
            server.shutdown();
        }
    }

    @Test
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
            executeWithTimeout(new Runnable() {
                @Override
                public void run() {
                    assertThat(proxy.go(), equalTo(returnValueOne));
                    assertThat(proxy.go(), equalTo(returnValueTwo));
                }
            });
        } finally {
            client.shutdown();
            server.shutdown();
        }
    }

    @Test
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
            executeWithTimeout(new Runnable() {
                @Override
                public void run() {
                    assertThat(proxy.go(), equalTo(returnValue));
                }
            });
        } finally {
            client.shutdown();
            server.shutdown();
        }
    }

    @Test
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
            executeWithTimeout(new Runnable() {
                @Override
                public void run() {
                    proxy.go(param);
                }
            });
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
    private interface NoParamsPrimitiveReturn {
        long go();
    }
    private interface IntegerParam {
        void go(Integer i);
    }
}
