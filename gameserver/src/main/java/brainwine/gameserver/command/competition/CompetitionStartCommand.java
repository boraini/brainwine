package brainwine.gameserver.command.competition;

import brainwine.gameserver.command.CommandExecutor;
import brainwine.gameserver.command.CommandInfo;
import brainwine.gameserver.competition.Competition;
import brainwine.gameserver.competition.ZoneCompetition;
import brainwine.gameserver.dialog.Dialog;
import brainwine.gameserver.dialog.DialogSection;
import brainwine.gameserver.player.Player;
import brainwine.gameserver.zone.Zone;

import static brainwine.gameserver.player.NotificationType.SYSTEM;

@CommandInfo(name="compstart", aliases = {"compostart", "competitionstart"}, description="Starts a competition in your world.")
public class CompetitionStartCommand extends CompetitionCommand{

    @Override
    public void execute(Zone zone, Player player, String[] args) {
        if (zone.hasCompetition()) {
            player.notify("There is already an ongoing competition titled " + zone.getCompetition().getTitle() + "! Use /compotitle to change its title.", SYSTEM);
            return;
        }

        String title = args.length > 0 ? String.join(" ", args) : zone.getName();
        if(title.trim().isEmpty()) {
            player.notify("Invalid title. Usage: " + getUsage(player), SYSTEM);
            return;
        }

        player.showDialog(new Dialog().setTitle("Starting Competition").addSection(new DialogSection().setText("You are about to start the competition \""+ args[0] + "\". Would you like to reset all the landmark plaques so people can vote later on?")).setActions("yesno"), ans -> {
            // Check again.
            if(zone.hasCompetition()) {
                player.notify("There is already an ongoing competition titled " + zone.getCompetition().getTitle() + "!");
                return;
            }

            if(ans.length == 0 || "yes".equals(ans[0])) {
                ZoneCompetition.clearLandmarkVotes(zone);
            }

            zone.setCompetition(new Competition(title));

            Dialog dialog = new Dialog().setTitle("Competition has started!").addSection(new DialogSection().setText("Competition " + title + " has started in your world " + zone.getName() + "."));

            if(zone.isProtected()) {
                String message = "Make sure to remove your world protection to let non-members start building, if needed!";
                dialog.addSection(new DialogSection().setText(player.isV3() ? "<color=#ff0000>" + message + "</color>" : message));
            }

            player.showDialog(dialog);
        });
    }

    @Override
    public String getUsage(CommandExecutor executor) {
        return "/compstart [title]";
    }
}
