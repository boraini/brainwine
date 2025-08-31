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

import java.util.Collections;
import java.util.function.Function;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

public class ConvertItemInteraction implements ItemInteraction {
    private String machineName = "Converter";
    private String actionName = "convert";
    private Function<Item, Map<Item, Integer>> getConversion;

    public ConvertItemInteraction(String machineName, String actionName, Function<Item, Map<Item, Integer>> getConversion) {
        this.machineName = machineName;
        this.actionName = actionName;
        this.getConversion = getConversion;
    }

    @Override
    public void interact(Zone zone, Entity entity, int x, int y, Layer layer, Item item, int mod, MetaBlock metaBlock, Object config, Object[] data) {
        if(data == null || data.length == 0) return;
        int itemCode = -1;
        // { code }
        if(data.length == 1 && data[0] instanceof Number) itemCode = (int)data[0];
        // { "item", code }
        else if(data.length >= 2 && data[1] instanceof Number) itemCode = (int)data[1];
        if(itemCode < 0) return;

        if(!(entity instanceof Player)) return;

        Item droppedItem = ItemRegistry.getItem(itemCode);
        Player player = (Player) entity;

        // Check if the machine is receiving steam
        if(item.usesSteam() || item.hasUse(ItemUseType.EXTENDED_STEAMABLE)) {
            if(!zone.isBlockPowered(x, y)) {
                player.notify("You need to supply the machine with steam first.");
                return;
            }
        }

        // Check if the item can be converted
        if(getConversion.apply(droppedItem).isEmpty()) {
            player.showDialog(DialogHelper.messageDialog(
                    StringUtils.capitalize("Cannot " + actionName),
                    "Sorry but you can't " + actionName + " " + droppedItem.getTitle() + "."));
            return;
        }

        // Check if player has any of the dropped item
        if(!player.getInventory().hasItem(droppedItem)) {
            player.sendMessage(new InventoryMessage(player.getInventory().getClientConfig(droppedItem)));
            return;
        }

        Dialog dialog = new Dialog().setTitle(this.machineName).addSection(new DialogSection().addItem(new DialogListItem().setItem(droppedItem.getCode()).setText(droppedItem.getTitle()))).addSection(TradeSession.Dialogs.createQuantitySelector(player.getInventory().getQuantity(droppedItem)).setTitle("How many would you like to " + actionName + "?")).setActions(actionName);

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
        if(getConversion.apply(droppedItem).isEmpty()) {
            return;
        }

        if(player.getInventory().hasItem(droppedItem, quantity)) {
            player.getInventory().removeItem(droppedItem, quantity, true);
            for(Map.Entry<Item, Integer> entry : getConversion.apply(droppedItem).entrySet()) {
                player.getInventory().addItem(entry.getKey(), quantity * entry.getValue(), true);
            }
        } else {
            player.sendMessage(new InventoryMessage(player.getInventory().getClientConfig(droppedItem)));
        }
    }
}
