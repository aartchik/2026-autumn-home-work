package company.vk.edu.distrib.compute.nosorozhek.kv;

import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.kv.ClusterDaoFactoryTest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@ClusterDaoFactoryTest
public class ClusterDaoFactory implements company.vk.edu.distrib.compute.kv.RemoteDaoFactory<String> {
    @Override
    public Dao<String> create(int... ports) throws IOException {
        if (ports.length == 0) {
            throw new IllegalArgumentException("At least one port is required");
        }
        for (int port : ports) {
            if (port <= 0 || port > 65535) {
                throw new IllegalArgumentException("Ports must be between 1 and 65535");
            }
        }

        return new ClusterDao<>(
                "http://localhost",
                value -> value.getBytes(StandardCharsets.UTF_8),
                bytes -> new String(bytes, StandardCharsets.UTF_8),
                ports
        );
    }
}
