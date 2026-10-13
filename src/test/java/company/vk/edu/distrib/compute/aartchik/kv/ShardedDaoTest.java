package company.vk.edu.distrib.compute.aartchik.kv;

import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.kv.KVService;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.NoSuchElementException;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShardedDaoTest {
    @Test
    void configurationMustBeNonemptyValidAndDistinct() {
        ClusterDaoFactory factory = new ClusterDaoFactory();
        assertThrows(IllegalArgumentException.class, factory::create);
        assertThrows(IllegalArgumentException.class, () -> factory.create(0));
        assertThrows(IllegalArgumentException.class, () -> factory.create(65536));
        assertThrows(IllegalArgumentException.class, () -> factory.create(1234, 1234));
    }

    @Test
    void clusterCrudHasOneOwnerAndIgnoresConfigurationOrder() throws Exception {
        int firstPort = freePort();
        int secondPort = freePort();
        KVService first = new KvServiceFactory().create(firstPort);
        KVService second = new KvServiceFactory().create(secondPort);
        first.start();
        try {
            second.start();
            try (Dao<String> cluster = new ClusterDaoFactory().create(firstPort, secondPort);
                    Dao<String> reversed = new ClusterDaoFactory().create(secondPort, firstPort);
                    Dao<String> firstNode = new HttpDaoFactory().create(firstPort);
                    Dao<String> secondNode = new HttpDaoFactory().create(secondPort)) {
                int firstCount = 0;
                int secondCount = 0;
                for (int index = 0; index < 100; index++) {
                    String key = "shard-" + index + " /?&=é";
                    cluster.upsert(key, "value-" + index);
                    assertEquals("value-" + index, reversed.get(key));
                    boolean onFirst = contains(firstNode, key);
                    boolean onSecond = contains(secondNode, key);
                    assertTrue(onFirst ^ onSecond, "Each key must have exactly one owner");
                    firstCount += onFirst ? 1 : 0;
                    secondCount += onSecond ? 1 : 0;
                    reversed.upsert(key, "updated");
                    assertEquals("updated", cluster.get(key));
                    assertEquals(onFirst, contains(firstNode, key));
                    assertEquals(onSecond, contains(secondNode, key));
                    reversed.delete(key);
                    assertThrows(NoSuchElementException.class, () -> cluster.get(key));
                    assertTrue(!contains(firstNode, key) && !contains(secondNode, key));
                }
                assertTrue(firstCount > 25 && secondCount > 25, "Both nodes must serve a substantial share of keys");
            } finally {
                second.stop();
            }
        } finally {
            first.stop();
        }
    }

    @Test
    void concurrentRequestsAndClosedDao() throws Exception {
        int port = freePort();
        KVService service = new KvServiceFactory().create(port);
        service.start();
        HttpDao dao = new HttpDao(port);
        try (dao; var workers = Executors.newVirtualThreadPerTaskExecutor()) {
            var tasks = new java.util.ArrayList<java.util.concurrent.Future<String>>();
            for (int index = 0; index < 64; index++) {
                String key = "parallel-" + index;
                tasks.add(workers.submit(() -> {
                    dao.upsert(key, key);
                    return dao.get(key);
                }));
            }
            for (int index = 0; index < tasks.size(); index++) {
                assertEquals("parallel-" + index, tasks.get(index).get());
            }
        } finally {
            service.stop();
        }
        assertThrows(IOException.class, () -> dao.get("closed"));
        assertThrows(IOException.class, () -> dao.upsert("closed", "value"));
        assertThrows(IOException.class, () -> dao.delete("closed"));
        dao.close();
    }

    private static boolean contains(Dao<String> dao, String key) throws IOException {
        try {
            dao.get(key);
            return true;
        } catch (NoSuchElementException missing) {
            return false;
        }
    }

    private static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
