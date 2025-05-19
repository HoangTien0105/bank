package com.bank.utils;

import java.time.LocalDate;
import java.time.Period;

public class DateUtils {
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
