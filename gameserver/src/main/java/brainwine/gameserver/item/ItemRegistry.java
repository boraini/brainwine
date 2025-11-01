package brainwine.gameserver.item;

import static brainwine.shared.LogMarkers.SERVER_MARKER;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import brainwine.gameserver.player.AppearanceSlot;

public class ItemRegistry {
    
    private static final Logger logger = LogManager.getLogger();
    private static final Map<String, Item> items = new HashMap<>();
    private static final Map<Integer, Item> itemsByCode = new HashMap<>();
    private static final Map<String, List<Item>> itemsByCategory = new HashMap<>();
    private static final Map<Item, Item> pilesByItem = new HashMap<>();
    private static final List<String> hiddenItems = new ArrayList<>();
    
    // TODO maybe just move the registry stuff here
    public static void clear() {
        items.clear();
        itemsByCode.clear();
    }
    
    public static boolean registerItem(Item item) {
        String id = item.getId();
        int code = item.getCode();
        
        if(items.containsKey(id)) {
            logger.warn(SERVER_MARKER, "Duplicate item id {} for code {}", id, code);
            return false;
        }
        
        if(itemsByCode.containsKey(code)) {
            logger.warn(SERVER_MARKER, "Duplicate item code {} for id {}", code, id);
            return false;
        }
        
        String category = item.getCategory();
        List<Item> categorizedItems = itemsByCategory.get(category);
        
        if(categorizedItems == null) {
            categorizedItems = new ArrayList<>();
            itemsByCategory.put(category, categorizedItems);
        }
        
        categorizedItems.add(item);
        
        if(item.isHidden()) {
            while(hiddenItems.size() < 9) {
                hiddenItems.add("air");
            }
            if(item.getCategory().equals("prosthetics") && item.hasAppearanceSlot()) {
                // Just putting them into hardcoded slots seems to work well
                int a = 6;
                int b = 0;
                if(item.getId().contains("onyx")) a = 0;
                else if(item.getId().contains("diamond")) a = 3;
                if(item.getAppearanceSlot() == AppearanceSlot.FACIAL_GEAR) b = 2;
                else if(item.getAppearanceSlot() == AppearanceSlot.TOPS_OVERLAY) b = 1;
                hiddenItems.set(a + b, item.getId());
            } else {
                // TODO v3 has a hard limit of 20 hidden items (see Inventory#maxLocationSlots in the game client)
                if(hiddenItems.size() == 20) {
                    logger.warn(SERVER_MARKER, "Upper hidden item limit has been reached. Certain hidden accessories might not work properly!");
                }
                
                hiddenItems.add(item.getId());
            }
        }
        
        items.put(id, item);
        itemsByCode.put(code, item);
        return true;
    }

    public static void registerItemRelationships() {
        for(Item item : items.values()) {
            // Record back relationship for piles
            // TODO maybe there is a better configuration option for this.
            if(item.hasUse(ItemUseType.PILE)) {
                Item inventoryItem = item.getInventoryItem();
                if(inventoryItem != item) {
                    pilesByItem.put(inventoryItem, item);
                }
            }
        }
    }
    
    public static Item getItem(String id) {
        return items.getOrDefault(id, Item.AIR);
    }
    
    public static Item getItem(int code) {
        return itemsByCode.getOrDefault(code, Item.AIR);
    }
    
    public static Collection<Item> getItems() {
        return Collections.unmodifiableCollection(items.values());
    }
    
    public static List<Item> getItemsByCategory(String category) {
        return Collections.unmodifiableList(itemsByCategory.getOrDefault(category, Collections.emptyList()));
    }

    public static Item getPile(Item inventory) {
        return pilesByItem.getOrDefault(inventory, Item.AIR);
    }
    
    public static int getHiddenItemIndex(Item item) {
        return hiddenItems.indexOf(item.getId());
    }
}
