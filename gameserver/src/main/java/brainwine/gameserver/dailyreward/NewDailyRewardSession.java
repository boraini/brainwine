package brainwine.gameserver.dailyreward;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import brainwine.gameserver.GameServer;
import brainwine.gameserver.dailyreward.DailyRewardManager;
import brainwine.gameserver.dialog.Dialog;
import brainwine.gameserver.dialog.DialogSection;
import brainwine.gameserver.dialog.input.DialogTextInput;
import brainwine.gameserver.item.Item;
import brainwine.gameserver.item.ItemRegistry;
import brainwine.gameserver.player.Player;

public class NewDailyRewardSession {
    private static final int ITEMS_ASKED = 10;

    DailyRewardManager dailyRewardManager = GameServer.getInstance().getDailyRewardManager();
    private Player player;

    List<UnvalidatedItem> unvalidatedItems = new ArrayList<>();
    List<Object> unvalidatedLoot = new ArrayList<>();
    Object unvalidatedBeginsAt = OffsetDateTime.now().toString();
    Object unvalidatedEndsAt = unvalidatedBeginsAt;
    Object unvalidatedRequiredExperience = 0;

    public NewDailyRewardSession(Player player) {
        this.player = player;

        for(int i = 0; i < ITEMS_ASKED; i++) {
            unvalidatedItems.add(new UnvalidatedItem("", ""));
        }
        for(int i = 0; i < ITEMS_ASKED; i++) {
            unvalidatedLoot.add("");
        }
    }

    public void showNextDialog() {
        Dialog dialog;
        
        try {
            dialog = new Dialog().setTitle("Add New Daily Reward");

            dialog.addSection(makeDateInput(unvalidatedBeginsAt, "begins_at", "Begins At (Example: 2025-12-25T00:47)", player.isV3()));
            dialog.addSection(makeDateInput(unvalidatedEndsAt, "ends_at", "Ends At (Example: 2025-12-25T00:47)", player.isV3()));

            dialog.addSection(makeNumberInput(unvalidatedRequiredExperience, "required_experience", "Required Experience", player.isV3()));

            for(int i = 0; i < unvalidatedItems.size(); i++) {
                for(DialogSection section : makeItemInput(unvalidatedItems.get(i), i, player.isV3())) {
                    dialog.addSection(section);
                }
            }

            for(int i = 0; i < unvalidatedItems.size(); i++) {
                dialog.addSection(makeLootCategoryInput(unvalidatedLoot.get(i), i, player.isV3()));
            }
        } catch(Exception e) {
            player.notify("Error occurred while building dialog.");
            e.printStackTrace();
            return;
        }

        player.showDialog(dialog, ans -> {
            try {
                if(ans.length > 0 && "cancel".equals(ans[0])) {
                    return;
                }

                // Collect form values into a hashmap
                Map<String, Object> values = new HashMap<>();
                int ansIdx = 0;
                for(DialogSection section : dialog.getSections()) {
                    if(section.getInput() != null && section.getInput().getKey() != null) {
                        if(ansIdx >= ans.length) {
                            player.notify("Invalid input! Too many values.");
                            return;
                        }
                        values.put(section.getInput().getKey(), ans[ansIdx++]);
                    }
                }

                // Update saved form values
                for(Map.Entry<String, Object> entry : values.entrySet()) {
                    if(entry.getKey().startsWith("item_id_")) {
                        int index = Integer.parseInt(entry.getKey().substring("item_id_".length()));
                        if(index >= unvalidatedItems.size() || index < 0) {
                            player.notify("Item index out of bounds.");
                            showNextDialog();
                            return;
                        }
                        Object quantity = values.get("item_quantity_" + index);
                        if(quantity == null) {
                            player.notify("Item quantity not found in your input!");
                            showNextDialog();
                            return;
                        }
                        unvalidatedItems.set(index, new UnvalidatedItem(entry.getValue(), quantity));
                    }

                    if(entry.getKey().startsWith("loot_")) {
                        int index = Integer.parseInt(entry.getKey().substring("loot_".length()));
                        if(index >= unvalidatedLoot.size() || index < 0) {
                            player.notify("Loot category index out of bounds.");
                            showNextDialog();
                            return;
                        }
                        unvalidatedLoot.set(index, entry.getValue());
                    }

                    if(entry.getKey().equals("begins_at")) {
                        unvalidatedBeginsAt = entry.getValue();
                    }

                    if(entry.getKey().equals("ends_at")) {
                        unvalidatedEndsAt = entry.getValue();
                    }

                    if(entry.getKey().equals("required_experience")) {
                        unvalidatedRequiredExperience = entry.getValue();
                    }
                }

                // Validate in context
                OffsetDateTime beginsAt = OffsetDateTime.parse((String)unvalidatedBeginsAt);
                OffsetDateTime endsAt = OffsetDateTime.parse((String)unvalidatedEndsAt);
                int requiredExperience = Integer.parseInt((String)unvalidatedRequiredExperience);

                if(beginsAt.isAfter(endsAt)) {
                    player.notify("Ends At must be after Begins At!");
                    showNextDialog();
                    return;
                }

                for(UnvalidatedItem unvalidatedItem : unvalidatedItems) {
                    if(!unvalidatedItem.isBlank() && !unvalidatedItem.isValid()) {
                        player.notify("There were invalid items, but I didn't catch it.");
                        showNextDialog();
                        return;
                    }
                }

                for(Object lootCategory : unvalidatedLoot) {
                    if(lootCategory != null && !(lootCategory instanceof String)) {
                        player.notify("There were invalid loot categories, but I didn't catch it.");
                        showNextDialog();
                        return;
                    }
                }

                // Actually add it
                Map<Item, Integer> items = new HashMap<>();
                for(UnvalidatedItem unvalidatedItem : unvalidatedItems) {
                    if(!unvalidatedItem.isBlank() && unvalidatedItem.isValid()) {
                        items.put(unvalidatedItem.getValidatedItem(), unvalidatedItem.getValidatedQuantity());
                    }
                }
                List<String> lootCategories = new ArrayList<>();
                for(Object loot : unvalidatedLoot) {
                    if(loot instanceof String && !"".equals(loot)) {
                        lootCategories.add((String)loot);
                    }
                }

                dailyRewardManager.addReward(new DailyReward(beginsAt, endsAt, requiredExperience, items, lootCategories.toArray(new String[0])));
            } catch(Exception e) {
                player.notify(e.getMessage());
                showNextDialog();
            }
        });
    }

    public DialogSection makeDateInput(Object value, String key, String title, boolean v3) {
        DialogSection section = new DialogSection().setTitle(title).setInput(new DialogTextInput().setMaxLength(100).setKey(key).setValue(value.toString()));
        try {
            OffsetDateTime.parse((String)value);
        } catch(Exception e) {
            section.setText(String.format(v3 ? "<color=#f00>%s</color>" : "[ERROR] %s", key + " is of wrong data type!"));
        }
        return section;
    }

    public DialogSection makeNumberInput(Object value, String key, String title, boolean v3) {
        DialogSection section = new DialogSection().setTitle(title).setInput(new DialogTextInput().setMaxLength(100).setKey(key).setValue(value.toString()));
        if(!(value instanceof Number)) try {
            Integer.parseInt(value.toString());
        } catch(Exception e) {
            section.setText(String.format(v3 ? "<color=#f00>%s</color>" : "[ERROR] %s", key + " is of wrong data type!"));
        }
        return section;
    }

    public List<DialogSection> makeItemInput(UnvalidatedItem unvalidatedItem, int index, boolean v3) {
        DialogSection titleSection = new DialogSection();
        titleSection.setText(String.format(v3 ? "<color=#0ff>%s</color>" : "%s", "Item " + (index + 1)));

        DialogSection item = new DialogSection().setTitle("Item").setInput(new DialogTextInput().setMaxLength(100).setKey("item_id_" + index));
        item.getInput().setValue(unvalidatedItem.getRawItemId());
        if(unvalidatedItem.getValidationMessageForItem() != null) {
            item.setText(String.format(v3 ? "<color=#f00>%s</color>" : "[ERROR] %s", unvalidatedItem.getValidationMessageForItem()));
        }
        DialogSection quantity = new DialogSection().setTitle("Quantity").setInput(new DialogTextInput().setKey("item_quantity_" + index));
        quantity.getInput().setValue(unvalidatedItem.getRawQuantity() == null ? null : unvalidatedItem.getRawQuantity().toString());
        if(unvalidatedItem.getValidationMessageForQuantity() != null) {
            item.setText(String.format(v3 ? "<color=#f00>%s</color>" : "[ERROR] %s", unvalidatedItem.getValidationMessageForQuantity()));
        }

        return Stream.of(titleSection, item, quantity).collect(Collectors.toList());
    }

    public DialogSection makeLootCategoryInput(Object lootCategory, int index, boolean v3) {
        DialogSection section = new DialogSection().setTitle("Loot Category " + (index + 1)).setInput(new DialogTextInput().setMaxLength(100).setKey("loot_" + index));
        if (lootCategory != null && !(lootCategory instanceof String)) {
            section.setText(String.format(v3 ? "<color=#f00>%s</color>" : "[ERROR] %s", "Loot category is of wrong data type!"));
        }
        return section;
    }

    static class UnvalidatedItem {
        private Object itemId;
        private Object quantity;

        private String itemMessage;
        private String quantityMessage;

        public UnvalidatedItem(Object itemId, Object quantity) {
            this.itemId = itemId;
            this.quantity = quantity;
        }

        public boolean isBlank() {
            return "".equals(itemId) || "".equals(quantity);
        }

        public boolean isValid() {
            return !isBlank() && getValidationMessageForItem() == null && getValidationMessageForQuantity() == null;
        }

        public Object getRawItemId() {
            return itemId;
        }

        public Object getRawQuantity() {
            return quantity;
        }

        public Item getValidatedItem() {
            return itemId instanceof String ? ItemRegistry.getItem((String)itemId) : Item.AIR;
        }

        public int getValidatedQuantity() {
            if(quantity instanceof Number) return (int)quantity;
            return Integer.parseInt((String)quantity);
        }

        public String getValidationMessageForItem() {
            if("".equals(itemId)) return null;
            if(!(itemId instanceof String)) {
                return "Item id is of wrong data type!";
            }
            if(getValidatedItem().isAir()) {
                return "Item not found!";
            }

            return null;
        }

        public String getValidationMessageForQuantity() {
            if("".equals(quantity)) return null;
            try {
                getValidatedQuantity();
                return null;
            } catch(Exception e) {
                return e.getMessage();
            }
        }
    }
}
