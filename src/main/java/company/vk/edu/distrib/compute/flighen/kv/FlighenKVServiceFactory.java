package company.vk.edu.distrib.compute.flighen.kv;

import company.vk.edu.distrib.compute.AbstractHttpServiceFactory;
import company.vk.edu.distrib.compute.kv.KVServiceTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@KVServiceTest
public class FlighenKVServiceFactory extends AbstractHttpServiceFactory<FlighenKVService> {
    private static final Logger log = LoggerFactory.getLogger(FlighenKVServiceFactory.class);

    private static final int THREADSNUM = 8;

    @Override
    protected FlighenKVService doCreate(int port) throws IOException {
        boolean multithreaded = Boolean.parseBoolean(System.getenv("KV_MULTITHREADED"));

        int threads = 1;

        if (multithreaded) {
            threads = THREADSNUM;
            log.info("Starting KV Service in multithreading mode");
        }

        return new FlighenKVService(port, threads);
    }
}
