package brainwine.gameserver.command.guild;

import brainwine.gameserver.GameServer;
import brainwine.gameserver.command.CommandExecutor;
import brainwine.gameserver.command.CommandInfo;
import brainwine.gameserver.guild.Guild;
import brainwine.gameserver.player.Player;
import brainwine.gameserver.player.PlayerManager;

@CommandInfo(name = "gremove", description = "Remove a player from your guild.")
public class GuildRemoveCommand extends GuildCommand {

    @Override
    public void execute(Player player, Guild guild, String[] args) {
        if(!requireLeader(player, guild) || !checkArgumentCount(player, args, 1)) {
            return;
        }

        String name = String.join(" ", args).trim();

        if(name.equalsIgnoreCase(player.getName())) {
            player.notify("You cannot remove yourself, try a /gquit command.");
            return;
        }

        PlayerManager playerManager = GameServer.getInstance().getPlayerManager();
        Player target = playerManager.getPlayer(name);

        if(target == null) {
            player.notify(String.format("Player %s not found.", name));
        } else if(!guild.getId().equals(target.getGuildId())) {
            player.notify(String.format("Sorry, %s does not belong to your guild.", target.getName()));
        } else {
            guild.removeMember(target, true);
        }
    }

    @Override
    public String getUsage(CommandExecutor executor) {
        return "/gremove <player>";
    }
}
