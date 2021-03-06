package io.github.thebusybiscuit.slimefun4.testing.tests.researches;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;

import be.seeseemelk.mockbukkit.MockBukkit;
import io.github.thebusybiscuit.slimefun4.core.researching.Research;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunPlugin;
import io.github.thebusybiscuit.slimefun4.implementation.setup.ResearchSetup;

@TestMethodOrder(value = OrderAnnotation.class)
public class TestResearchSetup {

    @BeforeAll
    public static void load() {
        MockBukkit.mock();
        MockBukkit.load(SlimefunPlugin.class);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @Test
    @Order(value = 1)
    public void testForExceptions() {
        // Not really ideal but still important to test.
        // Research amount is variable, so we can't test for that.
        // We are really only concerned about any runtime exceptions here.
        SlimefunPlugin.getRegistry().setResearchingEnabled(true);
        Assertions.assertDoesNotThrow(() -> ResearchSetup.setupResearches());

        // Running it a second time should NOT be allowed.
        Assertions.assertThrows(UnsupportedOperationException.class, () -> ResearchSetup.setupResearches());
    }

    @Test
    @Order(value = 2)
    public void testResearchTranslations() throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/languages/researches_en.yml"), StandardCharsets.UTF_8))) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(reader);

            for (Research research : SlimefunPlugin.getRegistry().getResearches()) {
                String path = research.getKey().getNamespace() + '.' + research.getKey().getKey();
                Assertions.assertTrue(config.contains(path));
            }
        }
    }

}
