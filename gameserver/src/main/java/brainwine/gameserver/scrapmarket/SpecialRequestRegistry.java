package brainwine.gameserver.scrapmarket;

import brainwine.gameserver.resource.ResourceFinder;
import brainwine.shared.JsonHelper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static brainwine.shared.LogMarkers.SERVER_MARKER;

/**
 * Registry for managing barter trades that can be offered by trader androids.
 * Loads trades from a JSON configuration file.
 */
public class SpecialRequestRegistry {
    private static final Logger logger = LogManager.getLogger();
    private static final String CONFIG_FILE = "barter-trades.json";
    private static SpecialRequestRegistry instance;
    
    private List<SpecialRequest> trades = new ArrayList<>();
    
    private SpecialRequestRegistry() {
    }
    
    /**
     * Load barter trades from the configuration file
     */
    public void loadTrades() {
        logger.info(SERVER_MARKER, "Loading barter trades ...");
        trades.clear();
        
        try {
            URL url = ResourceFinder.getResourceUrl(CONFIG_FILE);
            
            if(url == null) {
                logger.info(SERVER_MARKER, "No barter trades configuration file found ({}), skipping barter trades", CONFIG_FILE);
                return;
            }
            
            List<SpecialRequest> loadedTrades = JsonHelper.readValue(url, new TypeReference<List<SpecialRequest>>() {});
            
            // Initialize each trade (convert item IDs to Item objects)
            for(SpecialRequest trade : loadedTrades) {
                // Only add trades that have valid items
                if(!trade.getRequiredItems().isEmpty() && !trade.getRewardItems().isEmpty()) {
                    trades.add(trade);
                } else {
                    logger.warn(SERVER_MARKER, "Skipping barter trade '{}' - missing valid required or reward items", trade.getName());
                }
            }
            
            logger.info(SERVER_MARKER, "Successfully loaded {} barter trade{}", trades.size(), trades.size() == 1 ? "" : "s");
        } catch(Exception e) {
            logger.error(SERVER_MARKER, "Could not load barter trades from {}", CONFIG_FILE, e);
        }
    }
    
    /**
     * Get all available barter trades
     */
    public List<SpecialRequest> getTrades() {
        return trades;
    }
    
    /**
     * Get the singleton instance
     */
    public static SpecialRequestRegistry getInstance() {
        if(instance == null) {
            instance = new SpecialRequestRegistry();
        }
        return instance;
    }
}
