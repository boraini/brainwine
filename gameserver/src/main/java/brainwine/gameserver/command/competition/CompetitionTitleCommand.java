package brainwine.gameserver.command.competition;

import brainwine.gameserver.command.CommandExecutor;
import brainwine.gameserver.command.CommandInfo;
import brainwine.gameserver.player.Player;
import brainwine.gameserver.zone.Zone;

import static brainwine.gameserver.player.NotificationType.SYSTEM;

@CommandInfo(name="comptitle", aliases = {"compotitle", "competitiontitle"}, description="Changes the title of the ongoing competition.")
public class CompetitionTitleCommand extends CompetitionCommand {

    @Override
    public void execute(Zone zone, Player player, String[] args) {
        if(!zone.hasCompetition()) {
            player.notify("There is no competition ongoing in this world right now.", SYSTEM);
            return;
        }

        String title = String.join(" ", args);
        if(title.trim().isEmpty()) {
            player.notify("Invalid title. Usage: " + getUsage(player), SYSTEM);
            return;
        }

        zone.getCompetition().setTitle(title);
    }

    @Override
    public String getUsage(CommandExecutor executor) {
        return "/comptitle <title>";
    }
}
