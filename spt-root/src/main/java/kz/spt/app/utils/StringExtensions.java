package kz.spt.app.utils;

import java.util.Map;
import java.util.ResourceBundle;

public class StringExtensions {

    public static String locale(String code, Map<String, String>... replace) {
        if (code.isEmpty()) return code;
        ResourceBundle bundle = LocalizationExtensions.resourceBundle();
        if (replace.length <= 0) return bundle.getString(code);

        String value = bundle.getString(code);
        replace[0].forEach((k, v) -> {
            value.replace(k, v);
        });
        return value;
    }
}
