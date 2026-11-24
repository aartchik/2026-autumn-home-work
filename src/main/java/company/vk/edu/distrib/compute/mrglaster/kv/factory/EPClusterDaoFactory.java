package company.vk.edu.distrib.compute.mrglaster.kv.factory;

import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.kv.ClusterDaoFactoryTest;
import company.vk.edu.distrib.compute.kv.RemoteDaoFactory;
import company.vk.edu.distrib.compute.mrglaster.kv.dao.EPClusterDao;

import java.io.IOException;

@ClusterDaoFactoryTest
public class EPClusterDaoFactory implements RemoteDaoFactory<String> {
    @Override
    public Dao<String> create(int... ports) throws IOException {
        return new EPClusterDao(ports);
    }
}
