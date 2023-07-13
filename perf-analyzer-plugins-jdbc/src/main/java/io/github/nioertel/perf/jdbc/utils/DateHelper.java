package io.github.nioertel.perf.jdbc.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class DateHelper {

	public static final ZoneId EUROPE_BERLIN = ZoneId.of("Europe/Berlin");

	private DateHelper() {
	}

	public static ZonedDateTime toZonedDateTime(long epochMillis) {
		return Instant.ofEpochMilli(epochMillis).atZone(EUROPE_BERLIN);
	}

	public static LocalDateTime toLocalDateTime(long epochMillis) {
		return toZonedDateTime(epochMillis).toLocalDateTime();
	}
}
