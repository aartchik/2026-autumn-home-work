package company.vk.edu.distrib.compute.tomovalex.kv;

import company.vk.edu.distrib.compute.AbstractHttpServiceFactory;
import company.vk.edu.distrib.compute.kv.KVServiceTest;

import java.io.IOException;
import java.nio.file.Path;

@KVServiceTest
public class KVServiceFactory extends AbstractHttpServiceFactory<KVService> {
    @Override
    protected KVService doCreate(int port) throws IOException {
        Path directory = Path.of(System.getProperty("java.io.tmpdir"), "tomovalex-kv", Integer.toString(port));
        boolean multithreaded = Boolean.parseBoolean(System.getenv("MULTITHREADED"));
        return new KVService(port, new ByteDao(directory), multithreaded);
    }
}
