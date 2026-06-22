package uws.ac.uk.studymate.util

import org.junit.Assert.assertEquals
import org.junit.Test

/*//////////////////////
Unit tests for the focus-time formatter (0.9J).
 *//////////////////////
class DurationFormatTest {

    @Test fun zeroIsMinutes() = assertEquals("0m", DurationFormat.hoursMinutes(0))

    @Test fun underAnHourIsMinutesOnly() = assertEquals("45m", DurationFormat.hoursMinutes(45))

    @Test fun exactHourHasNoMinutes() = assertEquals("1h", DurationFormat.hoursMinutes(60))

    @Test fun hoursAndMinutes() = assertEquals("1h 30m", DurationFormat.hoursMinutes(90))

    @Test fun multipleHours() = assertEquals("2h 5m", DurationFormat.hoursMinutes(125))

    @Test fun negativeClampsToZero() = assertEquals("0m", DurationFormat.hoursMinutes(-10))
}
