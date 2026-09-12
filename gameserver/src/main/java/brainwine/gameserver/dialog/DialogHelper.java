package brainwine.gameserver.dialog;

import static brainwine.shared.LogMarkers.SERVER_MARKER;

import java.util.HashMap;
import java.util.Map;

import brainwine.gameserver.item.Item;
import brainwine.gameserver.player.Player;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;

import brainwine.gameserver.GameConfiguration;
import brainwine.gameserver.dialog.input.DialogInput;
import brainwine.gameserver.dialog.input.DialogTextInput;
import brainwine.gameserver.util.MapHelper;
import brainwine.shared.JsonHelper;

public class DialogHelper {
    
    private static final Logger logger = LogManager.getLogger();
    private static Map<String, Map<String, Object>> cache = new HashMap<>();
    
    public static Dialog getDialog(String name) {
        String path = String.format("dialogs.%s", name);
        Map<String, Object> config = getDialogConfig(path);
        
        if(config == null) {
            return messageDialog(String.format("Dialog '%s' does not exist.", path));
        }
        
        try {
            return JsonHelper.readValue(config, Dialog.class);
        } catch (JsonProcessingException e) {
            logger.error(SERVER_MARKER, "Failed to deserialize dialog: {}", path, e);
            return messageDialog(String.format("Deserialization for dialog '%s' failed: %s", path, e.getMessage()));
        }
    }
    
    public static Dialog getWardrobeDialog(String name) {
        String path = String.format("wardrobe_panel.dialogs.%s", name);
        Map<String, Object> config = getDialogConfig(path);
        
        if(config == null) {
            return messageDialog(String.format("Dialog '%s' does not exist.", path));
        }
        
        DialogInput input = null;
        
        try {
            input = JsonHelper.readValue(config, DialogInput.class);
        } catch (JsonProcessingException e) {
            logger.error(SERVER_MARKER, "Failed to deserialize dialog: {}", path, e);
            return messageDialog(String.format("Deserialization for dialog '%s' failed: %s", path, e.getMessage()));
        }
        
        return new Dialog()
                .setAlignment(DialogAlignment.LEFT)
                .setTarget("appearance")
                .addSection(new DialogSection()
                        .setInput(input));
    }
    
    public static Dialog messageDialog(String message) {
        return messageDialog("Attention", message);
    }
    
    public static Dialog messageDialog(String title, String message) {
        return new Dialog()
                .addSection(new DialogSection()
                        .setTitle(title)
                        .setText(message));
    }
    
    public static Dialog inputDialog(String message) {
        return inputDialog("Attention", message);
    }
    
    public static Dialog inputDialog(String title, String message) {
        return new Dialog()
                .addSection(new DialogSection()
                        .setTitle(title)
                        .setText(message)
                        .setInput(new DialogTextInput()
                                .setMaxLength(128)
                                .setKey("input")));
    }

    public static DialogSection itemListSection(Player player, String title, Map<Item, Integer> items) {
        DialogSection section = new DialogSection();
        if(title != null) {
            section.setText(title);
        }
        items.forEach((item, quantity) -> {
            if(player.hasClientVersion("3.13.8")) {
                section.addItem(new DialogListItem().setItem(item.getCode()).setText(String.format("%s x %s", item.getFancyTitle(), quantity)).setSupportRichText(true));
            } else {
                section.addItem(new DialogListItem().setItem(item.getCode()).setText(String.format("%s x %s", item.getTitle(), quantity)));
            }
        });

        return section;
    }
    
    private static Map<String, Object> getDialogConfig(String path) {
        Map<String, Object> config = null;
        
        if((config = cache.get(path)) == null) {
            config = MapHelper.getMap(GameConfiguration.getBaseConfig(), path);
            
            if(config != null) {
                cache.put(path, config);
            }
        }
        
        return config;
    }
}
