package kz.spt.lib.utils;

public class Utils {

    public static String changeCyrillicToLatin(String origin) {
        return origin
                .replace("В", "B")
                .replace("С", "C")
                .replace("Т", "T")
                .replace("Е", "E")
                .replace("О", "O")
                .replace("Р", "P")
                .replace("Х", "X")
                .replace("А", "A")
                .replace("Н", "H")
                .replace("М", "M")
                .replace("У", "Y")
                .replace("К", "K")
                .replace(" ", "");
    }

    public static String convertRegion(String regionCode) {
        switch (regionCode) {
            case "ae-du":
            case "dubai":
                return "Dubai";
            case "ae-az":
            case "abu-dhabi":
                return "Abu Dhabi";
            case "ae-aj":
            case "ajman":
                return "Ajman";
            case "ae-fu":
            case "fujairah":
                return "Fujairah";
            case "ae-rk":
            case "ras-al-khaimah":
                return "Ras Al Khaimah";
            case "ae-sh":
            case "sharjah":
                return "Sharjah";
            case "ae-uq":
            case "alquwain":
                return "Umm Al Quwain";
            case "qa":
                return "Qatar";
            case "sa":
                return "Saudi Arabia";
            default:
                return regionCode;
        }
    }
}
