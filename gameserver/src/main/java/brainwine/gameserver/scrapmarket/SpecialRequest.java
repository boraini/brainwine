package brainwine.gameserver.scrapmarket;

import brainwine.gameserver.item.Item;
import brainwine.gameserver.player.Player;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Represents a barter trade where a player can exchange specific items for other items.
 */
public class SpecialRequest {
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("required_items")
    private Map<String, Integer> requiredItemIds;
    
    @JsonProperty("reward_items")
    private Map<String, Integer> rewardItemIds;
    
    // Cached Item objects
    private Map<Item, Integer> requiredItems;
    private Map<Item, Integer> rewardItems;
    
    /**
     * Check if the player has all required items for this trade
     */
    public boolean canPlayerAfford(Player player) {
        for(Map.Entry<Item, Integer> entry : requiredItems.entrySet()) {
            if(player.getInventory().getQuantity(entry.getKey()) < entry.getValue()) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Execute the trade - remove required items and give reward items
     */
    public void executeTrade(Player player) {
        // Remove required items
        for(Map.Entry<Item, Integer> entry : requiredItems.entrySet()) {
            player.getInventory().removeItem(entry.getKey(), entry.getValue(), true);
        }
        
        // Give reward items
        for(Map.Entry<Item, Integer> entry : rewardItems.entrySet()) {
            player.getInventory().addItem(entry.getKey(), entry.getValue(), true);
        }
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public Map<Item, Integer> getRequiredItems() {
        return requiredItems;
    }
    
    public Map<Item, Integer> getRewardItems() {
        return rewardItems;
    }
}
