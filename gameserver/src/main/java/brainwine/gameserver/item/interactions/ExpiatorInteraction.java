package brainwine.gameserver.item.interactions;

import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import brainwine.gameserver.entity.Entity;
import brainwine.gameserver.item.Item;
import brainwine.gameserver.item.ItemRegistry;
import brainwine.gameserver.item.ItemUseType;
import brainwine.gameserver.item.Layer;
import brainwine.gameserver.player.NotificationType;
import brainwine.gameserver.player.Player;
import brainwine.gameserver.zone.Block;
import brainwine.gameserver.zone.EcologicalMachine;
import brainwine.gameserver.zone.MetaBlock;
import brainwine.gameserver.zone.Zone;

/**
 * Interaction handler for the expiator
 */
public class ExpiatorInteraction extends EcologicalMachineInteraction {

    private static final String ACTION_ID = "expiateSoulCrystal";
    public ExpiatorInteraction() {
        super(EcologicalMachine.EXPIATOR);
    }
    
    @Override
    public void interact(Zone zone, Player player, int x, int y) {
        Item soulCrystal = ItemRegistry.getItem("accessories/crystal-soul");
        if(!soulCrystal.isAir()) {
            Block block = zone.getBlockSafe(x, y);
            if(block != null && "soul-crystal".equals(block.getFrontItem().getUse(ItemUseType.EXPIATOR))) {
                // live-server specific configuration.
                if(player.isActionOnCooldown(ACTION_ID, 5, ChronoUnit.SECONDS)) {
                    player.notify("You may only expiate " + soulCrystal.getTitle() + "s every 5 seconds.");
                } else if(!player.getInventory().hasItem(soulCrystal)) {
                    player.notify("Sorry, but you don't have any " + soulCrystal.getTitle() + "s to expiate.");
                } else {
                    player.getInventory().removeItem(soulCrystal, 1, true);
                    player.recordActionTime(ACTION_ID);
                    zone.spawnEffect(x + 2.0F, y, "expiate", 10);
                    player.notify("You released a lost soul!", NotificationType.ACCOMPLISHMENT);
                    player.notifyPeers(String.format("%s released a lost soul.", player.getName()), NotificationType.PEER_ACCOMPLISHMENT);
                    player.getStatistics().trackDeliverances(1);
                }
                return;
            }
        }

        // TODO create a more generic function for this
        List<Entity> ghosts = zone.getNpcs().stream()
                .filter(npc -> npc.getConfig().getName().equals("ghost") && npc.inRange(x, y, 5.0))
                .collect(Collectors.toList());
        
        // Check if there are ghosts nearby
        if(ghosts.isEmpty()) {
            player.notify("No ghosts in range.");
            return;
        }

        // This allows the server operator to choose what infernal protectors do.
        Item hellDish = ItemRegistry.getItem("hell/dish");
        List<MetaBlock> protectors;
        if(hellDish.hasUse(ItemUseType.GUARD_WAVES)) {
            protectors = Collections.emptyList();
        } else {
            protectors = zone.getMetaBlocksWithItem(hellDish);
            Collections.shuffle(protectors);
        }
        
        // Expiate nearby ghosts
        for(Entity ghost : ghosts) {
            ghost.setHealth(0.0F);
            
            // Destroy a random infernal protector
            if(!protectors.isEmpty()) {
                MetaBlock protector = protectors.remove(0);
                zone.updateBlock(protector.getX(), protector.getY(), Layer.FRONT, 0);
            }
        }
        
        zone.spawnEffect(x + 2.0F, y, "expiate", 10);
        player.notify("You released a lost soul!", NotificationType.ACCOMPLISHMENT);
        player.notifyPeers(String.format("%s released a lost soul.", player.getName()), NotificationType.PEER_ACCOMPLISHMENT);
        player.getStatistics().trackDeliverances(ghosts.size());
    }
}
