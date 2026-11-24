package company.vk.edu.distrib.compute.robert.urlshortener;

import company.vk.edu.distrib.compute.AbstractHttpServiceFactory;
import company.vk.edu.distrib.compute.urlshortener.UrlShortenerAuthTest;
import company.vk.edu.distrib.compute.urlshortener.UrlShortenerTest;

import java.io.IOException;

@UrlShortenerTest
@UrlShortenerAuthTest
public class RobertHttpServiceFactory extends AbstractHttpServiceFactory<RobertUrlShortenerService> {

    @Override
    protected RobertUrlShortenerService doCreate(int port) throws IOException {
        return new RobertUrlShortenerService(port);
    }

}
