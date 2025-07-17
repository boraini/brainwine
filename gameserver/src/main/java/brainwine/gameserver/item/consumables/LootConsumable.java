package brainwine.gameserver.item.consumables;

import brainwine.gameserver.GameServer;
import brainwine.gameserver.dialog.Dialog;
import brainwine.gameserver.dialog.DialogSection;
import brainwine.gameserver.item.Item;
import brainwine.gameserver.item.LazyItemGetter;
import brainwine.gameserver.loot.Loot;
import brainwine.gameserver.player.Player;
import brainwine.gameserver.server.messages.InventoryMessage;

public class LootConsumable implements Consumable {
    private LazyItemGetter keyItem = new LazyItemGetter("consumables/lockboxkey");

    @Override
    public void consume(Item item, Player player, Object details) {
        if(!player.isGodMode() && item.isLocked() && (keyItem.get().isAir() || !player.getInventory().hasItem(keyItem.get()))) {
            fail(player, item, "You need a key to unlock this " + item.getTitle() + "!");
            return;
        }

        player.showDialog(new Dialog()
                .setTitle("Opening " + item.getTitle())
                .addSection(new DialogSection().setText("Would you like to open this " + item.getTitle() + (
                        item.isLocked()
                            ? " using a " + keyItem.get().getTitle() + "?"
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
        String[] lootTables = item.getLootCategories();
        Loot loot = GameServer.getInstance().getLootManager().getRandomLoot(player, lootTables);
        if(loot == null) {
            fail(player, item, "Couldn't find any loot for you.");
            return;
        }

        player.awardLoot(loot);
        player.getInventory().removeItem(item, true);
        if(item.isLocked()) player.getInventory().removeItem(keyItem.get(), true);
    }

    private void fail(Player player, Item item, String message) {
        if(message != null) {
            player.notify(message);
        }
        player.sendMessage(new InventoryMessage(player.getInventory().getClientConfig(item)));
    }
}
