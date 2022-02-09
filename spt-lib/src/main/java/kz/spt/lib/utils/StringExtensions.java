package kz.spt.lib.utils;

import java.util.Map;

public class StringExtensions {

    public static String locale(String code, Map<String,String> replace){
        if(code.isEmpty()) return code;
        if(replace.isEmpty()) return LocalizationExtensions.resourceBundle().getString(code);

        String bundle = LocalizationExtensions.resourceBundle().getString(code);
        replace.forEach((k,v)->{
            bundle.replace(k,v);
        });
        return bundle;
    }
}
