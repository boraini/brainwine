package brainwine.gameserver.command.competition;

import brainwine.gameserver.command.Command;
import brainwine.gameserver.command.CommandExecutor;
import brainwine.gameserver.player.Player;
import brainwine.gameserver.zone.Zone;

/**
 * Base class for commands regarding competition management.
 * Just like with world commands, the executor needs to own the zone.
 * This is still kept as a separate class for refactoring flexibility.
 */
public abstract class CompetitionCommand extends Command {
    
    public abstract void execute(Zone zone, Player player, String[] args);
    
    @Override
    public void execute(CommandExecutor executor, String[] args) {
        Player player = (Player)executor;
        Zone zone = player.getZone();
        
        // Check if player owns world
        if(!player.isGodMode() && !zone.isOwner(player)) {
            player.notify("Sorry, you do not own this world.");
            return;
        }
        
        execute(zone, player, args);
    }
    
    @Override
    public boolean canExecute(CommandExecutor executor) {
        if(!(executor instanceof Player)) return false;
        if(executor.isAdmin()) return true;

        Player player = (Player)executor;

        return player.getZone().isOwner(player);
    }
}
