package company.vk.edu.distrib.compute.vagifbaratov.urlshortener;

import company.vk.edu.distrib.compute.AbstractHttpServiceFactory;
import company.vk.edu.distrib.compute.urlshortener.UrlShortenerAuthTest;
import company.vk.edu.distrib.compute.urlshortener.UrlShortenerTest;
import company.vk.edu.distrib.compute.vagifbaratov.urlshortener.dao.PersistentCredentialsDao;
import company.vk.edu.distrib.compute.vagifbaratov.urlshortener.dao.PersistentLinksDao;

import java.io.IOException;
import java.nio.file.Path;

@UrlShortenerTest
@UrlShortenerAuthTest
public class UrlShortenerServiceFactory extends AbstractHttpServiceFactory<UrlShortenerServiceImpl> {
    private static final Path DATA_DIRECTORY = Path.of(
            System.getProperty("java.io.tmpdir"), "vbaratov-url-shortener"
    );

    @Override
    protected UrlShortenerServiceImpl doCreate(int port) throws IOException {

        return new UrlShortenerServiceImpl(
                port,
                new PersistentLinksDao(
                        DATA_DIRECTORY.resolve("links-storage.properties")
                ),
                new PersistentCredentialsDao(
                        DATA_DIRECTORY.resolve("credentials-storage.properties")
                )
        );
    }
}
