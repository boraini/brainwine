package brainwine.gameserver.command.guild;

import brainwine.gameserver.GameServer;
import brainwine.gameserver.command.CommandExecutor;
import brainwine.gameserver.command.CommandInfo;
import brainwine.gameserver.guild.Guild;
import brainwine.gameserver.player.Player;
import brainwine.gameserver.player.PlayerManager;

@CommandInfo(name = "ginvite", description = "Invite a player to your guild.")
public class GuildInviteCommand extends GuildCommand {

    @Override
    public void execute(Player player, Guild guild, String[] args) {
        if(!requireLeader(player, guild) || !requireNamesSet(player, guild) || !checkArgumentCount(player, args, 1)) {
            return;
        }

        String name = String.join(" ", args).trim();
        PlayerManager playerManager = GameServer.getInstance().getPlayerManager();
        Player invitee = playerManager.getPlayer(name);

        if(invitee == null) {
            player.notify(String.format("Player %s not found.", name));
        } else if(invitee.getGuildId() != null) {
            player.notify(String.format("Sorry, %s already belongs to a guild.", invitee.getName()));
        } else if(invitee.isOnline() && guild.nearObelisk(player) && guild.nearObelisk(invitee)) {
            guild.offerMembership(invitee);
            player.notify(String.format("%s has been invited to the \"%s\" guild.", invitee.getName(), guild.getName()));
        } else {
            player.notify(String.format("Please meet %s at the guild obelisk to invite them.", invitee.getName()));
        }
    }

    @Override
    public String getUsage(CommandExecutor executor) {
        return "/ginvite <player>";
    }
}
