package kz.spt.lib.utils;

import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;
import java.util.ResourceBundle;

public class LocalizationExtensions {
    public static String language(){
        return LocaleContextHolder.getLocale().toString().equals("ru") ? "ru-RU":"en";
    }

    public static ResourceBundle resourceBundle(){
        return ResourceBundle.getBundle("tgbot-plugin", Locale.forLanguageTag(language()));
    }
}
