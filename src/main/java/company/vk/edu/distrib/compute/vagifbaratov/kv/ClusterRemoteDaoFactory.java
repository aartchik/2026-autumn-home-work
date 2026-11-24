package company.vk.edu.distrib.compute.vagifbaratov.kv;

import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.kv.ClusterDaoFactoryTest;
import company.vk.edu.distrib.compute.kv.RemoteDaoFactory;

import java.io.IOException;
import java.util.Arrays;

@ClusterDaoFactoryTest
public class ClusterRemoteDaoFactory implements RemoteDaoFactory<String> {

    @Override
    public Dao<String> create(int... ports) throws IOException {
        return new ClusterRemoteDao("http://localhost", validatePorts(ports));
    }

    private int[] validatePorts(int... ports) {
        if (ports == null || ports.length == 0) {
            throw new IllegalArgumentException("At least 1 port required");
        }
        int[] sorted = ports.clone();
        Arrays.sort(sorted);

        for (int i = 0; i < sorted.length; i++) {
            if (sorted[i] <= 0 || sorted[i] >= 65536) {
                throw new IllegalArgumentException("Port is invalid: " + sorted[i]);
            }

            if (i > 0 && sorted[i] == sorted[i - 1]) {
                throw new IllegalArgumentException("Duplicate port: " + sorted[i]);
            }
        }

        return sorted;
    }
}
