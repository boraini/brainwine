package brainwine.gameserver.item.interactions;

import brainwine.gameserver.entity.Entity;
import brainwine.gameserver.item.Item;
import brainwine.gameserver.item.ItemUseType;
import brainwine.gameserver.item.Layer;
import brainwine.gameserver.item.usetypeconfig.GuardWavesConfig;
import brainwine.gameserver.player.Player;
import brainwine.gameserver.zone.MetaBlock;
import brainwine.gameserver.zone.Zone;

public class GuardWavesInteraction implements ItemInteraction {
    @Override
    public void interact(Zone zone, Entity entity, int x, int y, Layer layer, Item item, int mod, MetaBlock metaBlock, Object config, Object[] data) {
        if(!entity.isPlayer()) return;
        Player player = (Player)entity;

        if(metaBlock == null) {
            player.notify("Sorry, could not find the " + item.getTitle() + " you are trying to interact with.");
        }

        // Check if the first wave was already initialized
        boolean alreadyInitialized = metaBlock != null && metaBlock.hasProperty("w") && metaBlock.hasProperty("!");

        // Update the block mod if it was 0
        GuardWavesConfig guardWavesConfig = item.getStructuredUse(ItemUseType.GUARD_WAVES);
        if(mod == 0 && !guardWavesConfig.isAuto()) {
            zone.updateBlock(x, y, layer, item, 1);
        }

        // Always update guard waves just in case (for example when interacting with an infernal protector which are supposed to be always active)
        zone.getEntityManager().updateGuardWaves(x, y, false);
        if(!alreadyInitialized) {
            player.notify("Oh no, better contain it now!");
        }
    }
}
