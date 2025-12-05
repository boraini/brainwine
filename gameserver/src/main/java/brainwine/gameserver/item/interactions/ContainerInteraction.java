package brainwine.gameserver.item.interactions;

import java.util.stream.Stream;

import brainwine.gameserver.GameServer;
import brainwine.gameserver.dialog.Dialog;
import brainwine.gameserver.dialog.DialogSection;
import brainwine.gameserver.entity.Entity;
import brainwine.gameserver.item.Item;
import brainwine.gameserver.item.ItemRegistry;
import brainwine.gameserver.item.ItemUseType;
import brainwine.gameserver.item.Layer;
import brainwine.gameserver.loot.Loot;
import brainwine.gameserver.loot.LootManager;
import brainwine.gameserver.player.NotificationType;
import brainwine.gameserver.player.Player;
import brainwine.gameserver.util.MapHelper;
import brainwine.gameserver.zone.EcologicalMachine;
import brainwine.gameserver.zone.MetaBlock;
import brainwine.gameserver.zone.Zone;

/**
 * Interaction handler for lootable containers
 */
public class ContainerInteraction implements ItemInteraction {
    
    @Override
    public void interact(Zone zone, Entity entity, int x, int y, Layer layer, Item item, int mod, MetaBlock metaBlock,
            Object config, Object[] data) {
        // Do nothing if entity is not a player
        if(!entity.isPlayer()) {
            return;
        }
        
        // Check if the right data is present
        if(metaBlock == null || data != null) {
            return;
        }
        
        Player player = (Player)entity;
        String dungeonId = metaBlock.getStringProperty("@");
        
        // Check if container is protected
        if(item.hasUse(ItemUseType.FIELDABLE) && (zone.isBlockProtected(x, y, player) || (dungeonId != null && zone.isDungeonIntact(dungeonId)))) {
            player.notify(zone.getDungeonType(dungeonId).getContainerProtectedMessage());
            return;
        }
        
        boolean plenty = item.hasUse(ItemUseType.PLENTY);
        String lootCode = metaBlock.getStringProperty("y");
        
        // Check loot code
        if(plenty) {
            if(lootCode == null) {
                player.notify("This chest cannot be plundered.");
                return;
            }
            
            if(player.hasLootCode(lootCode)) {
                player.notify("You've already plundered this chest.");
                return;
            }
        }
        
        // Check for locked chests
        if(item.isLocked()) {
            Item keyToUse = LootManager.getKeyItemToUse(player, item);
            if(item.isLocked() && keyToUse.isAir()) {
                player.notify("You need a key to unlock this " + item.getTitle() + "!");
                return;
            }
            player.showDialog(new Dialog()
                .setTitle("Opening " + item.getTitle())
                .addSection(new DialogSection().setText("Would you like to open this " + item.getTitle() + " using a " + keyToUse.getTitle() + "?"))
                , ans -> {
                    if(ans.length == 0) {
                        awardLoot(zone, player, x, y, keyToUse);
                    }
                }
            );
        } else {
            awardLoot(zone, player, x, y, Item.AIR);
        }
    }

    public void awardLoot(Zone zone, Player player, int x, int y, Item keyToUse) {
        MetaBlock metaBlock = zone.getMetaBlock(x, y);
        if(metaBlock == null) {
            player.notify("Cannot find the block you are trying to loot.");
        }
        Item item = metaBlock.getItem();
        boolean plenty = item.hasUse(ItemUseType.PLENTY);
        String lootCode = metaBlock.getStringProperty("y");
        String specialItem = metaBlock.getStringProperty("$");

        if(!keyToUse.isAir() && !player.getInventory().hasItem(keyToUse)) {
            player.notify("You don't have any " + keyToUse.getTitle() + " anymore.");
            return;
        }

        String[] lootCategories = item.getLootCategories();
        if(item.isLocked()) {
            lootCategories = LootManager.getLootCategoriesForKey(item, keyToUse);
            if(lootCategories.length == 0) {
                player.notify("You need a key to unlock this " + item.getTitle() + "!");
                return;
            }
        }
        
        // Award loot
        if(specialItem != null) {
            if(specialItem.equals("?")) {
                Loot loot = metaBlock.hasProperty("l") ? new Loot(Item.get(metaBlock.getStringProperty("l")), metaBlock.getIntProperty("q"))
                        : GameServer.getInstance().getLootManager().getRandomLoot(player, item.getLootCategories());
                int experience = metaBlock.getIntProperty("xp");
                
                if(loot != null) {
                    if(plenty) {
                        player.addLootCode(lootCode);
                    } else {
                        metaBlock.removeProperty("$");
                        metaBlock.removeProperty("xp"); 
                    }
                    
                    player.awardLoot(loot, item.getLootGraphic(), "You found:");
                    player.addExperience(experience);
                    player.getStatistics().trackContainerLooted(item);
                } else {
                    player.notify("No eligible loot could be found for this container.");
                }
            } else {
                Item machinePart = ItemRegistry.getItem(specialItem);
                
                if(zone.addMachinePart(machinePart)) {
                    EcologicalMachine machine = EcologicalMachine.fromPart(machinePart);
                    String machineName = machine.toString().toLowerCase();
                    String determiner = Stream.of("a", "e", "i", "o", "u").filter(machineName::startsWith).findFirst().isPresent() ? "an" : "a";
                    String text = String.format("You discovered %s %s component!", determiner, machineName);
                    
                    if(player.isV3()) {
                        player.notify(text, NotificationType.ACCOMPLISHMENT);
                    } else {
                        Object message = MapHelper.map(String.class, String.class, 
                                "t", text,
                                "i", machinePart.getId());
                        player.notify(message, NotificationType.ACCOMPLISHMENT);
                    }
                    
                    player.notifyPeers(String.format("%s discovered %s %s component.", player.getName(), determiner, machineName), NotificationType.PEER_ACCOMPLISHMENT);
                    player.getStatistics().trackDiscovery(machinePart);
                    metaBlock.removeProperty("$");
                } else {
                    // TODO how should we handle this...?
                }
            }
        }

        if(!keyToUse.isAir()) player.getInventory().removeItem(keyToUse, true);
        
        // Update container mod
        if(!plenty && !metaBlock.hasProperty("$")) {
            zone.updateBlock(x, y, Layer.FRONT, item, 0, metaBlock.getOwner(), metaBlock.getMetadata());
        }
    }
}
