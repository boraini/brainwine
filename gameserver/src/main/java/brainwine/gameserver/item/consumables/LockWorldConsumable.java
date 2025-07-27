package brainwine.gameserver.item.consumables;

import brainwine.gameserver.dialog.Dialog;
import brainwine.gameserver.dialog.DialogSection;
import brainwine.gameserver.item.Item;
import brainwine.gameserver.player.NotificationType;
import brainwine.gameserver.player.Player;
import brainwine.gameserver.server.messages.InventoryMessage;
import brainwine.gameserver.server.messages.NotificationMessage;
import brainwine.gameserver.zone.Zone;
import brainwine.gameserver.zone.ZoneManager;

public class LockWorldConsumable implements Consumable {
    public static final int SMALL_WORLD_CROWN_REWARD = 25;
    public static final int LARGE_WORLD_CROWN_REWARD = 100;
    public static final int LARGE_WORLD_THRESHOLD = 2000 * 1000;

    @Override
    public void consume(Item item, Player player, Object details) {
        Zone zone = player.getZone();
        if(zone == null) return;

        if(!player.isGodMode()) {
            if(!player.getInventory().hasItem(item)) {
                fail(player, item, null);
                return;
            }

            if(!zone.isOwner(player)) {
                fail(player, item, "Sorry, you do not own this world.");
                return;
            }
        }

        if(zone.getRules().isDeleted()) {
            fail(player, item, "This world is already deleted.");
            return;
        }

        int blockSize = zone.getWidth() * zone.getHeight();
        int crownReward = blockSize >= LARGE_WORLD_THRESHOLD ? LARGE_WORLD_CROWN_REWARD : SMALL_WORLD_CROWN_REWARD;

        player.showDialog(new Dialog()
                        .addSection(new DialogSection().setText("You have chosen to delete this world in exchange of " + crownReward + " crowns."))
                        .addSection(new DialogSection().setText("You will lose access to the world for the foreseeable future. Are you sure you want to continue?")),
                ans -> {
                    if(ans.length == 0) confirm(player, item, zone);
                }
        );
    }

    public void fail(Player player, Item item, String message) {
        if(message != null) player.notify(message);
        player.sendMessage(new InventoryMessage(player.getInventory().getClientConfig(item)));
    }

    public void confirm(Player player, Item item, Zone zone) {
        int blockSize = zone.getWidth() * zone.getHeight();
        int crownReward = blockSize >= LARGE_WORLD_THRESHOLD ? LARGE_WORLD_CROWN_REWARD : SMALL_WORLD_CROWN_REWARD;

        if(!player.isGodMode()) {
            if(!player.getInventory().hasItem(item)) {
                fail(player, item, String.format("Sorry, you don't have any %ss.", item.getTitle()));
                return;
            }

            player.getInventory().removeItem(item, true);
            player.addCrowns(crownReward);
        }

        ZoneManager.markZoneForDeletion(zone, player);

        player.sendDelayedMessage(new NotificationMessage("This world is being deleted. Thank you for helping us free server storage. You are getting " + crownReward + "crowns as a reward.", NotificationType.POPUP), 3000);
    }
}
