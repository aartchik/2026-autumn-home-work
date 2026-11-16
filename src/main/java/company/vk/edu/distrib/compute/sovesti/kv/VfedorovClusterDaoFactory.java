package company.vk.edu.distrib.compute.sovesti.kv;

import java.io.IOException;
import java.net.http.HttpClient;
import java.util.stream.IntStream;

import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.kv.ClusterDaoFactoryTest;
import company.vk.edu.distrib.compute.kv.RemoteDaoFactory;
import company.vk.edu.distrib.compute.sovesti.urlshortener.dao.DecodingDao;

@ClusterDaoFactoryTest
public final class VfedorovClusterDaoFactory implements RemoteDaoFactory<String> {

    @Override
    public Dao<String> create(int... ports) throws IOException {
        return new DecodingDao(withShards(HttpClient.newHttpClient(), new ClusterDao(), ports));
    }

    private ClusterDao withShards(HttpClient client, ClusterDao cluster, int... ports) {
        IntStream.of(ports).forEach(port -> cluster.putShard(String.valueOf(port), new RemoteDao(port, client)));
        return cluster;
    }

}
