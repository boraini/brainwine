package brainwine.gameserver.command;

import brainwine.gameserver.GameServer;
import brainwine.gameserver.player.Player;
import brainwine.gameserver.zone.Block;
import brainwine.gameserver.zone.Zone;

// Don't change the order of owners, members, everyone
public enum CommandAccessLevel {
    OWNERS(true, false, false),
    MEMBERS(true, true, false),
    EVERYONE(true, true, true),
    NON_OWNERS(false, true, true),
    NON_MEMBERS(false, false, true),
    NO_ONE(false, false, false),
    ONLY_MEMBERS(false, true, false),
    OWNERS_AND_OTHERS(true, false, true);

    private boolean owners;
    private boolean members;
    private boolean others;

    CommandAccessLevel(boolean owners, boolean members, boolean others) {
        this.owners = owners;
        this.members = members;
        this.others = others;
    }

    public boolean isPrivileged(CommandExecutor executor, Zone zone) {
        if(executor == null || zone == null) return false;
        if(executor instanceof GameServer) return true;
        if(executor.isAdmin()) return true;

        Player player = (Player)executor;
        if(zone.isOwner(player)) return this.owners;
        else if(zone.isMember(player)) return this.members;
        else return this.others;

    }

    public boolean isPrivileged(Player player, Block block) {
        if(player.isGodMode()) return true;
        return player.getBlockHash() == block.getOwnerHash() ? this.owners : this.others;
    }
}
