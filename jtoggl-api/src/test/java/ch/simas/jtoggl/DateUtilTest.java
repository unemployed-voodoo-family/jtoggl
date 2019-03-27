package ch.simas.jtoggl;

import ch.simas.jtoggl.util.DateUtil;
import org.junit.Test;
import org.junit.Assert;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;

public class DateUtilTest {

    @Test
    public void convertDateToString() {
        Calendar cal = Calendar.getInstance();
        cal.set(2019, 1, 1, 1, 1, 0);
        Date d = cal.getTime();
        Assert.assertEquals("2019-02-01T01:01:00+01:00", DateUtil.convertDateToString(d));
    }

    @Test
    public void convertZonedDateToString() {
        OffsetDateTime d = OffsetDateTime.of(2019, 2, 1, 1, 1, 0, 0, ZoneOffset.ofHours(1));
        Assert.assertEquals("2019-02-01T01:01:00+01:00", DateUtil.convertDateToString(d));
    }
}
