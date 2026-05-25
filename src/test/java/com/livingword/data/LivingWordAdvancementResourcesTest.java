package com.livingword.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class LivingWordAdvancementResourcesTest {
    private static final Path ADVANCEMENTS = Path.of("src/main/resources/data/livingword/advancement/living_word");
    private static final Path RECIPE_ADVANCEMENTS = Path.of("src/main/resources/data/livingword/advancement/recipes/misc");

    @Test
    void livingWordHasVanillaAdvancementTree() throws Exception {
        List<String> advancementIds = List.of(
            "root",
            "scripture_disc",
            "shofar",
            "lectern_station",
            "complete_set"
        );

        for (String advancementId : advancementIds) {
            Path path = ADVANCEMENTS.resolve(advancementId + ".json");
            assertTrue(Files.isRegularFile(path), "Missing advancement " + path);
            JsonObject advancement = readObject(path);
            assertTrue(advancement.has("criteria"), "Advancement needs criteria: " + advancementId);
            assertTrue(advancement.has("display"), "Advancement needs display: " + advancementId);
        }

        JsonObject root = readObject(ADVANCEMENTS.resolve("root.json"));
        assertEquals("livingword:bible", firstInventoryItem(root, "has_bible"));
        assertFalse(root.has("parent"), "Root advancement should start the Living Word tab");

        JsonObject disc = readObject(ADVANCEMENTS.resolve("scripture_disc.json"));
        assertEquals("livingword:living_word/root", disc.get("parent").getAsString());
        assertEquals("livingword:scripture_disc_john", firstInventoryItem(disc, "has_scripture_disc"));

        JsonObject shofar = readObject(ADVANCEMENTS.resolve("shofar.json"));
        assertEquals("livingword:living_word/root", shofar.get("parent").getAsString());
        assertEquals("livingword:shofar", firstInventoryItem(shofar, "has_shofar"));
    }

    @Test
    void livingWordAdvancementsHaveTranslations() throws Exception {
        String lang = Files.readString(Path.of("src/main/resources/assets/livingword/lang/en_us.json"));

        for (String key : List.of(
            "advancements.livingword.root.title",
            "advancements.livingword.root.description",
            "advancements.livingword.scripture_disc.title",
            "advancements.livingword.scripture_disc.description",
            "advancements.livingword.shofar.title",
            "advancements.livingword.shofar.description",
            "advancements.livingword.lectern_station.title",
            "advancements.livingword.lectern_station.description",
            "advancements.livingword.complete_set.title",
            "advancements.livingword.complete_set.description"
        )) {
            assertTrue(lang.contains("\"" + key + "\""), "Missing lang key " + key);
        }
    }

    @Test
    void craftableLivingWordItemsUnlockInRecipeBook() throws Exception {
        for (String recipeId : List.of("bible", "scripture_disc_john", "shofar")) {
            Path path = RECIPE_ADVANCEMENTS.resolve(recipeId + ".json");
            assertTrue(Files.isRegularFile(path), "Missing recipe advancement " + path);
            JsonObject advancement = readObject(path);
            JsonArray recipes = advancement.getAsJsonObject("rewards").getAsJsonArray("recipes");
            assertEquals("livingword:" + recipeId, recipes.get(0).getAsString());
            assertTrue(advancement.getAsJsonObject("criteria").has("has_the_recipe"));
        }
    }

    private static String firstInventoryItem(JsonObject advancement, String criterionName) {
        JsonObject criterion = advancement.getAsJsonObject("criteria").getAsJsonObject(criterionName);
        assertEquals("minecraft:inventory_changed", criterion.get("trigger").getAsString());
        return criterion.getAsJsonObject("conditions")
            .getAsJsonArray("items")
            .get(0)
            .getAsJsonObject()
            .get("items")
            .getAsString();
    }

    private static JsonObject readObject(Path path) throws Exception {
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }
}
