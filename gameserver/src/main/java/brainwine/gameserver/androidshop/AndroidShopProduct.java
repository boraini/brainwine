package brainwine.gameserver.androidshop;

import brainwine.gameserver.dialog.Dialog;
import brainwine.gameserver.dialog.DialogListItem;
import brainwine.gameserver.dialog.DialogSection;
import brainwine.gameserver.item.Item;
import brainwine.gameserver.player.NotificationType;
import brainwine.gameserver.player.Player;

public class AndroidShopProduct {
    private Item item = Item.AIR;
    private int price = 0;
    private int maxQuantityPerDay = Integer.MAX_VALUE;

    public AndroidShopProduct(Item item, int price, int maxQuantityPerDay) {
        this.item = item;
        this.price = price;
        this.maxQuantityPerDay = maxQuantityPerDay;
    }

    public Item getItem() {
        return item;
    }

    public int getPrice() {
        return price;
    }

    public int getMaxQuantityPerDay() {
        return maxQuantityPerDay;
    }

    public void purchase(Player player, int quantity) {
        DialogSection section = new DialogSection().setTitle("You received:");
        Dialog dialog = new Dialog().addSection(section);

        if(quantity > 0) {
            section.addItem(new DialogListItem()
                    .setItem(item.getCode())
                    .setImage(String.format("inventory/%s", item.getId()))
                    .setText(String.format("%s x %s", item.getTitle(), quantity)));
            player.getInventory().addItem(item, quantity, true);
        }

        // Show dialog
        if(player.isV3()) {
            player.showDialog(dialog);
        } else {
            player.notify(dialog, NotificationType.REWARD);
        }
    }
}
