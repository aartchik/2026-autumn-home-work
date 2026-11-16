package company.vk.edu.distrib.compute.flighen.kv;

import company.vk.edu.distrib.compute.Dao;

@FunctionalInterface
public interface LinksDaoListener {
    void onLinksDaoChanged(Dao<String> dao);
}
