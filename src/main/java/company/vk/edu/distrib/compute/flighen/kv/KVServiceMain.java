/*
package company.vk.edu.distrib.compute.flighen.kv;


public class KVServiceMain {
    private KVServiceMain() {
    }

    public static void main(String[] args) throws Exception {
        int port = 8081;

        FlighenKVServiceFactory factory = new FlighenKVServiceFactory();

        FlighenKVService service =
                factory.doCreate(port);

        service.start();

        Runtime.getRuntime().addShutdownHook(
                new Thread(service::stop)
        );

        Thread.currentThread().join();
    }
}
*/
