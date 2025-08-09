package brainwine.gameserver.item.interactions;

import brainwine.gameserver.dialog.Dialog;
import brainwine.gameserver.dialog.DialogHelper;
import brainwine.gameserver.dialog.DialogListItem;
import brainwine.gameserver.dialog.DialogSection;
import brainwine.gameserver.entity.Entity;
import brainwine.gameserver.item.Item;
import brainwine.gameserver.item.ItemRegistry;
import brainwine.gameserver.item.ItemUseType;
import brainwine.gameserver.item.Layer;
import brainwine.gameserver.player.Player;
import brainwine.gameserver.player.TradeSession;
import brainwine.gameserver.server.messages.InventoryMessage;
import brainwine.gameserver.zone.MetaBlock;
import brainwine.gameserver.zone.Zone;

public class SmelterInteraction implements ItemInteraction {
    @Override
    public void interact(Zone zone, Entity entity, int x, int y, Layer layer, Item item, int mod, MetaBlock metaBlock, Object config, Object[] data) {
        if(data == null || data.length == 0 || !(data[0] instanceof Number)) return;
        if(!(entity instanceof Player)) return;

        Item droppedItem = ItemRegistry.getItem((int) data[0]);
        Player player = (Player) entity;

        // Check if the smelter is receiving steam
        if(item.usesSteam() || item.hasUse(ItemUseType.EXTENDED_STEAMABLE)) {
            if(!zone.isBlockPowered(x, y)) {
                player.notify("You need to supply the machine with steam first.");
                return;
            }
        }

        // Check if the item can be smelted
        if(droppedItem.getSmelt() == null) {
            player.showDialog(DialogHelper.messageDialog(
                    "Cannot Smelt",
                    "Sorry but you can't smelt " + droppedItem.getTitle() + "."));
            return;
        }

        // Check if player has any of the dropped item
        if(!player.getInventory().hasItem(droppedItem)) {
            player.sendMessage(new InventoryMessage(player.getInventory().getClientConfig(droppedItem)));
            return;
        }

        Dialog dialog = new Dialog().setTitle("Forge").addSection(new DialogSection().addItem(new DialogListItem().setItem(droppedItem.getCode()).setText(droppedItem.getTitle()))).addSection(TradeSession.Dialogs.createQuantitySelector(player.getInventory().getQuantity(droppedItem)).setTitle("How many would you like to smelt?")).setActions("Smelt");

        player.showDialog(dialog, ans -> {
            if(ans.length > 0 && !"cancel".equals(ans[0])) {
                try {
                    int quantity = Integer.parseInt(ans[0].toString());
                    confirm(player, item, droppedItem, quantity);
                } catch(NumberFormatException ignored) {
                }
            }
        });
    }

    public void confirm(Player player, Item item, Item droppedItem, int quantity) {
        Item convertedItem = droppedItem.getSmelt();

        if(convertedItem == null) return;

        if(player.getInventory().hasItem(droppedItem, quantity)) {
            player.getInventory().removeItem(droppedItem, quantity, true);
            player.getInventory().addItem(convertedItem, quantity, true);
        } else {
            player.sendMessage(new InventoryMessage(player.getInventory().getClientConfig(droppedItem)));
        }
    }
}
