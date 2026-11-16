package company.vk.edu.distrib.compute.miiishenka.kv;

import java.io.IOException;

import company.vk.edu.distrib.compute.AbstractHttpServiceFactory;
import company.vk.edu.distrib.compute.kv.KVServiceTest;

@KVServiceTest
public class MiiishenkaKVServiceFactory extends AbstractHttpServiceFactory<MiiishenkaKVService> {
    @Override
    protected MiiishenkaKVService doCreate(int port) throws IOException {
        return new MiiishenkaKVService(port);
    }
}
