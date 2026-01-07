package brainwine.gameserver.command.admin;

import static brainwine.gameserver.player.NotificationType.SYSTEM;

import brainwine.gameserver.command.Command;
import brainwine.gameserver.command.CommandExecutor;
import brainwine.gameserver.command.CommandInfo;
import brainwine.gameserver.entity.EntityStatus;
import brainwine.gameserver.player.Player;
import brainwine.gameserver.server.messages.EntityStatusMessage;

@CommandInfo(name = "invisible", description = "Toggle invisibility to other players.")
public class InvisibleCommand extends Command {

    @Override
    public void execute(CommandExecutor executor, String[] args) {
        Player player = (Player) executor;
        boolean newInvisibleState = !player.isInvisible();

        player.setInvisible(newInvisibleState);

        if(newInvisibleState) {
            // Make the player disappear from other players' views
            player.sendMessageToPeers(new EntityStatusMessage(player, EntityStatus.EXITING));
            executor.notify("You are now invisible to other players.", SYSTEM);
        } else {
            // Make the player reappear to other players
            player.sendMessageToPeers(new EntityStatusMessage(player, EntityStatus.ENTERING));
            executor.notify("You are now visible to other players.", SYSTEM);
        }
    }

    @Override
    public String getUsage(CommandExecutor executor) {
        return "/invisible";
    }

    @Override
    public boolean canExecute(CommandExecutor executor) {
        return executor.isAdmin() && executor instanceof Player;
    }
}
