package company.vk.edu.distrib.compute.sovesti.kv;

import java.io.IOException;
import java.util.Objects;

import company.vk.edu.distrib.compute.AbstractHttpServiceFactory;
import company.vk.edu.distrib.compute.kv.KVServiceTest;
import company.vk.edu.distrib.compute.sovesti.urlshortener.dao.DaoFactory;
import company.vk.edu.distrib.compute.sovesti.urlshortener.dao.InFileDaoFactory;
import company.vk.edu.distrib.compute.sovesti.urlshortener.handler.HttpServers;

@KVServiceTest
public final class VfedorovKVServiceFactory extends AbstractHttpServiceFactory<VfedorovKVService> {

    private final DaoFactory<byte[]> factory;

    public VfedorovKVServiceFactory(DaoFactory<byte[]> factory) {
        super();
        this.factory = Objects.requireNonNull(factory);
    }

    public VfedorovKVServiceFactory() {
        this(new InFileDaoFactory());
    }

    @Override
    protected VfedorovKVService doCreate(int port) throws IOException {
        return new VfedorovKVService(port, new HttpServers().create(port), factory);
    }

}
