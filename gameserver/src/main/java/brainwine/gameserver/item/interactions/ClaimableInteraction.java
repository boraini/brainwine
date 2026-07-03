package brainwine.gameserver.item.interactions;

import brainwine.gameserver.competition.CompetitionEntry;
import brainwine.gameserver.competition.CompetitionPhase;
import brainwine.gameserver.competition.ZoneCompetition;
import brainwine.gameserver.dialog.Dialog;
import brainwine.gameserver.dialog.DialogSection;
import brainwine.gameserver.entity.Entity;
import brainwine.gameserver.item.Item;
import brainwine.gameserver.item.ItemUseType;
import brainwine.gameserver.item.Layer;
import brainwine.gameserver.player.Player;
import brainwine.gameserver.util.MathUtils;
import brainwine.gameserver.zone.Block;
import brainwine.gameserver.zone.MetaBlock;
import brainwine.gameserver.zone.Zone;
import brainwine.shared.JsonHelper;

import java.io.IOException;
import java.util.StringJoiner;

public class ClaimableInteraction implements ItemInteraction {

    @Override
    public void interact(Zone zone, Entity entity, int x, int y, Layer layer, Item item, int mod, MetaBlock metaBlock, Object config, Object[] data) {
        if (!entity.isPlayer()) return;
        Player player = (Player)entity;

        // Do not let changing owner.
        if (metaBlock.hasOwner()) {
            if (metaBlock.getOwner().equals(player)) {
                player.notify("You have already claimed this " + item.getTitle() + ".");
            } else {
                player.notify("This " + item.getTitle() + " is owned by " + metaBlock.getOwner().getName() + ".");
            }
            return;
        }

        if(item.hasUse(ItemUseType.COMPETITION)) {
            if(!zone.hasCompetition() || zone.getCompetition().getPhase() != CompetitionPhase.ACTIVE) {
                player.notify("You may not claim this outside of an active competition.");
            }

            // Do not let keep other player's landmarks inside the field.
            MetaBlock playerLandmark = null;
            for(MetaBlock landmark : zone.getMetaBlocksWithUse(ItemUseType.LANDMARK)) {
                if(landmark.hasOwner() && landmark.getOwner() != player && MathUtils.inRange(x, y, landmark.getX(), landmark.getY(), item.getField())) {
                    player.notify("You may not claim this right now since there are landmarks of other players inside its field.");
                    return;
                }
            }
        }

        zone.updateBlock(x, y, layer, item, 0, player);

        if(item.hasUse(ItemUseType.COMPETITION)) {
            // Add a new competition entry
            zone.getCompetition().addEntry(player, x, y);

            // Set the entry title from the landmark, if a landmark is found.
            CompetitionEntry entry = zone.getCompetition().getEntry(x, y);
            MetaBlock playerLandmark = ZoneCompetition.findLandmark(zone, entry);
            if(playerLandmark != null) {
                if(playerLandmark.isOwnedBy(player)) {
                    updateCompetitionEntryFromLandmark(entry, playerLandmark);
                } else if(!playerLandmark.hasOwner()) {
                    // Let the player edit the associated scoring landmark *only once*
                    metaBlock.setOwner(player);
                    metaBlock.removeProperty("cd");
                    zone.sendBlockMetaUpdate(metaBlock);
                    Object object = playerLandmark.getItem().getUse(ItemUseType.CREATE_DIALOG);
                    if(object != null) {
                        try {
                            Dialog createDialog = JsonHelper.readValue(object, Dialog.class);
                            for(DialogSection section : createDialog.getSections()) {
                                if(section.getInput() != null && metaBlock.getProperty(section.getInput().getKey()) != null) {
                                    section.getInput().setValue(metaBlock.getProperty(section.getInput().getKey()).toString());
                                }
                            }
                            player.showDialog(createDialog, ans -> {
                                // Guard against stuff that might have changed since the player was first shown the dialog.
                                if(!zone.hasCompetition()) return;
                                CompetitionEntry newEntry = zone.getCompetition().getEntry(metaBlock.getX(), metaBlock.getY());
                                if(newEntry == null) return;
                                if(zone.getMetaBlock(metaBlock.getX(), metaBlock.getY()) != metaBlock) return;
                                Block block = zone.getBlockSafe(x, y);
                                if(block == null) return;

                                // Fake a create-dialog interaction
                                ItemUseType.CREATE_DIALOG.getInteraction()
                                        .interact(zone, player, metaBlock.getX(), metaBlock.getY(), Layer.FRONT, metaBlock.getItem(), block.getFrontMod(), metaBlock, object, ans);
                                // Update the entry title from the new meta-block
                                MetaBlock newMetaBlock = zone.getMetaBlock(metaBlock.getX(), metaBlock.getY());
                                if(newMetaBlock == null) return;
                                updateCompetitionEntryFromLandmark(newEntry, newMetaBlock);
                            });
                        } catch (IOException ignored) {}
                    }
                }
            }
        }

        zone.spawnEffect(x, y, "chime", 1);
        player.notify("You now have claimed this " + item.getTitle() + ", good luck!");
    }

    private void updateCompetitionEntryFromLandmark(CompetitionEntry entry, MetaBlock playerLandmark) {
        String t1 = playerLandmark.getStringProperty("t1");
        String t2 = playerLandmark.getStringProperty("t2");
        String t3 = playerLandmark.getStringProperty("t3");
        StringJoiner sj = new StringJoiner(" ");
        if (t1 != null) sj.add(t1);
        if (t2 != null) sj.add(t2);
        if (t3 != null) sj.add(t3);
        String joined = sj.toString();
        if(!joined.trim().isEmpty()) {
            entry.setTitle(joined.trim());
        }
    }
}
