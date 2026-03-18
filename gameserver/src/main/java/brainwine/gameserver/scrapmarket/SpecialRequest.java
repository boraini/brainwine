package brainwine.gameserver.scrapmarket;

import brainwine.gameserver.item.Item;
import brainwine.gameserver.player.Player;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a barter trade where a player can exchange specific items for other items.
 */
public class SpecialRequest {
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("description")
    private String description;

    private Map<Item, Integer> requiredItems = new HashMap<>();

    private Map<Item, Integer> rewardItems = new HashMap<>();
    
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

    @JsonSetter("required_items")
    public void setRequiredItems(Map<String, Integer> requiredItems) {
        this.requiredItems.clear();
        for(Map.Entry<String, Integer> entry : requiredItems.entrySet()) {
            Item item = ItemRegistry.getItem(entry.getKey());

            if (!item.isAir()) {
                this.requiredItems.put(item, entry.getValue());
            }
        }
    }

    @JsonSetter("reward_items")
    public void setRewardItems(Map<String, Integer> rewardItems) {
        this.rewardItems.clear();
        for(Map.Entry<String, Integer> entry : rewardItems.entrySet()) {
            Item item = ItemRegistry.getItem(entry.getKey());

            if (!item.isAir()) {
                this.rewardItems.put(item, entry.getValue());
            }
        }
    }
    
    public Map<Item, Integer> getRequiredItems() {
        return requiredItems;
    }
    
    public Map<Item, Integer> getRewardItems() {
        return rewardItems;
    }
}
