package io.github.athysrock.petharnesses.component;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class HarnessLinkStreamCodecTest {
    @Test
    void compactStreamCodecRoundTripsReciprocalIdentity() {
        HarnessLink original = new HarnessLink(
                HarnessLink.CURRENT_VERSION,
                UUID.fromString("00000000-0000-0000-0000-000000000010"),
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                UUID.fromString("00000000-0000-0000-0000-000000000002"));
        ByteBuf buffer = Unpooled.buffer();

        HarnessLink.STREAM_CODEC.encode(buffer, original);

        assertEquals(original, HarnessLink.STREAM_CODEC.decode(buffer));
        assertEquals(0, buffer.readableBytes());
    }
}
