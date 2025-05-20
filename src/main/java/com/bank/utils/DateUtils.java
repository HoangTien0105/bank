package com.bank.utils;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.TimeZone;

public class DateUtils {
    private static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String TIMEZONE_VIETNAM = "Asia/Ho_Chi_Minh";

    public static String formatDate(Date date) {
        return formatDate(date, DEFAULT_DATE_FORMAT);
    }

    public static String formatDate(Date date, String pattern) {
        if (date == null) return null;
        SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);
        dateFormat.setTimeZone(TimeZone.getTimeZone(TIMEZONE_VIETNAM));
        return dateFormat.format(date);
    }

    public static int calculateMonths(LocalDate from, LocalDate to) {
        if (from.isEqual(to)) {
            return 1;
        }

        Period period = Period.between(from, to);
        int months = period.getYears() * 12 + period.getMonths();

        // Kiểm tra ngày cuối cùng của tháng
        int lastDayOfFromMonth = from.lengthOfMonth();
        int lastDayOfToMonth = to.lengthOfMonth();

        // Nếu ngày bắt đầu là ngày cuối tháng, và ngày kết thúc cũng là ngày cuối tháng
        // Hoặc ngày kết thúc >= ngày bắt đầu
        if ((from.getDayOfMonth() == lastDayOfFromMonth && to.getDayOfMonth() == lastDayOfToMonth) ||
                to.getDayOfMonth() > from.getDayOfMonth()) {
            months++;
        }

        return months;
    }
}
