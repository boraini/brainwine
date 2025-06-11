package brainwine.gameserver.entity.npc.job;

import brainwine.gameserver.entity.npc.Npc;
import brainwine.gameserver.item.Item;
import brainwine.gameserver.player.Player;

public abstract class Job {

    public abstract boolean dialogue(Npc me, Player player);

    public void acceptItem(Npc me, Player player, Item item) {}
}
