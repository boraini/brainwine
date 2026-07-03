package brainwine.gameserver.command.competition;

import brainwine.gameserver.GameServer;
import brainwine.gameserver.command.CommandExecutor;
import brainwine.gameserver.command.CommandInfo;
import brainwine.gameserver.player.Player;
import brainwine.gameserver.zone.Zone;

import static brainwine.gameserver.player.NotificationType.SYSTEM;

@CommandInfo(name="compaddjudge", aliases = {"compoaddjudge", "competitionaddjudge"}, description="Adds a judge for the ongoing competition.")
public class CompetitionAddJudgeCommand extends CompetitionCommand {

    @Override
    public void execute(Zone zone, Player player, String[] args) {
        if(!zone.hasCompetition()) {
            player.notify("There is no competition ongoing in this world right now.", SYSTEM);
            return;
        }

        if(args.length < 1) {
            player.notify("Usage: " + getUsage(player), SYSTEM);
            return;
        }

        Player judge = GameServer.getInstance().getPlayerManager().getPlayer(args[0]);

        if (judge == null) {
            player.notify("Player " + args[0] + " not found.", SYSTEM);
            return;
        }

        if(zone.getCompetition().getJudges().contains(judge.getDocumentId())) {
            player.notify(judge.getName() + " is already a judge in this competition.", SYSTEM);
            return;
        }

        zone.getCompetition().addJudge(judge.getDocumentId());
        player.notify(judge.getName() + " has been added as a judge for this competition.");
    }

    @Override
    public String getUsage(CommandExecutor executor) {
        return "/compaddjudge <player name>";
    }
}
