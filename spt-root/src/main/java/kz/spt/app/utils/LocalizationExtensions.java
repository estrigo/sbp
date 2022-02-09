package kz.spt.app.utils;

import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;
import java.util.ResourceBundle;

public class LocalizationExtensions {
    public static String language() {
        return LocaleContextHolder.getLocale().toString().equals("ru") ? "ru" : "en";
    }

    public static ResourceBundle resourceBundle(String... name) {
        return ResourceBundle.getBundle(name.length > 0 ? name[0] : "messages", Locale.forLanguageTag(language()));
    }
}
