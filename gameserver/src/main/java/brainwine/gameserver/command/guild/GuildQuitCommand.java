package brainwine.gameserver.command.guild;

import brainwine.gameserver.command.CommandExecutor;
import brainwine.gameserver.command.CommandInfo;
import brainwine.gameserver.dialog.Dialog;
import brainwine.gameserver.dialog.DialogSection;
import brainwine.gameserver.guild.Guild;
import brainwine.gameserver.player.Player;

@CommandInfo(name = "gquit", description = "Leave your guild.")
public class GuildQuitCommand extends GuildCommand {

    @Override
    public void execute(Player player, Guild guild, String[] args) {
        if(guild.isLeader(player.getDocumentId())) {
            player.notify("You'll need to designate a new guild leader before quitting.");
            return;
        }

        Dialog confirm = new Dialog()
                .setActions("yesno")
                .addSection(new DialogSection()
                        .setTitle("Leave Guild")
                        .setText(String.format("This will remove you from the %s guild. Are you sure?", guild.getName())));

        player.showDialog(confirm, data -> {
            if(data.length == 1 && "cancel".equals(data[0])) {
                return;
            }

            guild.removeMember(player, false);
        });
    }

    @Override
    public String getUsage(CommandExecutor executor) {
        return "/gquit";
    }
}
