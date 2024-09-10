package brainwine.gameserver.command.admin;

import static brainwine.gameserver.player.NotificationType.SYSTEM;

import brainwine.gameserver.GameServer;
import brainwine.gameserver.command.Command;
import brainwine.gameserver.command.CommandExecutor;
import brainwine.gameserver.command.CommandInfo;
import brainwine.gameserver.player.Player;
import brainwine.gameserver.zone.Zone;

@CommandInfo(name = "badapple", description = "Plays Bad Apple around the player.")
public class BadAppleCommand extends Command {
    
    @Override
    public void execute(CommandExecutor executor, String[] args) {
        if (GameServer.getInstance().getZoneManager().getBadAppleObject() == null) {
            executor.notify("badapple.json object is not loaded.", SYSTEM);
        }
        Zone zone = ((Player)executor).getZone();
                
        zone.startBadApple((Player)executor);
        executor.notify(String.format("Started playing Bad Apple in %s.", zone.getName()), SYSTEM);
    }
    
    @Override
    public String getUsage(CommandExecutor executor) {
        return "/badapple";
    }
    
    @Override
    public boolean canExecute(CommandExecutor executor) {
        return executor.isAdmin() && executor instanceof Player;
    }
}

