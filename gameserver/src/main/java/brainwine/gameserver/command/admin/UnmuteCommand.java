package brainwine.gameserver.command.admin;

import static brainwine.gameserver.player.NotificationType.SYSTEM;

import brainwine.gameserver.GameServer;
import brainwine.gameserver.command.Command;
import brainwine.gameserver.command.CommandExecutor;
import brainwine.gameserver.command.CommandInfo;
import brainwine.gameserver.player.Player;

@CommandInfo(name = "unmute", description = "Unmutes a player.")
public class UnmuteCommand extends Command {

    @Override
    public void execute(CommandExecutor executor, String[] args) {
        if(args.length < 1) {
            executor.notify(String.format("Usage: %s", getUsage(executor)), SYSTEM);
            return;
        }
        
        Player target = GameServer.getInstance().getPlayerManager().getPlayer(args[0]);
        
        if(target == null) {
            executor.notify("This player does not exist.", SYSTEM);
            return;
        }
        
        if(!target.isMuted()) {
            executor.notify(String.format("%s isn't currently muted.", target.getName()), SYSTEM);
            return;
        }
        
        target.unmute(executor instanceof Player ? (Player)executor : null);
        executor.notify(String.format("Player %s has been unmuted.", target.getName()), SYSTEM);
    }
    
    @Override
    public String getUsage(CommandExecutor executor) {
        return "/unmute <player>";
    }
    
    @Override
    public boolean canExecute(CommandExecutor executor) {
        return executor.isAdmin();
    }
}