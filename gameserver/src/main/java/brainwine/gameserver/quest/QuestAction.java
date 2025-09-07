package brainwine.gameserver.quest;

import java.lang.IllegalArgumentException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import brainwine.gameserver.dialog.DialogSection;
import brainwine.gameserver.item.ItemRegistry;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

import brainwine.gameserver.dialog.DialogHelper;
import brainwine.gameserver.item.Item;
import brainwine.gameserver.player.Player;
import brainwine.gameserver.server.messages.EventMessage;
import brainwine.gameserver.server.messages.InventoryMessage;
import brainwine.shared.JsonHelper;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuestAction {
    public enum Type {
        INTERACT,
        BEGIN,
        DONE;
    }

    public enum Actor {
        PLAYER,
        ANDROID;
    }

    private Actor actor = Actor.PLAYER;

    private String method;
    
    private List<Object> params = new ArrayList<>(0);

    @JsonSetter("params")
    public void setParams(Object params) {
        if(params instanceof List) this.params = (List<Object>)params;
        else this.params.add(params);
    }

    public Actor getActor() {
        return actor;
    }

    public String getMethod() {
        return method;
    }

    public List<Object> getParams() {
        return params;
    }

    public String getCannotCancelReason(Player player) {
        switch(getMethod()) {
            case "gift_items!":
                try {
                    String reason = null;
                    for(Object object : getParams()) {
                        Map<String, Integer> items = JsonHelper.readValue(object, new TypeReference<Map<String, Integer>>() {});
                        for(String k : items.keySet()) {
                            Item item = ItemRegistry.getItem(k);
                            if(item.isAir()) continue;
                            if(!player.getInventory().hasItem(item, items.get(k))) {
                                if(reason == null) {
                                    reason = String.format("You need to give back my %d %s", items.get(k), item.getTitle());
                                } else {
                                    reason += String.format(", %d %s", items.get(k), item.getTitle());
                                }
                            }
                        }
                    }
                    return reason != null ? reason + "." : null;
                } catch(JsonProcessingException e) {
                    e.printStackTrace();
                    return "Exception occurred while checking if you can cancel this quest";
                }
            case "add_xp":
                return "I have given you some XP.";
            default:
                return null;
        }
    }

    public String getRevertImplicationsMessage() {
        switch(getMethod()) {
            case "gift_items!":
                try {
                    String reason = null;
                    for(Object object : getParams()) {
                        Map<String, Integer> items = JsonHelper.readValue(object, new TypeReference<Map<String, Integer>>() {});
                        for(String k : items.keySet()) {
                            Item item = ItemRegistry.getItem(k);
                            if(item.isAir()) continue;
                            if(reason == null) {
                                reason = String.format("You can cancel this quest but I'm going to have to take back my %d %s", items.get(k), item.getTitle());
                            } else {
                                reason += String.format(", %d %s", items.get(k), item.getTitle());
                            }
                        }
                    }
                    return reason != null ? reason + "." : null;
                } catch(JsonProcessingException e) {
                    e.printStackTrace();
                    return "I would tell you what will happen if you cancelled the quest but I encountered an error.";
                }
            default:
                return null;
        }
    }

    public DialogSection performAction(Player player, boolean preventMutations) {
        try{
            switch(getMethod()) {
                case "gift_items!":
                    if(preventMutations) break;
                    for(Object object : getParams()) {
                        Map<String, Integer> items = JsonHelper.readValue(object, new TypeReference<Map<String, Integer>>() {});
                        for(String k : items.keySet()) {
                            Item item = ItemRegistry.getItem(k);
                            if(item.isAir()) continue;
                            player.getInventory().addItem(item, items.get(k));
                            player.sendMessage(new InventoryMessage(player.getInventory().getClientConfig(item)));
                        }
                    }
                    break;
                case "event_message!":
                    if(params.size() < 2 || !(params.get(0) instanceof String)) {
                        throw new IllegalArgumentException();
                    }
                    player.sendMessage(new EventMessage((String) params.get(0), params.get(1)));
                    break;
                case "set_family_name!":
                    break;
                case "show_android_dialog":
                    String body = "No info.", title = null;
                    if(params.size() >= 1) {
                        if(!(params.get(0) instanceof String)) throw new IllegalArgumentException();
                        body = (String) params.get(0);
                    }

                    return new DialogSection().setText(body);
                case "add_xp":
                    if(preventMutations) break;
                    if(params.size() >= 1) {
                        int amount = JsonHelper.readValue(params.get(0), new TypeReference<Integer>() {});
                        player.addExperience(amount);
                    }
                    break;
                default:
                    player.notify(String.format("Unknown quest action %d", getMethod()));
            }
        } catch(JsonProcessingException e) {
            player.notify("Couldn't perform some actions for this quest due to JSON processing errors.");
        } catch(IllegalArgumentException e) {
            player.notify(String.format("Malformed quest action parameters for %d.", getMethod()));
        }
        return null;
    }

    public boolean revertAction(Player player) {
        switch(getMethod()) {
            case "gift_items!":
                try {
                    boolean success = true;
                    for(Object object : getParams()) {
                        Map<String, Integer> items = JsonHelper.readValue(object, new TypeReference<Map<String, Integer>>() {});
                        for(String k : items.keySet()) {
                            Item item = ItemRegistry.getItem(k);
                            if(item.isAir()) continue;
                            if(player.getInventory().hasItem(item, items.get(k))) {
                                player.getInventory().removeItem(item, items.get(k), true);
                            } else {
                                success = false;
                            }
                        }
                    }
                    return success;
                } catch(JsonProcessingException e) {
                    e.printStackTrace();
                    return false;
                }
            case "add_xp":
                return false;
            default:
                return true;
        }
    }
    
}
