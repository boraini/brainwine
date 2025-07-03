package brainwine.gameserver.server.requests;

import brainwine.gameserver.GameServer;
import brainwine.gameserver.command.CommandManager;
import brainwine.gameserver.item.DamageType;
import brainwine.gameserver.item.Item;
import brainwine.gameserver.player.NotificationType;
import brainwine.gameserver.player.Player;
import brainwine.gameserver.server.OptionalField;
import brainwine.gameserver.server.PlayerRequest;
import brainwine.gameserver.server.RequestInfo;

import java.util.Objects;

@RequestInfo(id = 13)
public class ChatRequest extends PlayerRequest {
    
    @OptionalField
    public String recipientName;
    public String text;
    
    @Override
    public void process(Player player) {
        if(text.startsWith(CommandManager.CUSTOM_COMMAND_PREFIX)) {
            CommandManager.executeCommand(player, text.substring(1));
            return;
        }
        
        if(player.isMuted()) {
            player.notify("You are currently muted. Your chat message was not sent.", NotificationType.SYSTEM);
            return;
        }

        String filteredText = text;
        if(!player.isGodMode()) {
            filteredText = GameServer.getInstance().getProfanityManager().filter(text);
            if(!Objects.equals(filteredText, text)) {
                player.attack(null, Item.AIR, 0.5f, DamageType.ENERGY);
            }
        }
        
        player.getZone().sendChatMessage(player, filteredText);
    }
}
