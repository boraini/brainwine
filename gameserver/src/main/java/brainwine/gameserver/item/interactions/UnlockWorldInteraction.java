package brainwine.gameserver.item.interactions;

import brainwine.gameserver.GameServer;
import brainwine.gameserver.dialog.Dialog;
import brainwine.gameserver.dialog.DialogSection;
import brainwine.gameserver.entity.Entity;
import brainwine.gameserver.item.Item;
import brainwine.gameserver.item.ItemUseType;
import brainwine.gameserver.item.Layer;
import brainwine.gameserver.player.Player;
import brainwine.gameserver.zone.*;
import brainwine.gameserver.zone.gen.ZoneGenerator;

import java.time.temporal.ChronoUnit;

public class UnlockWorldInteraction implements ItemInteraction {
    private static final String actionKey = "unlockWorldPortal";

    @Override
    public void interact(Zone zone, Entity entity, int x, int y, Layer layer, Item item, int mod, MetaBlock metaBlock,
            Object config, Object[] data) {
        if(!(entity instanceof Player))
            return;

        Player player = (Player) entity;

        if(!player.isGodMode()) {
            Block block = zone.getBlock(x, y);
            if(block != null) {
                if(player.getBlockHash() != block.getOwnerHash()) {
                    player.notify("Sorry, you may only use " + (player.isV3() ? item.getFancyTitle() : item.getTitle()) + "s of your own.");
                    return;
                }
            }
        }

        if(!player.isGodMode() && (item.usesSteam() || item.hasUse(ItemUseType.EXTENDED_STEAMABLE))) {
            if(!zone.isBlockPowered(x, y)) {
                player.notify("Sorry, you must power this with steam first.");
                return;
            }
        }
        if(!player.isGodMode() && player.isActionOnCooldown(actionKey, 5L, ChronoUnit.DAYS)) {
            player.notify("You can only use an " + item.getTitle() + " every 5 days.");
            return;
        }

        player.showDialog(new Dialog()
                .setTitle("Using " + item.getTitle())
                .addSection(new DialogSection().setText("Would you like to teleport to a new world of your own?")),
                ans -> {
                    if(ans.length == 0) {
                        confirm(item, player);
                    }
                });
    }

    private void confirm(Item item, Player player) {
        player.recordActionTime(actionKey);

        Biome biome = Biome.getRandomBiome();
        int width = biome == Biome.DEEP ? 1200 : 2000;
        int height = biome == Biome.DEEP ? 1000 : 600;
        int seed = (int) (Math.random() * Integer.MAX_VALUE);

        ZoneGenerator generator = ZoneGenerator.getZoneGenerator(biome);

        player.notify("Your zone is being generated. It should be ready soon!");
        generator.generateZoneAsync(biome, width, height, seed, zone -> {
            if(zone == null) {
                player.notify("An unexpected error occurred while generating your zone. Your " + item.getTitle()
                        + "is returned.");
            } else {
                zone.setOwner(player);
                zone.setPrivate(true);
                zone.setProtected(true);
                zone.setRules(ZoneRules.getPrivateDefaults());
                GameServer.getInstance().getZoneManager().addZone(zone);

                // Send player to the newly created zone
                player.changeZone(zone);
            }
        });
    }

}
