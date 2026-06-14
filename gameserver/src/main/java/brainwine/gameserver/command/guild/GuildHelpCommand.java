package brainwine.gameserver.command.guild;

import brainwine.gameserver.command.CommandExecutor;
import brainwine.gameserver.command.CommandInfo;
import brainwine.gameserver.dialog.DialogHelper;
import brainwine.gameserver.guild.Guild;
import brainwine.gameserver.player.Player;

@CommandInfo(name = "ghelp", description = "Show the guild command help screen.")
public class GuildHelpCommand extends GuildCommand {

    @Override
    public void execute(Player player, Guild guild, String[] args) {
        String dialog = guild.isLeader(player.getDocumentId()) ? "guild_owner_help" : "guild_member_help";
        player.showDialog(DialogHelper.getDialog(dialog));
    }

    @Override
    public String getUsage(CommandExecutor executor) {
        return "/ghelp";
    }
}
