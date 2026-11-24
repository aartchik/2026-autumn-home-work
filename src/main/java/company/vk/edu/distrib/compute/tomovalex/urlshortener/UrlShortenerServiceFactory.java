package company.vk.edu.distrib.compute.tomovalex.urlshortener;

import company.vk.edu.distrib.compute.AbstractHttpServiceFactory;
import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.tomovalex.kv.ClusterRemoteDaoFactory;
import company.vk.edu.distrib.compute.urlshortener.UrlShortenerAuthTest;
import company.vk.edu.distrib.compute.urlshortener.UrlShortenerTest;

import java.io.IOException;

@UrlShortenerAuthTest
@UrlShortenerTest
public class UrlShortenerServiceFactory extends AbstractHttpServiceFactory<UrlShortenerService> {
    @Override
    protected UrlShortenerService doCreate(int port) throws IOException {
        return new UrlShortenerService(port, createLinksDao(), new UsersDao());
    }

    private static Dao<String> createLinksDao() {
        String config = System.getenv("KV_PORTS");
        if (config == null || config.isBlank()) {
            return new LinksDao();
        }
        String[] values = config.split(",", -1);
        int[] ports = new int[values.length];
        for (int i = 0; i < values.length; i++) {
            ports[i] = Integer.parseInt(values[i].trim());
        }
        return new ClusterRemoteDaoFactory().create(ports);
    }
}
