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
        final Map<String, Integer> ECOLOGICAL_MACHINE_PART_COUNTS = Map.of(
            "purifier", 7,
            "composter", 5,
            "recycler", 5
        );

        final Map<String, String> MACHINE_ITEM_IDS = Map.of(
            "purifier", "mechanical/purifier",
            "composter", "mechanical/composter-chamber",
            "recycler", "mechanical/recycler"
        );

        // Do nothing if entity is dead or not a player
        if (!entity.isPlayer() || entity.isDead()) {
            return;
        }

        Player player = (Player)entity;

        if (!(item.getUse(ItemUseType.WORLD_MACHINE_ACTIVATION) instanceof String)) return;

        String machineType = (String) item.getUse(ItemUseType.WORLD_MACHINE_ACTIVATION);

        int properParts = zone.getEcologicalMachineParts().getOrDefault(machineType, 0);
        int pooledParts = zone.getEcologicalMachineParts().getOrDefault("common_machine_part_pool", 0);
        int requiredParts = ECOLOGICAL_MACHINE_PART_COUNTS.getOrDefault(machineType, Integer.MAX_VALUE);

        if (properParts + pooledParts >= requiredParts) {
            String machineItemId = MACHINE_ITEM_IDS.get(machineType);
            Item machine = Item.get(machineItemId);

            if (machine == null) player.notify(String.format(
                "Activated state of the machine is not recognized. ID was %s. Please report to the server developers.",
                machineItemId
            ));
            zone.updateBlock(x, y, Layer.FRONT, machine);
            zone.getEcologicalMachineParts().put("common_machine_part_pool", properParts >= requiredParts ? pooledParts : pooledParts - (requiredParts - properParts));
            zone.getEcologicalMachineParts().put(machineType, properParts >= requiredParts ? properParts - requiredParts : 0);
            player.notify(String.format("You have successfully activated the %s!", machine.getTitle()), NotificationType.ACCOMPLISHMENT);
        } else {
            player.notify(String.format(
                "Not all parts to activate this machine have been collected yet. %d out of %d have been collected.",
                properParts + pooledParts,
                requiredParts
            ));
        }
    }
    
}
