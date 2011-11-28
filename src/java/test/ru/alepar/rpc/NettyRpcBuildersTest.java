package ru.alepar.rpc;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Test;
import ru.alepar.rpc.api.NettyRpcClientBuilder;
import ru.alepar.rpc.api.NettyRpcServerBuilder;
import ru.alepar.rpc.api.RpcClient;
import ru.alepar.rpc.api.RpcServer;

import static ru.alepar.rpc.Config.*;

public class NettyRpcBuildersTest {

    private final Mockery mockery = new JUnit4Mockery();

    @Test(timeout = TIMEOUT)
    public void buildersCreateClientAndServerWhichCanTalk() throws Exception {
        final ServerRemote mock = mockery.mock(ServerRemote.class);
        mockery.checking(new Expectations() {{
            one(mock).call();
        }});
        
        final NettyRpcServerBuilder serverBuilder = new NettyRpcServerBuilder(Config.BIND_ADDRESS);
        final NettyRpcClientBuilder clientBuilder = new NettyRpcClientBuilder(Config.BIND_ADDRESS);

        RpcServer server = serverBuilder
                .addObject(ServerRemote.class, mock)
                .enableKeepAlive(50l)
                .build();

        RpcClient client = clientBuilder
                .enableKeepAlive(50l)
                .build();

        try {
            ServerRemote proxy = client.getRemote().getProxy(ServerRemote.class);
            proxy.call();
            giveTimeForMessagesToBeProcessed();
        } finally {
            client.shutdown();
            server.shutdown();
        }
    }

    public interface ServerRemote {
        void call();
    }
}
