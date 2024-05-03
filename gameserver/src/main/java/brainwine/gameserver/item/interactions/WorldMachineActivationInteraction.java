package brainwine.gameserver.item.interactions;

import java.util.Map;

import brainwine.gameserver.entity.Entity;
import brainwine.gameserver.entity.player.NotificationType;
import brainwine.gameserver.entity.player.Player;
import brainwine.gameserver.item.Item;
import brainwine.gameserver.item.ItemUseType;
import brainwine.gameserver.item.Layer;
import brainwine.gameserver.zone.MetaBlock;
import brainwine.gameserver.zone.Zone;

public class WorldMachineActivationInteraction implements ItemInteraction {
    
    @Override
    public void interact(Zone zone, Entity entity, int x, int y, Layer layer, Item item, int mod, MetaBlock metaBlock,
            Object config, Object[] data) {
        Map<String, Integer> ECOLOGICAL_MACHINE_PART_COUNTS = Map.of(
            "purifier", 1
        );

        Map<String, String> MACHINE_ITEM_IDS = Map.of(
            "purifier", "mechanical/purifier"
        );

        // Do nothing if entity is dead or not a player
        if (!entity.isPlayer() || entity.isDead()) {
            return;
        }

        Player player = (Player)entity;

        if (!(item.getUse(ItemUseType.WORLD_MACHINE_ACTIVATION) instanceof String)) return;


        String machineType = (String) item.getUse(ItemUseType.WORLD_MACHINE_ACTIVATION);

        if (zone.getEcologicalMachineParts().get(machineType) >= ECOLOGICAL_MACHINE_PART_COUNTS.get(machineType)) {
            String machineItemId = MACHINE_ITEM_IDS.get(machineType);
            Item machine = Item.get(machineItemId);

            if (machine == null) player.notify(String.format(
                "Activated state of the machine is not recognized. ID was %s. Please report to the server developers.",
                machineItemId
            ));
            zone.updateBlock(x, y, Layer.FRONT, machine);
            player.notify(String.format("You have successfully activated the ", machine.getTitle()), NotificationType.ACHIEVEMENT);
        } else {
            player.notify("Not all parts to activate this machine have been collected yet.");
        }
    }
    
}
