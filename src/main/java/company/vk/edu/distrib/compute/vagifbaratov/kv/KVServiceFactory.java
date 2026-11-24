package company.vk.edu.distrib.compute.vagifbaratov.kv;

import company.vk.edu.distrib.compute.AbstractHttpServiceFactory;
import company.vk.edu.distrib.compute.kv.KVService;
import company.vk.edu.distrib.compute.kv.KVServiceTest;

import java.io.IOException;
import java.nio.file.Path;

@KVServiceTest
public class KVServiceFactory extends AbstractHttpServiceFactory<KVService> {
    private static final Path DATA_DIRECTORY = Path.of(
            System.getProperty("java.io.tmpdir"), "vbaratov-kv"
    );

    @Override
    protected KVService doCreate(int port) throws IOException {
        PersistentByteArrayDao dao = new PersistentByteArrayDao(
                DATA_DIRECTORY.resolve(Integer.toString(port))
        );
        try {
            return new KVServiceImpl(port, dao);
        } catch (IOException | RuntimeException exception) {
            dao.close();
            throw exception;
        }
    }
}
