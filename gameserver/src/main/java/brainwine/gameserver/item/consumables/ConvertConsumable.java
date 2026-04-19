package brainwine.gameserver.item.consumables;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import brainwine.gameserver.dialog.Dialog;
import brainwine.gameserver.dialog.DialogSection;
import brainwine.gameserver.dialog.input.DialogSelectInput;
import brainwine.gameserver.item.Item;
import brainwine.gameserver.item.ItemRegistry;
import brainwine.gameserver.player.Inventory;
import brainwine.gameserver.player.Player;
import brainwine.gameserver.server.messages.InventoryMessage;

/**
 * Consumable handler for upgrade kits
 */
public class ConvertConsumable implements Consumable {

    @Override
    public void consume(Item item, Player player, Object details) {
        Map<Item, Map<Item, Integer>> conversions = item.getConversions();
        Inventory inventory = player.getInventory();
        
        // Find items in the player's inventory that can be upgraded
        Set<Item> convertables = conversions.keySet().stream().filter(i -> inventory.hasItem(i)).collect(Collectors.toSet());
        
        // Don't do anything if the player has no items that can be converted
        if(convertables.isEmpty()) {
            player.notify("You do not have any upgradeable items.");
            player.sendMessage(new InventoryMessage(inventory.getClientConfig(item)));
            return;
        }
        
        // Map item titles to their id
        Map<String, String> keyMap = convertables.stream().collect(Collectors.toMap(Item::getTitle, Item::getId, (a, b) -> a));
        
        // Create upgrade dialog
        Dialog dialog = new Dialog().addSection(new DialogSection()
                .setTitle("Which item would you like to upgrade?")
                .setInput(new DialogSelectInput()
                        .setOptions(convertables.stream().map(Item::getTitle).collect(Collectors.toList()))
                        .setMaxColumns(3)
                        .setKey("item")));
        
        player.showDialog(dialog, data -> {
            // Handle cancellation
            if(data.length == 1 && data[0].equals("cancel")) {
                player.sendMessage(new InventoryMessage(player.getInventory().getClientConfig(item)));
                return;
            }
            
            // Fail if player no longer has consumable
            if(!inventory.hasItem(item)) {
                fail(item, player);
                return;
            }
            
            // Fail if there is no data
            if(data.length == 0) {
                fail(item, player);
                return;
            }
            
            String key = keyMap.get(data[0]);
            
            // Fail if the chosen item title doesn't map to an id
            if(key == null) {
                fail(item, player);
                return;
            }
            
            Item itemToUpgrade = ItemRegistry.getItem(key);
            Map<Item, Integer> targetItems = conversions.get(itemToUpgrade);
            
            // Fail if the player doesn't have the item they want to upgrade or there is no upgrade for it
            if(!inventory.hasItem(itemToUpgrade) || targetItems == null) {
                fail(item, player);
                return;
            }
            
            inventory.removeItem(item, true); // Remove the consumable
            inventory.removeItem(itemToUpgrade, true); // Remove the item that was upgraded
            // Add the items that the item upgraded to :)
            for(Map.Entry<Item, Integer> targetItemAndQty : targetItems.entrySet()) {
                inventory.addItem(targetItemAndQty.getKey(), targetItemAndQty.getValue(), true);
            }

            player.notify(String.format("%s upgraded to %s", itemToUpgrade.getTitle(), formatQuantities(targetItems)));
        });
    }

    private static String formatQuantities(Map<Item, Integer> targetItems) {
        String[] esEnding = "s|ch|sh|x|z".split("\\|");
        String[] anStarting = "a|e|i|o|u".split("\\|");
        StringBuilder message = new StringBuilder();
        int i = 0;
        for(Map.Entry<Item, Integer> targetItemAndQty : targetItems.entrySet()) {
            if(targetItems.size() > 1 && i == targetItems.size() - 1) {
                message.append(targetItems.size() > 2 ? ", and " : " and ");
            } else if(i > 0) {
                message.append(", ");
            }
            String itemTitle = targetItemAndQty.getKey().getTitle() != null ? targetItemAndQty.getKey().getTitle() : targetItemAndQty.getKey().getId();
            String itemTitleLower = itemTitle.toLowerCase();
            if(targetItemAndQty.getValue() != 1) {
                message.append(targetItemAndQty.getValue());
                message.append(" ");
                message.append(itemTitle);
                message.append(Arrays.stream(esEnding).anyMatch(itemTitleLower::endsWith) ? "es" : "s");
            } else {
                message.append(Arrays.stream(anStarting).anyMatch(itemTitleLower::startsWith) ? "an" : "a");
                message.append(" ");
                message.append(itemTitle);
            }
            i++;
        }
        return message.toString();
    }

    private void fail(Item item, Player player) {
        player.notify("Oops! There was a problem with the upgrade.");
        player.sendMessage(new InventoryMessage(player.getInventory().getClientConfig(item)));
    }
}
