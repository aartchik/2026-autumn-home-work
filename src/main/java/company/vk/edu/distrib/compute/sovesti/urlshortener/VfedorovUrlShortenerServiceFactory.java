package company.vk.edu.distrib.compute.sovesti.urlshortener;

import java.io.IOException;

import company.vk.edu.distrib.compute.AbstractHttpServiceFactory;
import company.vk.edu.distrib.compute.sovesti.urlshortener.handler.HttpServers;
import company.vk.edu.distrib.compute.urlshortener.UrlShortenerAuthTest;
import company.vk.edu.distrib.compute.urlshortener.UrlShortenerTest;

@UrlShortenerTest
@UrlShortenerAuthTest
public final class VfedorovUrlShortenerServiceFactory extends AbstractHttpServiceFactory<VfedorovUrlShortenerService> {

    @Override
    protected VfedorovUrlShortenerService doCreate(int port) throws IOException {
        return new VfedorovUrlShortenerService(new HttpServers().create(port));
    }

}
