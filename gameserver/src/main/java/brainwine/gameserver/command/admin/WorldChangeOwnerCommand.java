package brainwine.gameserver.command.admin;

import brainwine.gameserver.GameServer;
import brainwine.gameserver.command.Command;
import brainwine.gameserver.command.CommandExecutor;
import brainwine.gameserver.command.CommandInfo;
import brainwine.gameserver.player.Player;
import brainwine.gameserver.zone.Zone;

import static brainwine.gameserver.player.NotificationType.SYSTEM;

@CommandInfo(name = "wown", description = "Change the owner of the current world.", aliases = {"wowner", "wchown", "chown"})
public class WorldChangeOwnerCommand extends Command {
    @Override
    public void execute(CommandExecutor executor, String[] args) {
        if(!(executor instanceof Player)) {
            return;
        }

        if(!executor.isAdmin()) {
            executor.notify("You are not authorized to change world owners.", SYSTEM);
            return;
        }

        if(args.length < 1) {
            executor.notify("Usage: " + getUsage(executor), SYSTEM);
            return;
        }

        Player newOwner;
        if("(null)".equals(args[0])) {
            newOwner = null;
        } else {
            newOwner = GameServer.getInstance().getPlayerManager().getPlayer(args[0]);
            if(newOwner == null) {
                executor.notify("This player does not exist.", SYSTEM);
                return;
            }
        }

        Zone targetZone = ((Player)executor).getZone();
        if(targetZone == null) {
            executor.notify("Zone not found.", SYSTEM);
            return;
        }

        Player oldOwner = GameServer.getInstance().getPlayerManager().getPlayerById(targetZone.getOwner());

        if(oldOwner != null && oldOwner.isOnline()) {
            oldOwner.notify("You no longer own the world " + targetZone.getName() + ".");
        }

        if(newOwner != null && newOwner.isOnline()) {
            newOwner.notify("You now own the world " + targetZone.getName() + ".");
        }

        targetZone.setOwner(newOwner);
    }

    @Override
    public String getUsage(CommandExecutor executor) {
        return "/wown <player | (null)>";
    }

    @Override
    public boolean canExecute(CommandExecutor executor) {
        return executor.isAdmin() && executor instanceof Player;
    }
}
