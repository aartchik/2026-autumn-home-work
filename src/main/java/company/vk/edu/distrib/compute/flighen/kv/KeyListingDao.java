package company.vk.edu.distrib.compute.flighen.kv;

import company.vk.edu.distrib.compute.Dao;

import java.util.List;

public interface KeyListingDao<T> extends Dao<T> {
    List<String> getKeys();
}
