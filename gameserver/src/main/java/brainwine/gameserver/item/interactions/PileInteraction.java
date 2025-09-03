package brainwine.gameserver.item.interactions;

import brainwine.gameserver.entity.Entity;
import brainwine.gameserver.item.Item;
import brainwine.gameserver.item.Layer;
import brainwine.gameserver.player.Player;
import brainwine.gameserver.zone.Block;
import brainwine.gameserver.zone.MetaBlock;
import brainwine.gameserver.zone.Zone;

public class PileInteraction implements ItemInteraction {

    @Override
    public void interact(Zone zone, Entity entity, int x, int y, Layer layer, Item item, int mod, MetaBlock metaBlock, Object config, Object[] data) {
        if(!entity.isPlayer()) return;
        Player player = (Player)entity;
        int pile = (int)config;

        if(!player.getInventory().hasItem(item.getInventoryItem(), pile)) {
            fail(player, "You don't have enough of this item to pile any more.");
            return;
        }

        Block block = zone.getBlock(x, y);

        if(block.getMod(layer) >= item.getMaxMod()) {
            fail(player, "This pile has gotten too big.");
            return;
        }

        player.getInventory().removeItem(item.getInventoryItem(), pile, true);
        zone.updateBlockMod(x, y, layer, block.getFrontMod() + 1);
    }

    private void fail(Player player, String message) {
        player.notify(message);
    }
}
