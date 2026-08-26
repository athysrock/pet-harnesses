package io.github.athysrock.petharnesses.component;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.athysrock.petharnesses.network.CapturedPetNetworkSanitizer;
import java.util.UUID;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

final class CapturedPetNetworkCodecTest {
    @Test
    void networkPatchOmitsCapturedPetComponentWithoutMutatingAuthoritativePatch() {
        CompoundTag fullEntity = new CompoundTag();
        fullEntity.putString("id", "minecraft:wolf");
        fullEntity.putString("private_mod_data", "must stay server side");
        CapturedPetData authoritative = new CapturedPetData(
                CapturedPetData.CURRENT_VERSION,
                UUID.fromString("00000000-0000-0000-0000-000000000002"),
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                fullEntity);
        DataComponentType<CapturedPetData> serverOnly = DataComponentType.<CapturedPetData>builder()
                .persistent(CapturedPetData.CODEC).build();
        DataComponentType<String> unrelatedModComponent = DataComponentType.<String>builder()
                .persistent(com.mojang.serialization.Codec.STRING).build();
        DataComponentPatch source = DataComponentPatch.builder()
                .set(serverOnly, authoritative)
                .set(unrelatedModComponent, "must remain")
                .build();

        DataComponentPatch network = CapturedPetNetworkSanitizer.forNetwork(source, serverOnly);

        assertFalse(network.entrySet().stream()
                .anyMatch(entry -> entry.getKey() == serverOnly));
        assertTrue(source.entrySet().stream()
                .anyMatch(entry -> entry.getKey() == serverOnly));
        assertTrue(network.entrySet().stream()
                .anyMatch(entry -> entry.getKey() == unrelatedModComponent
                        && entry.getValue().orElseThrow().equals("must remain")));
    }
}
