package brainwine.gameserver.command;

import static brainwine.gameserver.player.NotificationType.SYSTEM;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import brainwine.gameserver.dialog.Dialog;
import brainwine.gameserver.dialog.DialogHelper;
import brainwine.gameserver.dialog.DialogSection;
import brainwine.gameserver.dialog.input.DialogSelectInput;
import brainwine.gameserver.item.Item;
import brainwine.gameserver.player.AppearanceSlot;
import brainwine.gameserver.player.Player;
import brainwine.gameserver.util.MapHelper;

@CommandInfo(name = "exo", description = "Lets you toggle the visibility of your exoskeleton parts.")
public class ExoCommand extends Command {
    public enum Mode {
        SIMPLE,
        ADVANCED,
    }

    public Mode chooseMode(CommandExecutor executor) {
        return Mode.SIMPLE; // TODO it should be based on which type of accessory slot is used
    }

    @Override
    public void execute(CommandExecutor executor, String[] args) {
        Mode mode = null;
        if(args.length == 0) {
            mode = chooseMode(executor);
        } else {
            if(args[0].startsWith("s")) mode = Mode.SIMPLE;
            if(args[0].startsWith("a")) mode = Mode.ADVANCED;
        }

        if(mode == null) {
            executor.notify("First argument needs to start with s (for simple) or a (for advanced).", SYSTEM);
            return;
        }

        if(mode == Mode.SIMPLE) executeSimple(executor, args);
        if(mode == Mode.ADVANCED) executeAdvanced(executor, args);
    }

    public void executeSimple(CommandExecutor executor, String[] args) {
        Player player = (Player)executor;

        String headgearKey = AppearanceSlot.FACIAL_GEAR.getId();
        String torsoKey = AppearanceSlot.TOPS_OVERLAY.getId();
        String legsKey = AppearanceSlot.LEGS_OVERLAY.getId();

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

    public void executeAdvanced(CommandExecutor executor, String[] args) {
        Player player = (Player)executor;
        
        // TODO: text index would be far more appropriate for this
        Map<String, Integer> headgearKeys = new HashMap<>();
        Map<String, Integer> torsoKeys = new HashMap<>();
        Map<String, Integer> legsKeys = new HashMap<>();
        Dialog dialog = new Dialog()
                .addSection(new DialogSection().setTitle("Exo Visibility"))
                .addSection(createSlotSection(player, headgearKeys, AppearanceSlot.FACIAL_GEAR, "Headset", "headset"))
                .addSection(createSlotSection(player, torsoKeys, AppearanceSlot.TOPS_OVERLAY, "Torso", "torso"))
                .addSection(createSlotSection(player, legsKeys, AppearanceSlot.LEGS_OVERLAY, "Legs", "legs"));
        
       player.showDialog(dialog, data -> {
           // Handle cancellation
           if(data.length == 1 && data[0].equals("cancel")) {
               return;
           }
           
           // Check data length
           if(data.length != 3) {
               return;
           }
           
           // Update player appearance
           // TODO: Hiding exoskeletons is not implemented properly on v3 clients.
           // Players will need to relog in order for changes to apply properly.
           Map<String, Object> appearance = new HashMap<>();
           appearance.put(AppearanceSlot.FACIAL_GEAR.getId(), headgearKeys.getOrDefault(String.valueOf(data[0]), 0));
           appearance.put(AppearanceSlot.TOPS_OVERLAY.getId(), torsoKeys.getOrDefault(String.valueOf(data[1]), 0));
           appearance.put(AppearanceSlot.LEGS_OVERLAY.getId(), legsKeys.getOrDefault(String.valueOf(data[2]), 0));
           player.updateAppearance(appearance);
       });
    }

    @Override
    public String getUsage(CommandExecutor executor) {
        return "/exo";
    }

    @Override
    public boolean canExecute(CommandExecutor executor) {
        return executor instanceof Player;
    }
    
    private static DialogSection createSlotSection(Player player, Map<String, Integer> keyMap, AppearanceSlot slot, String text, String key) {
        List<String> options = new ArrayList<>();
        
        for(Item item : player.getInventory().getAccessories()) {
            if(item.getAppearanceSlot() == slot) {
                String option = item.getTitle().split(" ", 2)[0];
                options.add(option);
                keyMap.put(option, item.getCode());
            }
        }
        
        // TODO options are sorted inversely by item code but should ideally be sorted by some kind of arbitrary tier
        options.sort((a, b) -> Integer.compare(keyMap.get(b), keyMap.get(a)));
        options.add("Hidden");
        return new DialogSection().setText(text).setInput(new DialogSelectInput().setOptions(options).setKey(key));
    }
}
