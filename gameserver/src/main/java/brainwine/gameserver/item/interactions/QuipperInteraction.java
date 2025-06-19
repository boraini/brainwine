package brainwine.gameserver.item.interactions;

import brainwine.gameserver.Fake;
import brainwine.gameserver.entity.Entity;
import brainwine.gameserver.item.Item;
import brainwine.gameserver.item.Layer;
import brainwine.gameserver.server.messages.EffectMessage;
import brainwine.gameserver.zone.MetaBlock;
import brainwine.gameserver.zone.Zone;

import java.time.temporal.ChronoUnit;
import java.util.NoSuchElementException;

public class QuipperInteraction implements ItemInteraction {
    private static final String actionName = "quipper";
    
    @Override
    public void interact(Zone zone, Entity entity, int x, int y, Layer layer, Item item, int mod, MetaBlock metaBlock, Object config, Object[] data) {
        if(!(config instanceof String)) return;

        if(zone.isActionOnCooldown(actionName, 500, ChronoUnit.MILLIS)) return;

        String message;
        try {
            message = Fake.get((String)config);
        } catch(NoSuchElementException e) {
            message = "I don't know what to say.";
        }

        zone.sendMessage(new EffectMessage(x + 0.5f, y - 0.5f, "emote", message));
        zone.recordActionTime(actionName);
    }
}
