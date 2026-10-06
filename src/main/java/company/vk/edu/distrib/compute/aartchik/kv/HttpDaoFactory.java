package company.vk.edu.distrib.compute.aartchik.kv;

import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.kv.RemoteDaoFactory;
import company.vk.edu.distrib.compute.kv.RemoteDaoFactoryTest;

@RemoteDaoFactoryTest
public final class HttpDaoFactory implements RemoteDaoFactory<String> {
    @Override
    public Dao<String> create(int... ports) {
        if (ports.length != 1 || ports[0] <= 0 || ports[0] > 65535) {
            throw new IllegalArgumentException("Exactly one valid KV port is required");
        }
        return new HttpDao(ports[0]);
    }
}
