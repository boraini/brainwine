package brainwine.gameserver.item.consumables;

import brainwine.gameserver.GameServer;
import brainwine.gameserver.dialog.Dialog;
import brainwine.gameserver.dialog.DialogSection;
import brainwine.gameserver.item.Item;
import brainwine.gameserver.item.ItemRegistry;
import brainwine.gameserver.loot.Loot;
import brainwine.gameserver.loot.LootManager;
import brainwine.gameserver.player.Player;
import brainwine.gameserver.server.messages.InventoryMessage;

public class LootConsumable implements Consumable {
    @Override
    public void consume(Item item, Player player, Object details) {
        Item keyToUse = LootManager.getKeyItemToUse(player, item);
        if(item.isLocked() && keyToUse.isAir()) {
            fail(player, item, "You need a key to unlock this " + item.getTitle() + "!");
            return;
        }

        player.showDialog(new Dialog()
                .setTitle("Opening " + item.getTitle())
                .addSection(new DialogSection().setText("Would you like to open this " + item.getTitle() + (
                        item.isLocked()
                            ? " using a " + keyToUse.getTitle() + "?"
                            : "?"
                        )))
                , ans -> {
                    if(ans.length == 0) {
                        confirm(item, player);
                    } else {
                        fail(player, item, null);
                    }
                }
        );
    }

    private void confirm(Item item, Player player) {
        if(!player.isGodMode() && !player.getInventory().hasItem(item)) {
            fail(player, item, String.format("Sorry, you don't have any %ss.", item.getTitle()));
            return;
        }

        Item keyToUse = LootManager.getKeyItemToUse(player, item);
        String[] lootCategories = LootManager.getLootCategoriesForKey(item, keyToUse);

        if(lootCategories.length == 0) {
            fail(player, item, "You need a key to unlock this " + item.getTitle() + "!");
            return;
        }

        Loot loot = GameServer.getInstance().getLootManager().getRandomLoot(player, lootCategories);
        if(loot == null) {
            fail(player, item, "Couldn't find any loot for you.");
            return;
        }

        player.awardLoot(loot);
        player.getInventory().removeItem(item, true);
        if(!keyToUse.isAir()) player.getInventory().removeItem(keyToUse, true);
    }

    private void fail(Player player, Item item, String message) {
        if(message != null) {
            player.notify(message);
        }
        player.sendMessage(new InventoryMessage(player.getInventory().getClientConfig(item)));
    }
}
