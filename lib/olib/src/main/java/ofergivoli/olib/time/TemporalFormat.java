package ofergivoli.olib.time;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;

public class TemporalFormat {

	public static final String fullTimeForamt = "yyyy-MM-dd  HH:mm:ss";
	public static final String fullTimeForFilenameFormat = "yyyy-MM-dd__HH-mm-ss.SSS";

	public static String temporalToString(TemporalAccessor date, String format, Locale locale) {
		return DateTimeFormatter.ofPattern(format,locale).format(date);
	}
	
	public static String temporalToString(TemporalAccessor date, String format) {
		return temporalToString(date,format,Locale.ROOT);
	}
	
	/**
	 * In US Locale.
	 */
	public static String temporalToStringUS(TemporalAccessor date, String format) {
		return temporalToString(date,format,Locale.US);
	}

	public static String getCurrentTimeInFullTimeForFilenameFormat() {
        return temporalToString(ZonedDateTime.now(), fullTimeForFilenameFormat);
    }
}
