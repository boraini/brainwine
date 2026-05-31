package brainwine.gameserver.entity.npc.job.jobs;

import brainwine.gameserver.GameConfiguration;
import brainwine.gameserver.androidshop.AndroidShop;
import brainwine.gameserver.androidshop.AndroidShopPerIpHistory;
import brainwine.gameserver.androidshop.AndroidShopSession;
import brainwine.gameserver.anticheat.IpAddressVsHardwareId;
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
import brainwine.gameserver.scrapmarket.ScrapMarket;
import brainwine.gameserver.scrapmarket.ScrapMarketOfferSession;
import brainwine.gameserver.scrapmarket.ScrapMarketSession;
import brainwine.gameserver.scrapmarket.SpecialRequestRegistry;
import brainwine.gameserver.scrapmarket.SpecialRequestSession;
import brainwine.gameserver.util.MapHelper;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class Trader extends DialoguerJob {
    Map<Player, Map<Item, Integer>> offers = new HashMap<>();
    @Override
    public List<DialogSection> getMainDialogSection(Npc me, Player player) {
        List<DialogSection> sections = new ArrayList<>(Arrays.asList(
                new DialogSection()
                    .setText(MapHelper.getString(GameConfiguration.getBaseConfig(), "dialogs.android.buy"))
                    .setChoice("buy"),
                new DialogSection()
                    .setText(MapHelper.getString(GameConfiguration.getBaseConfig(), "dialogs.android.sell"))
                    .setChoice("sell")
        ));

        // Add special requests option if trades are available
        if(!SpecialRequestRegistry.getInstance().getTrades().isEmpty()) {
            sections.add(new DialogSection()
                    .setText("I'm looking to trade some items. Interested?")
                    .setChoice("special_request"));
        }

        if(player.getTotalSkillLevel(Skill.BARTER) >= ScrapMarket.MIN_BARTER_LEVEL) {
            sections.add(new DialogSection()
                    .setText(MapHelper.getString(GameConfiguration.getBaseConfig(), "dialogs.android.scrap_market"))
                    .setChoice("scrap_market"));
        } else {
            sections.add(new DialogSection().setText(String.format(
                "You must be at least barter level %d to access the Scrap Market.",
                ScrapMarket.MIN_BARTER_LEVEL
            )));
        }

        return sections;
    }

    @Override
    public boolean handleDialogAnswers(Npc me, Player player, Object[] ans) {
        if (ans.length >= 1 && "sell".equals(ans[0])) {
            player.showDialog(
                    DialogHelper.messageDialog(
                            me.getName(),
                            MapHelper.getString(GameConfiguration.getBaseConfig(), "dialogs.android.sell_response")
                    ).setType(DialogType.ANDROID)
            );
        }

        if (ans.length >= 1 && "buy".equals(ans[0])) {
            new AndroidShopSession(AndroidShop.getInstance(), me, player).showNextDialog();
        }

        if (ans.length >= 1 && "special_request".equals(ans[0])) {
            new SpecialRequestSession(me, player).showNextDialog();
        }

        if (ans.length >= 1 && "scrap_market".equals(ans[0])) {
            new ScrapMarketSession(ScrapMarket.getInstance(), me, player).showNextDialog();
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

    private int calculatePayback(Player player, Map<Item, Integer> offer) {
        int payback = 0;
        for(Map.Entry<Item, Integer> entry : offer.entrySet()) {
            payback += Math.max(0, entry.getValue() * AndroidShop.getInstance().getAdjustments()
                    .getAdjustedBuyPrice(player, entry.getKey().getShillingsPrice())
            );
        }
        return payback;
    }

    private Dialog acceptItemLinkToScrapMarket(Dialog dialog) {
        dialog.addSection(new DialogSection().setText("If you'd like, you can list this on the scrap market in hopes of getting a better deal."));
        DialogSection section = new DialogSection().setText("Offer This on the Scrap Market").setChoice("scrap_market");
        dialog.addSection(section);
        return dialog;
    }

    private boolean acceptItemHandleScrapMarket(Npc me, Player player, Item item, Object[] ans) {
        if(ans.length > 0 && "scrap_market".equals(ans[0])) {
            new ScrapMarketOfferSession(ScrapMarket.getInstance(), player, item).showNextDialog();
            return true;
        }

        return false;
    }

    @Override
    public void acceptItem(Npc me, Player player, Item item) {
        player.getAndroidShopSellHistory().removeOldPurchases();
        AndroidShopPerIpHistory.getSaleInstance().removeOldPurchases(player);
        String itemTitle = getItemTitle(item);
        String itemTitlePlural = itemTitle + (itemTitle.toLowerCase().endsWith("s") ? "es" : "s");
        int barterSkill = player.getTotalSkillLevel(Skill.BARTER);
        int maxPrice = AndroidShop.getInstance().getAdjustments().getMaxPrice(player);
        int allowedDaily = item.getBarterPerDay();
        int playerHasTotal = player.getInventory().getQuantity(item);
        int playerHas = playerHasTotal;

        if(allowedDaily > 0) {
            int possible = Math.min(allowedDaily, playerHasTotal);
            int soFar = player.getAndroidShopSellHistory().getPurchases(item);
            soFar = Math.max(soFar, AndroidShopPerIpHistory.getSaleInstance().getPurchases(player, item));

            playerHas = possible - soFar;
        }

        if(player.isActionOnCooldown(IpAddressVsHardwareId.violationActionKey, IpAddressVsHardwareId.MIN_VIOLATIONS_INTERVAL, ChronoUnit.MILLIS)) {
            playerHas = 0;
        }

        Consumer<Object[]> scrapMarketOnlyHandler = ans -> acceptItemHandleScrapMarket(me, player, item, ans);

        if(playerHas == 0) {
            player.showDialog(acceptItemLinkToScrapMarket(DialogHelper
                    .messageDialog("Daily Limit Reached", "Sorry, but I won't buy any more of your " + item.getTitle() + " today. Come back tomorrow.")
                    .setType(DialogType.ANDROID)
            ), scrapMarketOnlyHandler);
            return;
        }

        if(item.getShillingsPrice() > maxPrice) {
            player.showDialog(acceptItemLinkToScrapMarket(DialogHelper
                    .messageDialog("Low Barter Skill", "Sorry, I don't think we can make a deal on this item right now. Work on your negotiating skills and come back later.")
                    .setType(DialogType.ANDROID)
            ), scrapMarketOnlyHandler);
            return;
        }

        if(barterSkill < item.getBarterLevel()) {
            player.showDialog(acceptItemLinkToScrapMarket(DialogHelper
                    .messageDialog("Low Barter Skill", "Sorry, but I don't trust in the quality of your " + (playerHas == 1 ? itemTitle : itemTitlePlural) + ". Improve on your barter skills and come back.")
                    .addSection(new DialogSection().setText("You need at least barter level " + item.getBarterLevel() + "."))
                    .setType(DialogType.ANDROID)
            ), scrapMarketOnlyHandler);
            return;
        }

        final Dialog dialog = new Dialog().setType(DialogType.ANDROID);
        if(item.getBarterMessage() != null) {
            dialog.addSection(new DialogSection().setText(item.getBarterMessage()));
        }

        String header;
        if(item.getShillingsPrice() <= -2) {
            header = "Sorry, there's nothing I can do with this item right now.";
        } else if(item.getShillingsPrice() == -1) {
            header = "Sorry but I don't know enough about this item to make an offer on it. I can take them from you to free up some space if you'd like.";
        } else if(item.getShillingsPrice() == 0) {
            header = "I'm not interested in your " + (playerHas == 1 ? itemTitle : itemTitlePlural) + " right now, but I can take them so you free up some space.";
        } else {
            int price = AndroidShop.getInstance().getAdjustments().getAdjustedBuyPrice(player, item.getShillingsPrice());
            header = "I buy " + itemTitlePlural + " for " + price + " shilling" + (price == 1 ? "" : "s") + " each.";
        }

        dialog.addSection(new DialogSection().setText(header));

        // For -2 and lower it doesn't allow trading at all.
        if(item.getShillingsPrice() < -1) {
            player.showDialog(dialog, scrapMarketOnlyHandler);
            return;
        }

        dialog.addSection(TradeSession.Dialogs.createQuantitySelector(player, item).setText(item.getShillingsPrice() > 0 ? "How many are you selling?" : "How many are you giving?"));

        if(barterSkill < 10) {
            dialog.addSection(new DialogSection().setText("When you reach barter level 10, you will also be able to list this on the Scrap Market and possibly get a better offer there."));
        } else {
            acceptItemLinkToScrapMarket(dialog);
        }

        player.showDialog(dialog, ans -> {
            if(acceptItemHandleScrapMarket(me, player, item, ans)) return;
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
            int payback = calculatePayback(player, offer);
            DialogSection itemsSection = new DialogSection();
            for(Map.Entry<Item, Integer> entry : offer.entrySet()) {
                Item item = entry.getKey();
                int quantity = entry.getValue();
                itemsSection.addItem(new DialogListItem().setItem(item.getCode()).setText(getItemTitle(item) + " x " + quantity));
            }

            Dialog dialog = new Dialog().setType(DialogType.ANDROID).setTitle("My Offer").setActions("yesno");
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
                    int totalQuantity = 0;
                    int finalPayback = calculatePayback(player, offer);
                    for(Map.Entry<Item, Integer> entry : offer.entrySet()) {
                        player.getInventory().removeItem(entry.getKey(), entry.getValue(), true);
                        totalQuantity += entry.getValue();
                        player.getAndroidShopSellHistory().recordPurchase(entry.getKey(), entry.getValue());
                        AndroidShopPerIpHistory.getSaleInstance().recordPurchase(player, entry.getKey(), entry.getValue());
                    }
                    player.getInventory().addItem(shillings, finalPayback, true);
                    player.getStatistics().trackAndroidShopSale(totalQuantity, finalPayback);
                    me.emote("Good trade.");
                }
            });
        }
        offers.remove(player);
    }
}
