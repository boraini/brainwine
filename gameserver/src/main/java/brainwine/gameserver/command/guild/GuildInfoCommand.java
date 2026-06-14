package brainwine.gameserver.command.guild;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import brainwine.gameserver.GameServer;
import brainwine.gameserver.command.CommandExecutor;
import brainwine.gameserver.command.CommandInfo;
import brainwine.gameserver.dialog.Dialog;
import brainwine.gameserver.dialog.DialogSection;
import brainwine.gameserver.guild.Guild;
import brainwine.gameserver.player.Player;
import brainwine.gameserver.player.PlayerManager;
import brainwine.gameserver.zone.Zone;
import brainwine.gameserver.zone.ZoneManager;

@CommandInfo(name = "ginfo", description = "Display information about your guild.")
public class GuildInfoCommand extends GuildCommand {

    @Override
    public void execute(Player player, Guild guild, String[] args) {
        PlayerManager playerManager = GameServer.getInstance().getPlayerManager();
        ZoneManager zoneManager = GameServer.getInstance().getZoneManager();

        Player leader = playerManager.getPlayerById(guild.getLeaderId());
        String leaderName = leader == null ? " " : leader.getName();

        Zone home = guild.getZoneId() == null ? null : zoneManager.getZone(guild.getZoneId());
        String zoneName = home == null ? "None" : home.getName();

        String guildName = "Unnamed";

        if(guild.getName() != null && guild.getShortName() != null) {
            guildName = String.format("%s [%s]", guild.getName(), guild.getShortName());
        }

        // List members other than the leader, alphabetically
        List<String> memberNames = guild.getMembers().stream()
                .map(playerManager::getPlayerById)
                .filter(Objects::nonNull)
                .map(Player::getName)
                .filter(name -> !name.equals(leaderName))
                .sorted()
                .collect(Collectors.toList());

        String members = memberNames.isEmpty() ? "none yet!" : String.join(", ", memberNames);

        Dialog dialog = new Dialog()
                .addSection(new DialogSection().setTitle(guildName))
                .addSection(new DialogSection().setTextColor("4d5b82").setText("Leader: " + leaderName))
                .addSection(new DialogSection().setTextColor("4d5b82").setText("Home World: " + zoneName))
                .addSection(new DialogSection().setText(" "))
                .addSection(new DialogSection().setTextColor("4d5b82").setText("Members"))
                .addSection(new DialogSection().setText(members));

        player.showDialog(dialog);
    }

    @Override
    public String getUsage(CommandExecutor executor) {
        return "/ginfo";
    }
}
