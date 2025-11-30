package brainwine.gameserver.item.interactions;

import brainwine.gameserver.entity.Entity;
import brainwine.gameserver.item.Item;
import brainwine.gameserver.item.Layer;
import brainwine.gameserver.player.Player;
import brainwine.gameserver.zone.MetaBlock;
import brainwine.gameserver.zone.Zone;

public class FertilizerInteraction implements ItemInteraction {
    @Override
    public void interact(Zone zone, Entity entity, int x, int y, Layer layer, Item item, int mod, MetaBlock metaBlock, Object config, Object[] data) {
        if(entity instanceof Player) {
            Player player = (Player)entity;

            if(!zone.getGrowthManager().isReceivingLight(x, y)) {
                player.notify("Area is not receiving enough light.");
                return;
            }

            if(!zone.isPurified()) {
                ((Player) entity).notify("This world is not purified.");
                return;
            }
        }

        zone.getGrowthManager().fertilize(x, y);
    }
}
