package kz.spt.app.service;

import java.io.IOException;
import java.util.Map;

public interface LanguagePropertiesService {
    Map<String, String> getWithDifferentLanguages(String key, Map<String, Object> values);
    Map<String, String> getWithDifferentLanguages(String key);
    String getMessageFromProperties(String key);
    String getMessageFromProperties(String key, String langCode);
    String putValuesToString(String text, Map<String, Object> values);

}
