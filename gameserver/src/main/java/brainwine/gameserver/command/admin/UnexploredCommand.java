package brainwine.gameserver.command.admin;

import brainwine.gameserver.GameServer;
import brainwine.gameserver.command.Command;
import brainwine.gameserver.command.CommandExecutor;
import brainwine.gameserver.command.CommandInfo;
import brainwine.gameserver.player.NotificationType;
import brainwine.gameserver.zone.Zone;

@CommandInfo(name = "unexplored", description = "Lists the unexplored worlds currently tracked by the automatic world generator.")
public class UnexploredCommand extends Command {
    @Override
    public void execute(CommandExecutor executor, String[] args) {
        executor.notify("Here are the worlds currently awaiting exploration:", NotificationType.SYSTEM);
        for(String id : GameServer.getInstance().getZoneManager().getUnexploredZones()) {
            Zone zone = GameServer.getInstance().getZoneManager().getZone(id);
            if(zone != null) {
                executor.notify(zone.getName(), NotificationType.SYSTEM);
            }
        }
    }

    @Override
    public String getUsage(CommandExecutor executor) {
        return "/unexplored";
    }

    @Override
    public boolean canExecute(CommandExecutor executor) {
        return executor.isAdmin();
    }
}
