package brainwine.gameserver.chat;

import brainwine.gameserver.GameServer;
import brainwine.gameserver.item.DamageType;
import brainwine.gameserver.item.Item;
import brainwine.gameserver.player.Player;

public class PlayerProfanity {
    public static String filterAndPunish(Player player, String unfiltered) {
        if(player.isGodMode()) {
            return unfiltered;
        }

        String text = GameServer.getInstance().getProfanityManager().filter(unfiltered);
        if(!text.equals(unfiltered)) {
            punish(player);
        }

        return text;
    }

    public static void punish(Player player) {
        player.attack(null, Item.AIR, 0.5f, DamageType.ENERGY);
    }
}
