package brainwine.gameserver.order;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.type.TypeReference;

import brainwine.shared.JsonHelper;

import static brainwine.shared.LogMarkers.SERVER_MARKER;

/**
 * Loads the chat name colors for each Order of the Crow tier from a 'order-colors.json' file next to the running server.
 */
public class OrderColors {

    private static final String FILE_NAME = "order-colors.json";
    private static final Logger logger = LogManager.getLogger();
    private static final String[] DEFAULT_NAMES = { "Iron", "Brass", "Sapphire", "Ruby", "Onyx", "Platinum" };
    private static final String[] DEFAULT_HEX = { "#808080", "#FFD700", "#4169E1", "#FF0000", "#5B2C6F", "#F0F8FF" };
    private static Map<Integer, String> colorsByTier = new LinkedHashMap<>();

    private OrderColors() {}

    /**
     * Loads the order-colors.json file. It creates the file if it is not found, with the default colors.
     */
    public static void load() {
        colorsByTier.clear();
        File file = new File(FILE_NAME);

        if(!file.exists()) {
            logger.info(SERVER_MARKER, "{} not found, creating it with default colors. Edit this file to customize order chat colors.", FILE_NAME);
            writeDefaults();
        }

        try {
            Map<String, TierColorEntry> loaded = JsonHelper.readValue(file, new TypeReference<LinkedHashMap<String, TierColorEntry>>() {});

            for(Map.Entry<String, TierColorEntry> entry : loaded.entrySet()) {
                TierColorEntry tierColor = entry.getValue();

                try {
                    int tier = Integer.parseInt(entry.getKey());

                    if(tierColor != null && tierColor.color != null) {
                        colorsByTier.put(tier, tierColor.color);
                    }
                } catch(NumberFormatException e) {
                    logger.warn(SERVER_MARKER, "Ignoring non-numeric order tier key '{}' in {}", entry.getKey(), FILE_NAME);
                }
            }

            logger.info(SERVER_MARKER, "Loaded {} order chat color(s) from {}", colorsByTier.size(), FILE_NAME);
        } catch(IOException e) {
            logger.error(SERVER_MARKER, "Failed to load {}, order chat colors will be disabled", FILE_NAME, e);
        }
    }

    /**
     * Creates the order-colors.json file with default colors.
     */
    private static void writeDefaults() {
        try {
            File file = new File(FILE_NAME);
            Map<String, TierColorEntry> defaults = new LinkedHashMap<>();

            for(int i = 0; i < DEFAULT_NAMES.length; i++) {
                defaults.put(String.valueOf(i + 1), new TierColorEntry(DEFAULT_NAMES[i], DEFAULT_HEX[i]));
            }

            JsonHelper.writeValue(file, defaults);
        } catch(IOException e) {
            logger.warn(SERVER_MARKER, "Could not write default {}", FILE_NAME, e);
        }
    }

    /**
     * Gets the defined color for the given order tier. Different orders have the same colors.
     * 
     * @param tier An order tier, 1-6 (Iron through Platinum).
     * @return The configured hex color for the tier, or null if none is configured.
     */
    public static String getColor(int tier) {
        return colorsByTier.get(tier);
    }

    public static class TierColorEntry {
        public String name;
        public String color;

        public TierColorEntry() {}

        public TierColorEntry(String name, String color) {
            this.name = name;
            this.color = color;
        }
    }
}
