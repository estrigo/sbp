package kz.spt.lib.service;

public interface PropertyService {

    String getValue(String key);

    void setValue(String key, String value);

    void disable(String key);
}
