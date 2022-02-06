package kz.spt.lib.utils;

public class Utils {

    public static String changeCyrillicToLatin(String origin){
        return origin
                .replace("В","B")
                .replace("С","C")
                .replace("Т","T")
                .replace("Е","E")
                .replace("О","O")
                .replace("Р","P")
                .replace("Х","X")
                .replace("А","A")
                .replace("Н","H")
                .replace("М","M")
                .replace("У","Y")
                .replace("К","K")
                .replace(" ","");
    }
}
