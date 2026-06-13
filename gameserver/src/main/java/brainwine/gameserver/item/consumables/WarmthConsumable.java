package brainwine.gameserver.item.consumables;

import brainwine.gameserver.item.Item;
import brainwine.gameserver.player.Player;

public class WarmthConsumable implements Consumable {
    public void consume(Item item, Player player, Object details) {
        if(player.getCold() < 0.05) return;

        player.applyWarmth();
        player.getInventory().removeItem(item, true);
        player.notify("Ahh, a nice warm cup of tea!");
    }
}
