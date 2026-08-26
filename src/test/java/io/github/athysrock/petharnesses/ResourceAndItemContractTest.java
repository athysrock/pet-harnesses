package io.github.athysrock.petharnesses;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class ResourceAndItemContractTest {
    @Test
    void recipeUsesLeatherAndString() throws Exception {
        JsonObject recipe = JsonParser.parseString(Files.readString(Path.of(
                "src/main/resources/data/pet_harnesses/recipe/pet_harness.json"))).getAsJsonObject();

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
}
