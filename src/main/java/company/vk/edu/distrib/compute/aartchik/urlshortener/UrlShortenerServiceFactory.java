package company.vk.edu.distrib.compute.aartchik.urlshortener;

import company.vk.edu.distrib.compute.AbstractHttpServiceFactory;
import company.vk.edu.distrib.compute.urlshortener.UrlShortenerService;
import company.vk.edu.distrib.compute.urlshortener.UrlShortenerTest;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@UrlShortenerTest
public final class UrlShortenerServiceFactory
        extends AbstractHttpServiceFactory<UrlShortenerService> {
    @Nullable private Path dataDirectory;

    @Override
    protected synchronized UrlShortenerService doCreate(int port) throws IOException {
        Path directory = dataDirectory;
        if (directory == null) {
            directory = Files.createTempDirectory("aartchik-url-shortener-");
            dataDirectory = directory;
        }
        return new UrlShortenerServiceImpl(port, directory);
    }
}
