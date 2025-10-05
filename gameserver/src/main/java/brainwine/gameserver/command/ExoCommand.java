package brainwine.gameserver.command;

import static brainwine.gameserver.player.NotificationType.SYSTEM;

import brainwine.gameserver.dialog.Dialog;
import brainwine.gameserver.dialog.DialogHelper;
import brainwine.gameserver.player.AppearanceSlot;
import brainwine.gameserver.player.Player;
import brainwine.gameserver.util.MapHelper;

@CommandInfo(name = "exo", description = "Lets you toggle the visibility of your exoskeleton parts.")
public class ExoCommand extends Command {
    @Override
    public void execute(CommandExecutor executor, String[] args) {

        if(!(executor instanceof Player)) {
            executor.notify("Only players can use this command.", SYSTEM);
        }
        Player player = (Player)executor;

        String headgearKey = "[" + AppearanceSlot.FACIAL_GEAR.getId() + "]";
        String torsoKey = "[" + AppearanceSlot.TOPS_OVERLAY.getId() + "]";
        String legsKey = "[" + AppearanceSlot.LEGS_OVERLAY.getId() + "]";

        Dialog dialog = DialogHelper.getDialog("exo");

        if(dialog.getSections().size() >= 4) {
            try {
                dialog.getSections().get(1).getInput().setValue(MapHelper.getBoolean(player.getAppearance(), headgearKey) ? "Visible" : "Hidden");
                dialog.getSections().get(2).getInput().setValue(MapHelper.getBoolean(player.getAppearance(), torsoKey) ? "Visible" : "Hidden");
                dialog.getSections().get(3).getInput().setValue(MapHelper.getBoolean(player.getAppearance(), legsKey) ? "Visible" : "Hidden");
            } catch(Exception e) {
                e.printStackTrace();
            }
        }

        player.showDialog(dialog, ans -> {
            if(ans.length < 3) return;

            player.updateAppearance(MapHelper.map(String.class, Object.class,
                    headgearKey, "Visible".equals(ans[0]),
                    torsoKey, "Visible".equals(ans[1]),
                    legsKey, "Visible".equals(ans[2])
            ));
        });
    }

    @Override
    public String getUsage(CommandExecutor executor) {
        return "/exo";
    }
}
