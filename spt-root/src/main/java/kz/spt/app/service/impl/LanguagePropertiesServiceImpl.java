package kz.spt.app.service.impl;

import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.loader.StringLoader;
import com.mitchellbosecke.pebble.template.PebbleTemplate;
import kz.spt.lib.service.Language;
import kz.spt.lib.service.LanguagePropertiesService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class LanguagePropertiesServiceImpl implements LanguagePropertiesService {

    @SneakyThrows
    public Map<String, String> getWithDifferentLanguages(String key, Map<String, Object> values){
        Locale locale = LocaleContextHolder.getLocale();
        Map<String, String> eventsText = new HashMap<>();

        Stream.of(Language.EN, Language.RU, Language.LOCAL)
                .forEach(lang -> {
                    String language = lang.equals(Language.LOCAL) ? locale.toString(): lang;
                    StringBuilder message = new StringBuilder(getMessageFromProperties(key, language));

                    if (values.containsKey("additionalMessage") && values.get("additionalMessage") != null){
                        StringBuilder additionalMessage = new StringBuilder(getMessageFromProperties(values.get("additionalMessage").toString(), language));
                        message.append(additionalMessage);
                    }

                    eventsText.put(lang, putValuesToString(message.toString(), values));
                });

        return eventsText;
    }

    @Override
    public Map<String, String> getWithDifferentLanguages(String key) {
        Locale locale = LocaleContextHolder.getLocale();
        Map<String, String> eventsText = new HashMap<>();
        Stream.of(Language.EN, Language.RU, Language.LOCAL)
                .forEach(lang -> {
                    String language = lang.equals(Language.LOCAL) ? locale.toString(): lang;
                    String message = getMessageFromProperties(key, language);
                    eventsText.put(lang, message);
                });

        return eventsText;
    }

    @Override
    public String getMessageFromProperties(String key, String langCode) {
        ResourceBundle bundle = ResourceBundle.getBundle("messages", Locale.forLanguageTag(langCode));
        String message = "";
        try {
            message = bundle.getString(key);
        } catch (Exception e) {
            ResourceBundle defaultBundle = ResourceBundle.getBundle("messages", Locale.forLanguageTag(Language.EN));
            log.info("key " + key + " for language " + langCode + " not found");
            try {
                message = defaultBundle.getString(key);
                log.info("Writing to local message in default language en");
            } catch (Exception ex) {
                log.info("key " + key + " not found in default message bundle en");
                message = null;
                log.info("Writing to local message null");
            }
        }
        return message;
    }

    @Override
    public String getMessageFromProperties(String key, String langCode, Map<String, Object> messageValues) {
        ResourceBundle bundle = ResourceBundle.getBundle("messages", Locale.forLanguageTag(langCode));
        String message = "";
        String messageWithValues = "";
        try {
            message = bundle.getString(key);
            messageWithValues = putValuesToString(message, messageValues);
        } catch (Exception e) {
            ResourceBundle defaultBundle = ResourceBundle.getBundle("messages", Locale.forLanguageTag(Language.EN));
            log.info("key " + key + " for language " + langCode + " not found");
            try {
                message = defaultBundle.getString(key);
                messageWithValues = putValuesToString(message, messageValues);
                log.info("Writing to local message in default language en");
            } catch (Exception ex) {
                log.info("key " + key + " not found in default message bundle en");
                messageWithValues = null;
                log.info("Writing to local message null");
            }
        }
        return messageWithValues;
    }

    public String getMessageFromProperties(String key) {
        Locale locale = LocaleContextHolder.getLocale();
        ResourceBundle bundle = ResourceBundle.getBundle("messages", Locale.forLanguageTag(locale.toString()));
        String message = "";
        try {
            message = bundle.getString(key);
        } catch (Exception e) {
            ResourceBundle defaultBundle = ResourceBundle.getBundle("messages", Locale.forLanguageTag(Language.EN));
            log.info("key " + key + " for language " + locale.toString() + " not found");
            try {
                message = defaultBundle.getString(key);
                log.info("Writing to local message in default language en");
            } catch (Exception ex) {
                ResourceBundle ruBundle = ResourceBundle.getBundle("messages", Locale.forLanguageTag(Language.RU));
                log.info("key " + key + " for language " + locale.toString() + " not found");
                try {
                    message = ruBundle.getString(key);
                    log.info("Writing to local message in default language ru");

                } catch (Exception exc) {
                    log.info("key " + key + " not found in default message bundle ru");
                    message = null;
                    log.info("Writing to local message null");
                }
            }
        }
        return message;
    }


    @SneakyThrows
    public String putValuesToString(String text, Map<String, Object> values){
        PebbleEngine pebbleEngine = new PebbleEngine.Builder().loader(new StringLoader()).build();
        PebbleTemplate compiledTemplate = pebbleEngine.getTemplate(text);

        Writer writer = new StringWriter();
        compiledTemplate.evaluate(writer, values);

        return writer.toString();
    }
}
