package company.vk.edu.distrib.compute.aartchik.moderation;

import company.vk.edu.distrib.compute.moderation.LinkCreatedEvent;
import company.vk.edu.distrib.compute.moderation.ModerationResultEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EventCodecTest {
    @Test
    void roundTripsEventsWithoutLosingUnicodeOrDelimiters() {
        Instant now = Instant.now();
        LinkCreatedEvent created = new LinkCreatedEvent("abc012XYZ9", "https://example.com/é?q=%09&x=a+b", now);
        assertEquals(created, EventCodec.decodeCreated(EventCodec.encode(created)));
        ModerationResultEvent result = new ModerationResultEvent("abc012XYZ9", false, "Причина\tс\nразделителями", now);
        assertEquals(result, EventCodec.decodeResult(EventCodec.encode(result)));
    }

    @Test
    void rejectsPoisonAndInvalidTimestampAsMalformedEvents() {
        assertThrows(IllegalArgumentException.class, () -> EventCodec.decodeCreated(null));
        for (String value : List.of(
                "poison {{{", "1\tabc012XYZ9\t???\t2026-11-24T00:00:00Z",
                "1\tabc012XYZ9\taHR0cHM6Ly9leGFtcGxlLmNvbQ==\tnot-a-timestamp",
                "1\tbad\taHR0cHM6Ly9leGFtcGxlLmNvbQ==\t2026-11-24T00:00:00Z")) {
            assertThrows(IllegalArgumentException.class, () -> EventCodec.decodeCreated(value));
        }
        assertThrows(IllegalArgumentException.class,
                () -> EventCodec.decodeResult("1\tabc012XYZ9\tmaybe\t\t2026-11-24T00:00:00Z"));
    }
}
