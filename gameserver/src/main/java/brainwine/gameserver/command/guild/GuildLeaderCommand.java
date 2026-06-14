package brainwine.gameserver.command.guild;

import brainwine.gameserver.GameServer;
import brainwine.gameserver.command.CommandExecutor;
import brainwine.gameserver.command.CommandInfo;
import brainwine.gameserver.dialog.Dialog;
import brainwine.gameserver.dialog.DialogSection;
import brainwine.gameserver.guild.Guild;
import brainwine.gameserver.player.Player;
import brainwine.gameserver.player.PlayerManager;

@CommandInfo(name = "gleader", description = "Pass guild leadership to another member.")
public class GuildLeaderCommand extends GuildCommand {

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
            return;
        }

        if(invitee.getGuildId() != null && !guild.getId().equals(invitee.getGuildId())) {
            player.notify(String.format("Sorry, %s already belongs to a guild.", invitee.getName()));
            return;
        }

        if(!invitee.isOnline() || !guild.nearObelisk(player) || !guild.nearObelisk(invitee)) {
            player.notify(String.format("Please meet %s at the guild obelisk to pass leadership.", invitee.getName()));
            return;
        }

        // Confirm with the current leader before offering leadership
        Dialog confirm = new Dialog()
                .setActions("yesno")
                .addSection(new DialogSection()
                        .setTitle("Pass Leadership")
                        .setText(String.format("This will revoke your %s guild leadership and make you a guild member. Are you sure?", guild.getName())));

        player.showDialog(confirm, data -> {
            if(data.length == 1 && "cancel".equals(data[0])) {
                return;
            }

            guild.offerLeadership(invitee);
            player.notify(String.format("%s has been invited to LEAD the \"%s\" guild.", invitee.getName(), guild.getName()));
        });
    }

    @Override
    public String getUsage(CommandExecutor executor) {
        return "/gleader <player>";
    }
}
