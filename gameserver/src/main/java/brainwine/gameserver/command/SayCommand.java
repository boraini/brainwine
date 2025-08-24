package brainwine.gameserver.command;

import static brainwine.gameserver.player.NotificationType.SYSTEM;

import brainwine.gameserver.chat.PlayerProfanity;
import brainwine.gameserver.player.ChatType;
import brainwine.gameserver.player.Player;

@CommandInfo(name = "say", description = "Shows a speech bubble to nearby players.")
public class SayCommand extends Command {

    @Override
    public void execute(CommandExecutor executor, String[] args) {
        if(args.length == 0) {
            executor.notify(String.format("Usage: %s", getUsage(executor)), SYSTEM);
            return;
        }
        
        Player player = ((Player)executor);
        
        if(player.isMuted()) {
            player.notify("You are currently muted. Your chat message was not sent.", SYSTEM);
            return;
        }
        
        String unfiltered = String.join(" ", args);
        String text = PlayerProfanity.filterAndPunish(player, unfiltered);

        player.getZone().sendChatMessage(player, text, ChatType.SPEECH);
    }
    
    @Override
    public String getUsage(CommandExecutor executor) {
        return "/say <message>";
    }
    
    @Override
    public boolean canExecute(CommandExecutor executor) {
        return executor instanceof Player;
    }
}
