package com.livingword.items;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class BibleItemEntityProtectionTest {
    private static final Path SOURCE = Path.of("src/main/java/com/livingword/items/BibleItemEntityProtection.java");

    @Test
    void droppedBiblesAreMadeInvulnerableAndNeverExpireWhenTheyEnterTheWorld() throws Exception {
        String source = Files.readString(SOURCE);

        assertTrue(source.contains("EntityJoinLevelEvent"), "dropped Bible protection should run when item entities enter the level");
        assertTrue(source.contains("setInvulnerable(true)"), "dropped Bibles should use vanilla invulnerability as a first line of protection");
        assertTrue(source.contains("setUnlimitedLifetime()"), "dropped Bibles should not despawn from normal item expiry");
        assertTrue(source.contains("LivingWordItems.BIBLE.get()"), "protection must be limited to the Living Word Bible item");
    }

    @Test
    void droppedBiblesAreInvulnerableToDamageSourcesAndExplosions() throws Exception {
        String source = Files.readString(SOURCE);

        assertTrue(source.contains("EntityInvulnerabilityCheckEvent"), "damage-source checks should force dropped Bibles invulnerable");
        assertTrue(source.contains("event.setInvulnerable(true)"), "damage-source checks should override non-fire damage like cactus, explosions, and void damage");
        assertTrue(source.contains("ExplosionEvent.Detonate"), "explosions should not include dropped Bibles in their affected entity list");
        assertTrue(source.contains("getAffectedEntities().removeIf"), "dropped Bibles should be removed from TNT/explosion entity effects");
    }

    @Test
    void droppedBiblesAreRescuedBeforeTheVoidCanDiscardThemDirectly() throws Exception {
        String source = Files.readString(SOURCE);

        assertTrue(source.contains("EntityTickEvent.Pre"), "void protection needs to run before ItemEntity.tick can discard below-world items");
        assertTrue(source.contains("getMinBuildHeight() - 48"), "dropped Bibles should be rescued before vanilla's below-world discard threshold");
        assertTrue(source.contains("getSharedSpawnPos()"), "void-rescued Bibles should return to a safe world location");
        assertTrue(source.contains("teleportTo"), "void-rescued Bibles should be moved back into the level instead of being deleted");
    }

    @Test
    void voidRescueNotifiesPlayersWithExactSpawnCoordinates() throws Exception {
        String source = Files.readString(SOURCE);

        assertTrue(source.contains("Component.literal"), "void rescue should send a readable chat message");
        assertTrue(source.contains("serverLevel.players()"), "void rescue should notify players in the affected level");
        assertTrue(source.contains("sendSystemMessage"), "void rescue should use normal chat system messages");
        assertTrue(source.contains("x=%.1f, y=%.1f, z=%.1f"), "void rescue message should include exact return coordinates");
        assertTrue(source.contains("sent to spawn"), "void rescue message should say the Bible was sent to spawn");
    }

    @Test
    void bibleProtectionIsRegisteredOnTheCommonEventBus() throws Exception {
        String modSource = Files.readString(Path.of("src/main/java/com/livingword/LivingWord.java"));
        String source = Files.readString(SOURCE);

        assertTrue(source.contains("NeoForge.EVENT_BUS.addListener"));
        assertTrue(modSource.contains("BibleItemEntityProtection.register()"));
    }
}
