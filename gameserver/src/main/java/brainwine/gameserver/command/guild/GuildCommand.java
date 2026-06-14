package brainwine.gameserver.command.guild;

import brainwine.gameserver.command.Command;
import brainwine.gameserver.command.CommandExecutor;
import brainwine.gameserver.guild.Guild;
import brainwine.gameserver.player.Player;

/**
 * Base class for guild console commands. Ensures the executor is a player who
 * belongs to a guild before dispatching, and provides leader/name validations.
 */
public abstract class GuildCommand extends Command {

    public abstract void execute(Player player, Guild guild, String[] args);

    @Override
    public void execute(CommandExecutor executor, String[] args) {
        Player player = (Player)executor;
        Guild guild = player.getGuild();

        if(guild == null) {
            player.notify("Sorry, you are not a member of a guild.");
            return;
        }

        execute(player, guild, args);
    }

    @Override
    public boolean canExecute(CommandExecutor executor) {
        return executor instanceof Player;
    }

    protected boolean requireLeader(Player player, Guild guild) {
        if(!guild.isLeader(player.getDocumentId())) {
            player.notify("Sorry, you are not a guild leader.");
            return false;
        }

        return true;
    }

    protected boolean requireNamesSet(Player player, Guild guild) {
        if(guild.getName() == null || guild.getName().isEmpty() || guild.getShortName() == null || guild.getShortName().isEmpty()) {
            player.notify("Please set your guild's name and shortname first.");
            return false;
        }

        return true;
    }
}
