package kz.spt.lib.utils;

import com.fasterxml.jackson.databind.ObjectMapper;

public class StaticValues {

    public enum CarOutBy {
        WHITELIST,
        PREPAID,
        PAYMENT_PROVIDER,
        BOOKING,
        ZERO_TOUCH,
        MANUAL, // ручной выезд авто
        ABONOMENT, // выезд через абономент
        REGISTER, // только регистрировать выезды
        FREE, //  Бесплатный заездь
        THIRD_PARTY_PAYMENT; // Оплата на стороннем приложении
    }

    public static final ObjectMapper objectMapper = new ObjectMapper();

    public static String dateFormatTZ = "yyyy-MM-dd'T'HH:mm:ssZ";
    public static String dateOnly = "yyyy-MM-dd";
    public static String simpleDateTimeFormat = "yyyy-MM-dd HH:mm:ss";
    public static String dateFormat = "dd.MM.yyyy HH:mm:ss";
    public static String whitelistPlugin = "whitelist-plugin";
    public static String billingPlugin = "billing-plugin";
    public static String ratePlugin = "rate-plugin";
    public static String bookingPlugin = "booking-plugin";
    public static String zerotouchPlugin = "zerotouch-plugin";
    public static String abonementPlugin = "abonoment-plugin";
    public static String parkomatPlugin = "parkomat-status-plugin";
    public static String carImageExtension = ".jpeg";
    public static String carImageSmallAddon = "_resize_w_200_h_100";
    public static String carImagePropertyName = "carImageUrl";
    public static String carSmallImagePropertyName = "carSmallImageUrl";
    public static String carmodelPlugin = "carmodel-plugin";
    public static String megaPlugin = "mega-plugin";
}
