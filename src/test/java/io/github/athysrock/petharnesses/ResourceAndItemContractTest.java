package io.github.athysrock.petharnesses;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.*;
import io.github.athysrock.petharnesses.item.HarnessDefinition;
import java.nio.file.*;
import org.junit.jupiter.api.Test;

final class ResourceAndItemContractTest {
    private static final Path RESOURCES = Path.of("src/main/resources");

    @Test
    void recipeIsExactlyLeatherStringPattern() throws Exception {
        JsonObject recipe = JsonParser.parseString(Files.readString(
                RESOURCES.resolve("data/pet_harnesses/recipe/pet_harness.json"))).getAsJsonObject();
        assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString());
        JsonArray pattern = recipe.getAsJsonArray("pattern");
        assertEquals("LLL", pattern.get(0).getAsString());
        assertEquals("SLS", pattern.get(1).getAsString());
        assertEquals("LLL", pattern.get(2).getAsString());
        assertEquals("minecraft:leather", recipe.getAsJsonObject("key").get("L").getAsString());
        assertEquals("minecraft:string", recipe.getAsJsonObject("key").get("S").getAsString());
        assertEquals("pet_harnesses:pet_harness", recipe.getAsJsonObject("result").get("id").getAsString());
        assertEquals(1, recipe.getAsJsonObject("result").get("count").getAsInt());
    }

    @Test
    void harnessIsNonStackingLegEquipmentWithoutArmorAttributes() {
        assertEquals(1, HarnessDefinition.MAX_STACK_SIZE);
        assertEquals("LEGS", HarnessDefinition.EQUIPMENT_SLOT);
        assertEquals(0, HarnessDefinition.ARMOR_PROTECTION);
    }

    @Test
    void allThreePlaceholderItemModelsAndEnglishNameExist() throws Exception {
        for (String state : new String[] {"pet_harness", "pet_harness_linked", "pet_harness_occupied"}) {
            assertTrue(Files.isRegularFile(RESOURCES.resolve("assets/pet_harnesses/items/" + state + ".json")), state);
            assertTrue(Files.isRegularFile(RESOURCES.resolve("assets/pet_harnesses/models/item/" + state + ".json")), state);
            assertTrue(Files.isRegularFile(RESOURCES.resolve("assets/pet_harnesses/textures/item/" + state + ".png")), state);
        }
        JsonObject lang = JsonParser.parseString(Files.readString(
                RESOURCES.resolve("assets/pet_harnesses/lang/en_us.json"))).getAsJsonObject();
        assertEquals("Pet Harness", lang.get("item.pet_harnesses.pet_harness").getAsString());
    }
}
