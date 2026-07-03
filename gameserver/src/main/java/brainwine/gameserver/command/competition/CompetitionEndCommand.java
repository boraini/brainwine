package brainwine.gameserver.command.competition;

import brainwine.gameserver.command.CommandExecutor;
import brainwine.gameserver.command.CommandInfo;
import brainwine.gameserver.competition.CompetitionPhase;
import brainwine.gameserver.dialog.Dialog;
import brainwine.gameserver.dialog.DialogSection;
import brainwine.gameserver.player.Player;
import brainwine.gameserver.zone.Zone;

import static brainwine.gameserver.player.NotificationType.SYSTEM;

@CommandInfo(name="compend", aliases = {"compoend", "competitionend"}, description="Ends the ongoing competition.")
public class CompetitionEndCommand extends CompetitionCommand {
    @Override
    public void execute(Zone zone, Player player, String[] args) {
        if (!zone.hasCompetition() || zone.getCompetition().getPhase() != CompetitionPhase.FINISHED) {
            player.notify("This world has no ongoing competition right now.", SYSTEM);
            return;
        }

        if (zone.getCompetition().getPhase() == CompetitionPhase.FINISHED) {
            confirm(player, zone);
        } else {
            player.showDialog(
                    new Dialog().setTitle("Ending Competition")
                        .addSection(new DialogSection().setText("Are you sure you would like to end this competition? It is still " + (zone.getCompetition().getPhase() == CompetitionPhase.ACTIVE ? "active." : "in the " + zone.getCompetition().getPhase().name().toLowerCase() + " phase.")))
                        .addSection(new DialogSection().setText("Any current votes will be lost even if you start a new competition."))
                        .setActions("yesno"),
                    ans -> {
                        if (ans.length == 0 || "yes".equals(ans[0])) {
                            confirm(player, zone);
                        }
                    }
            );
        }
    }

    public void confirm(Player player, Zone zone) {
        zone.setCompetition(null);
    }

    @Override
    public String getUsage(CommandExecutor executor) {
        return "/compend";
    }
}
