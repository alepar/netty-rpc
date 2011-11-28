package ru.alepar.rpc.server;

import java.util.Collection;

import org.junit.Test;
import ru.alepar.rpc.common.NettyId;
import ru.alepar.rpc.common.NettyRemote;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class ClientRepositoryTest {

    @Test
    public void collectionRetrunedByGetClientsIsUpdatedWithConnectingDisconnectingClients() throws Exception {
        ClientRepository repo = new ClientRepository();
        final Collection<NettyRemote> clients = repo.getClients();
        assertThat(clients.isEmpty(), equalTo(true));

        final NettyId clientId = new NettyId(0xcafebabe);
        repo.addClient(new NettyRemote(null, clientId, null));
        assertThat(clients.size(), equalTo(1));

        repo.removeClient(clientId);
        assertThat(clients.isEmpty(), equalTo(true));
    }
}
