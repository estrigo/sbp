package kz.spt.app.utils;

import lombok.experimental.UtilityClass;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@UtilityClass
public class DateTimeUtil {

    public static String getFormattedDurationString(Date inTimestamp, Date outTimestamp) {
        StringBuilder durationBuilder = new StringBuilder();
        String language = LocaleContextHolder.getLocale().getLanguage();

        if (inTimestamp != null) {

            long time_diff = (outTimestamp == null ? (new Date()).getTime() : outTimestamp.getTime()) - inTimestamp.getTime();
            long days_diff = TimeUnit.MILLISECONDS.toDays(time_diff) % 365;
            if (days_diff > 0) {
                durationBuilder
                        .append(days_diff)
                        .append(language.equals("ru") ? "д " : "d ");
            }

            long hours_diff = TimeUnit.MILLISECONDS.toHours(time_diff) % 24;
            if (hours_diff > 0 || durationBuilder.length() > 0) {
                durationBuilder
                        .append(hours_diff)
                        .append(language.equals("ru") ? "ч " : "h ");
            }

            long minutes_diff = TimeUnit.MILLISECONDS.toMinutes(time_diff) % 60;
            if (minutes_diff > 0 || durationBuilder.length() > 0) {
                durationBuilder
                        .append(minutes_diff)
                        .append(language.equals("ru") ? "м " : "m ");
            }

            long seconds_diff = TimeUnit.MILLISECONDS.toSeconds(time_diff) % 60;
            if (seconds_diff > 0 || durationBuilder.length() > 0) {
                durationBuilder
                        .append(seconds_diff)
                        .append(language.equals("ru") ? "с " : "s ");
            }
        }
        return durationBuilder.toString();
    }
}
