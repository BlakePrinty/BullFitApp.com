package com.bullfitapp.bullfit.common;

import org.springframework.stereotype.Component;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component("dt")
public class DateDisplay {

    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    private ZonedDateTime zoned(LocalDateTime utc, String zone) {
        ZoneId id = ZoneOptions.isValid(zone) ? ZoneId.of(zone) : ZoneId.of(ZoneOptions.DEFAULT);
        return utc.atZone(ZoneOffset.UTC).withZoneSameInstant(id);
    }

    public String date(LocalDateTime utc, String zone) {
        return utc == null ? "" : DATE.format(zoned(utc, zone));
    }

    public String time(LocalDateTime utc, String zone) {
        return utc == null ? "" : TIME.format(zoned(utc, zone));
    }

    public String dateTime(LocalDateTime utc, String zone) {
        return utc == null ? "" : date(utc, zone) + " at " + time(utc, zone);
    }

    public String day(LocalDate date) {
        return date == null ? "" : DATE.format(date);
    }
}
