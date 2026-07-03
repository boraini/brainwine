package brainwine.gameserver.command.competition;

import brainwine.gameserver.command.CommandExecutor;
import brainwine.gameserver.command.CommandInfo;
import brainwine.gameserver.item.ItemUseType;
import brainwine.gameserver.player.Player;
import brainwine.gameserver.zone.MetaBlock;
import brainwine.gameserver.zone.Zone;

@CommandInfo(name="/disownlandmarks", description="Disowns your landmarks and competition protectors")
public class DisownLandmarksCommand extends CompetitionCommand {

    @Override
    public void execute(Zone zone, Player player, String[] args) {
        if(args.length == 0 || !"yes".equals(args[0])) {
            player.notify("You will disown all your landmarks. To confirm, run /disownlandmarks yes");
        }

        for(MetaBlock mb : zone.getMetaBlocks()) {
            if(mb.getItem().hasUse(ItemUseType.LANDMARK)) {
                mb.clearOwner();
                zone.sendBlockMetaUpdate(mb);
            }
        }

        player.notify("Successfully disowned all your landmark plaques/competition protectors.");
    }

    @Override
    public String getUsage(CommandExecutor executor) {
        return "/disownlandmarks";
    }
}
