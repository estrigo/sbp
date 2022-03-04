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

    public static String convertRegion(String regionCode){
        switch (regionCode){
            case "ae-du" : return "Dubai";
            case "ae-az" : return "Abu Dhabi";
            case "ae-aj" : return "Ajman";
            case "ae-fu" : return "Fujairah";
            case "ae-rk" : return "Ras Al Khaimah";
            case "ae-sh" : return "Sharjah";
            case "ae-uq" : return "Umm Al Quwain";
            case "qa" : return "Qatar";
            case "sa" : return "Saudi Arabia";
            default: return regionCode;
        }
    }
}
