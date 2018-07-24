import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class TimezoneTest {

    public static void main(String[] args) {
	TimeZone timeZone = TimeZone.getTimeZone("UTC");
	Calendar calendar = Calendar.getInstance(timeZone);
	SimpleDateFormat simpleDateFormat =
	       new SimpleDateFormat("EE MMM dd HH:mm:ss zzz yyyy", Locale.US);
	simpleDateFormat.setTimeZone(timeZone);

	System.out.println("Time zone: " + timeZone.getID());
	System.out.println("default time zone: " + TimeZone.getDefault().getID());
	System.out.println();

	System.out.println("UTC:     " + simpleDateFormat.format(calendar.getTime()));
	System.out.println("Default: " + calendar.getTime());
    }

    public static void main1(String[] args) {
	DateTimeZone zone = DateTimeZone.forID("Europe/London");
	DateTimeFormatter format = DateTimeFormat.longDateTime();

	long current = System.currentTimeMillis();
	long currentUTC = DateTimeZone.UTC.convertLocalToUTC(current, true);
	System.out.println("currentUTC=[" + format.print(currentUTC));

	System.out.println("[" + format.print(current) + "] falls into DST? " + !zone.isStandardOffset(current));
	for (int i = 0; i < 100; i++) {
	    long next = zone.nextTransition(current);
	    if (current == next) {
		break;
	    }
	    System.out.println("UTC=[" + format.print(DateTimeZone.UTC.convertLocalToUTC(next, true)));
	    System.out.println(" [" + format.print(next) + "] falls into DST? " + !zone.isStandardOffset(next));
	    current = next;
	}
    }
}