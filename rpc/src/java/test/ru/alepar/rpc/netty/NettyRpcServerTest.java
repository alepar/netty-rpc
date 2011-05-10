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
import static org.junit.Assert.assertThat;
import static ru.alepar.rpc.netty.Config.BIND_ADDRESS;

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
        NoParamsVoidReturn proxy = client.getImplementation(NoParamsVoidReturn.class);

        try {
            proxy.go();
        } finally {
            client.shutdown();
            server.shutdown();
        }
    }

    @Test
    public void returnedObjectIsPassedToClientSide() throws Exception {
        final NoParamsIntReturn impl = mockery.mock(NoParamsIntReturn.class);
        final Integer returnValue = 5;

        mockery.checking(new Expectations() {{
            one(impl).go();
            will(returnValue(returnValue));
        }});

        RpcServer server = new NettyRpcServer(BIND_ADDRESS);
        server.addImplementation(NoParamsIntReturn.class, impl);

        RpcClient client = new NettyRpcClient(BIND_ADDRESS);
        NoParamsIntReturn proxy = client.getImplementation(NoParamsIntReturn.class);

        try {
            assertThat(proxy.go(), equalTo(returnValue));
        } finally {
            client.shutdown();
            server.shutdown();
        }
    }

    private interface NoParamsVoidReturn {
        void go();
    }
    private interface NoParamsIntReturn {
        Integer go();
    }
}
