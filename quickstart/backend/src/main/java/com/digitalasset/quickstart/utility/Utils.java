package com.digitalasset.quickstart.utility;

import daml_stdlib_da_time_types.da.time.types.RelTime;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public class Utils {

    private Utils() {}

    public static RelTime parseRelTime(String durationStr) {
        Duration duration = Duration.parse(durationStr);
        long micros = duration.toNanos() / 1_000;
        return new RelTime(micros);
    }

    public static OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant == null ? null : OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
