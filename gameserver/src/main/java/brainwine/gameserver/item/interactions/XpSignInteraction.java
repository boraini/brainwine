package brainwine.gameserver.item.interactions;

import brainwine.gameserver.entity.Entity;
import brainwine.gameserver.item.Item;
import brainwine.gameserver.item.Layer;
import brainwine.gameserver.player.Player;
import brainwine.gameserver.util.MapHelper;
import brainwine.gameserver.zone.MetaBlock;
import brainwine.gameserver.zone.Zone;

import java.util.HashMap;
import java.util.Map;

public class XpSignInteraction implements ItemInteraction {

    @Override
    public void interact(Zone zone, Entity entity, int x, int y, Layer layer, Item item, int mod, MetaBlock metaBlock, Object config, Object[] data) {
        if(!entity.isPlayer()) return;

        // Do nothing if data is invalid
        if(data != null) {
            return;
        }

        Player player = (Player)entity;

        Map<String, Object> v = MapHelper.getMap(metaBlock.getMetadata(), "v");
        if(v != null && v.containsKey(player.getDocumentId())) {
            player.notify("You have already gotten your XP from this.");
            return;
        }

        Map<String, Object> currentVotes = MapHelper.getMap(metaBlock.getMetadata(), "v", new HashMap<>());
        currentVotes.put(player.getDocumentId(), System.currentTimeMillis());
        metaBlock.setProperty("v", currentVotes);
        metaBlock.setProperty("vc", config);
        zone.sendBlockMetaUpdate(metaBlock);

        player.addExperience((int)config);
    }
}
