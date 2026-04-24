package brainwine.gameserver.item.consumables;

import brainwine.gameserver.GameServer;
import brainwine.gameserver.dialog.Dialog;
import brainwine.gameserver.dialog.DialogSection;
import brainwine.gameserver.item.Item;
import brainwine.gameserver.player.Player;
import brainwine.gameserver.server.messages.InventoryMessage;
import brainwine.gameserver.zone.Biome;
import brainwine.gameserver.zone.ZoneRules;
import brainwine.gameserver.zone.gen.ZoneGenerator;

import java.time.temporal.ChronoUnit;

public class UnlockWorldConsumable implements Consumable {
    private static final String actionKey = "unlockWorld";
    @Override
    public void consume(Item item, Player player, Object details) {
        if (!player.isGodMode() && player.isActionOnCooldown(actionKey, 5L, ChronoUnit.MINUTES)) {
            fail(player, item, "You can only use an " + item.getTitle() + " every 5 minutes.");
            return;
        }

        player.showDialog(new Dialog()
                        .setTitle("Using " + item.getTitle())
                        .addSection(new DialogSection().setText("Would you like to use this " + item.getTitle() + "?"))
                , ans -> {
                    if(ans.length == 0) {
                        confirm(item, player);
                    } else {
                        fail(player, item, null);
                    }
                }
        );
    }

    private void confirm(Item item, Player player) {
        if(!player.getInventory().hasItem(item)) {
            fail(player, item, String.format("Sorry, you don't have any %ss.", item.getTitle()));
            return;
        }

        if(!player.isGodMode() && GameServer.getInstance().getZoneManager().getZones().stream()
                .filter(z -> player.getDocumentId().equals(z.getOwner()))
                .count() >= 30
        ) {
            fail(player, item, "Sorry, you own too many worlds. Use an Auctioneer's Gavel to sell one before generating another.");
            return;
        }

        player.recordActionTime(actionKey);

        Biome biome = item.getBiome() != null ? item.getBiome() : Biome.getRandomBiome();
        int width = biome == Biome.DEEP ? 1200 : 2000;
        int height = biome == Biome.DEEP ? 1000 : 600;
        int seed = (int)(Math.random() * Integer.MAX_VALUE);

        ZoneGenerator generator = ZoneGenerator.getZoneGenerator(biome);

        player.getInventory().removeItem(item, true);
        player.notify("Your zone is being generated. It should be ready soon!");
        generator.generateZoneAsync(biome, width, height, seed, zone -> {
            if(zone == null) {
                player.getInventory().addItem(item);
                fail(player, item, "An unexpected error occurred while generating your zone. Your " + item.getTitle() + "is returned.");
            } else {
                zone.setOwner(player);
                zone.setPrivate(true);
                zone.setProtected(true);
                zone.setRules(ZoneRules.getPrivateDefaults());
                GameServer.getInstance().getZoneManager().addZone(zone);
                player.notify(String.format("Your zone '%s' is ready for exploration!", zone.getName()));
            }
        });
    }

    private void fail(Player player, Item item, String message) {
        if(message != null) {
            player.notify(message);
        }
        player.sendMessage(new InventoryMessage(player.getInventory().getClientConfig(item)));
    }
}
