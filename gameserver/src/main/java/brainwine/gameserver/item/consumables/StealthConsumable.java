package brainwine.gameserver.item.consumables;

import brainwine.gameserver.item.Item;
import brainwine.gameserver.player.Player;
import brainwine.gameserver.server.messages.InventoryMessage;

/**
 * Consumable handler for stealth cloaks
 */
public class StealthConsumable implements Consumable {

    @Override
    public void consume(Item item, Player player, Object details) {
        if("prosthetics".equals(item.getCategory()) && player.isMomentaryAccessoryOnCooldown(item)) {
            player.notify(String.format("You can't use your %s yet!", item.getTitle()));
            player.sendMessage(new InventoryMessage(player.getInventory().getClientConfig(item)));
            return;
        }
        if("consumables".equals(item.getCategory())) {
            player.getInventory().removeItem(item);
        }
        player.setStealth(true);
        float seconds = item.getPower();
        
        // Apply skill power bonus
        if(item.hasPowerBonus()) {
            seconds += player.getTotalSkillLevel(item.getPowerBonus().getFirst()) * item.getPowerBonus().getLast();
        }
        
        // Create timer
        long delay = (long)(seconds * 1000);
        player.addTimer("end stealth", delay, () -> player.setStealth(false));
    }
}
