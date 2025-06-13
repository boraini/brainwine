package brainwine.gameserver.item.consumables;

import brainwine.gameserver.GameServer;
import brainwine.gameserver.item.Item;
import brainwine.gameserver.item.ItemRegistry;
import brainwine.gameserver.loot.Loot;
import brainwine.gameserver.player.Player;
import brainwine.gameserver.server.messages.InventoryMessage;

public class LootConsumable implements Consumable {
    @Override
    public void consume(Item item, Player player, Object details) {
        Item keyItem = ItemRegistry.getItem("consumables/world-key");
        if(item.isLocked() && !player.getInventory().hasItem(keyItem)) {
            fail(player, item, "You need a key to unlock this " + item.getTitle() + "!");
            return;
        }

        String[] lootTables = item.getLootCategories();
        Loot loot = GameServer.getInstance().getLootManager().getRandomLoot(player, lootTables);
        if(loot == null) {
            fail(player, item, "Couldn't find any loot for you.");
            return;
        }

        player.awardLoot(loot);
        player.getInventory().removeItem(item, true);
        if(item.isLocked()) player.getInventory().removeItem(keyItem, true);
    }

    private void fail(Player player, Item item, String message) {
        player.notify(message);
        player.sendMessage(new InventoryMessage(player.getInventory().getClientConfig(item)));
    }
}
