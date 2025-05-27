package brainwine.gameserver.entity.npc.job.jobs;

import brainwine.gameserver.GameConfiguration;
import brainwine.gameserver.dialog.Dialog;
import brainwine.gameserver.dialog.DialogHelper;
import brainwine.gameserver.dialog.DialogListItem;
import brainwine.gameserver.dialog.DialogSection;
import brainwine.gameserver.dialog.DialogType;
import brainwine.gameserver.entity.npc.Npc;
import brainwine.gameserver.entity.npc.job.DialoguerJob;
import brainwine.gameserver.item.Item;
import brainwine.gameserver.item.ItemRegistry;
import brainwine.gameserver.player.Player;
import brainwine.gameserver.player.Skill;
import brainwine.gameserver.player.TradeSession;
import brainwine.gameserver.util.MapHelper;

import java.util.HashMap;
import java.util.Map;

public class Trader extends DialoguerJob {
    Map<Player, Map<Item, Integer>> offers = new HashMap<>();
    @Override
    public DialogSection getMainDialogSection(Npc me, Player player) {
        return new DialogSection()
                .setText(MapHelper.getString(GameConfiguration.getBaseConfig(), "dialogs.android.trade"))
                .setChoice("trade");
    }

    @Override
    public boolean handleDialogAnswers(Npc me, Player player, Object[] ans) {
        if (ans.length >= 1 && "trade".equals(ans[0])) {
            player.showDialog(
                    DialogHelper.messageDialog(
                            me.getName(),
                            MapHelper.getString(GameConfiguration.getBaseConfig(), "dialogs.android.trade_response")
                    ).setType(DialogType.ANDROID)
            );
        }

        return true;
    }

    private String getItemTitle(Item item) {
        return item.getTitle() != null ? item.getTitle() : item.getId();
    }

    private void validateOffer(Player player, Map<Item, Integer> offer) {
        Map<Item, Integer> result = new HashMap<>();
        for(Map.Entry<Item, Integer> entry : offer.entrySet()) {
            result.put(entry.getKey(), Math.min(player.getInventory().getQuantity(entry.getKey()), entry.getValue()));
        }
        offer.clear();
        offer.putAll(result);
    }

    private int calculatePayback(Map<Item, Integer> offer) {
        int payback = 0;
        for(Map.Entry<Item, Integer> entry : offer.entrySet()) {
            payback += Math.max(0, entry.getValue() * entry.getKey().getShillingsPrice());
        }
        return payback;
    }

    @Override
    public void acceptItem(Npc me, Player player, Item item) {
        final Dialog dialog = new Dialog().setType(DialogType.ANDROID);

        String itemTitle = getItemTitle(item);
        String itemTitlePlural = itemTitle + (itemTitle.toLowerCase().endsWith("s") ? "es" : "s");
        int playerHas = player.getInventory().getQuantity(item);
        int barterSkill = player.getSkillLevel(Skill.BARTER);
        int price = item.getShillingsPrice();

        if(barterSkill < item.getBarterLevel()) {
            player.showDialog(DialogHelper
                    .messageDialog("Low Barter Skill", "Sorry, but I don't trust in the quality of your " + (playerHas == 1 ? itemTitle : itemTitlePlural) + ". Improve on your barter skills and come back.")
                    .addSection(new DialogSection().setText("You need at least barter level " + item.getBarterLevel() + "."))
                    .setType(DialogType.ANDROID)
            );
            return;
        }

        String header;
        if(price > 0) {
            header = "I buy " + itemTitlePlural + " for " + price + " shilling" + (price == 1 ? "" : "s") + " each.";
        } else if(price < -1) {
            header = "Sorry, there's nothing I can do with this item right now.";
        } else if(price < 0) {
            header = "Sorry but I don't know enough about this item to make an offer on it.";
        } else {
            header = "I'm not interested in your " + (playerHas == 1 ? itemTitle : itemTitlePlural) + " right now, but I can take them so you free up some space.";
        }

        dialog.addSection(new DialogSection().setText(header));

        // For -2 and lower it doesn't allow trading at all.
        if(price < -1) {
            player.showDialog(dialog);
            return;
        }

        dialog.addSection(TradeSession.Dialogs.createQuantitySelector(player, item).setText(price > 0 ? "How many are you selling?" : "How many are you giving?"));

        player.showDialog(dialog, ans -> {
            if(ans.length == 0) return;

            if(!(ans[0] instanceof String && "cancel".equals(ans[0]))) {
                int quantity = 0;
                try {
                    quantity = Integer.parseInt(ans[0].toString());
                } catch(NumberFormatException e) {
                    player.notify("There has been an error processing your input.");
                    return;
                }

                Map<Item, Integer> offer = offers.computeIfAbsent(player, p -> new HashMap<>());
                offer.put(item, quantity);
                // I tried to make implementing multi item trading easier later on.
                completeOrder(me, player);
            }
        });
    }

    public void completeOrder(Npc me, Player player) {
        Map<Item, Integer> offer = offers.computeIfAbsent(player, p -> new HashMap<>());
        validateOffer(player, offer);
        Item shillings = ItemRegistry.getItem("accessories/shillings");
        if(offer.isEmpty()) {
            player.showDialog(DialogHelper.messageDialog("No Offer", "Sorry, you haven't offered any items yet").setType(DialogType.ANDROID));
        } else {
            validateOffer(player, offer);
            int payback = calculatePayback(offer);
            DialogSection itemsSection = new DialogSection();
            for(Map.Entry<Item, Integer> entry : offer.entrySet()) {
                Item item = entry.getKey();
                int quantity = entry.getValue();
                itemsSection.addItem(new DialogListItem().setItem(item.getCode()).setText(getItemTitle(item) + " x " + quantity));
            }

            Dialog dialog = new Dialog().setType(DialogType.ANDROID).setTitle("Your Offer").setActions("yesno");
            if(payback > 0) {
                dialog.addSection(itemsSection.setTitle("For your"));
                dialog.addSection(new DialogSection().setTitle("I pay").addItem(new DialogListItem().setItem(shillings.getCode()).setText(payback + (payback == 1 ? " Shilling" : " Shillings"))));
            } else {
                dialog.addSection(itemsSection.setText("I will be taking your"));
            }
            dialog.addSection(new DialogSection().setText("Do you accept?"));

            player.showDialog(dialog, ans -> {
                if(!(ans.length == 0 || "cancel".equals(ans[0]))) {
                    validateOffer(player, offer);
                    int finalPayback = calculatePayback(offer);
                    for(Map.Entry<Item, Integer> entry : offer.entrySet()) {
                        player.getInventory().removeItem(entry.getKey(), entry.getValue(), true);
                    }
                    player.getInventory().addItem(shillings, finalPayback, true);
                    me.emote("Good trade.");
                }
            });
        }
        offers.remove(player);
    }
}
