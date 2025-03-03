package com.vending.utils.date;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public class DateUtil {
    public static final DateTimeFormatter FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private static final ZoneId DEFAULT_ZONE = ZoneId.systemDefault();

    public static String formattaDateTime(LocalDateTime dateTime) {
        return dateTime.format(FORMATTER);
    }
    public static DateTimeFormatter getFormatter() {
        return FORMATTER;
    }
    public static LocalDateTime parseDateTime(String dateTime) {
        return LocalDateTime.parse(dateTime, FORMATTER);
    }

    public static Date localDateTimeToDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(DEFAULT_ZONE).toInstant());
    }

    public static LocalDateTime dateToLocalDateTime(Date date) {
        return LocalDateTime.ofInstant(date.toInstant(), DEFAULT_ZONE);
    }

    public static boolean isScaduto(LocalDateTime dateTime) {
        return dateTime.isBefore(LocalDateTime.now());
    }

    public static long getDifferenzaGiorni(LocalDateTime data1, LocalDateTime data2) {
        return ChronoUnit.DAYS.between(data1, data2);
    }

    public static LocalDateTime inizioGiornata(LocalDateTime dateTime) {
        return dateTime.toLocalDate().atStartOfDay();
    }

    public static LocalDateTime fineGiornata(LocalDateTime dateTime) {
        return dateTime.toLocalDate().atTime(23, 59, 59);
    }
}
