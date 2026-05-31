package brainwine.gameserver.androidshop;

import brainwine.gameserver.anticheat.IpAddressVsHardwareId;
import brainwine.gameserver.dialog.Dialog;
import brainwine.gameserver.dialog.DialogHelper;
import brainwine.gameserver.dialog.DialogListItem;
import brainwine.gameserver.dialog.DialogSection;
import brainwine.gameserver.dialog.DialogType;
import brainwine.gameserver.entity.npc.Npc;
import brainwine.gameserver.item.Item;
import brainwine.gameserver.item.ItemRegistry;
import brainwine.gameserver.player.Player;
import brainwine.gameserver.player.TradeSession;
import brainwine.gameserver.shop.ShopSection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Consumer;

public class AndroidShopSession {
    private final AndroidShop shop;
    private final Npc me;
    private final Player player;
    private final Consumer<Boolean> onOutcome;
    private Optional<String> currentSection = Optional.empty();
    private Optional<String> currentProduct = Optional.empty();
    private OptionalInt currentQuantity = OptionalInt.empty();

    private static final Logger logger = LogManager.getLogger();

    public AndroidShopSession(AndroidShop shop, Npc me, Player player) {
        this(shop, me, player, null);
    }

    public AndroidShopSession(AndroidShop shop, Npc me, Player player, Consumer<Boolean> onOutcome) {
        this.shop = shop;
        this.me = me;
        this.player = player;
        this.onOutcome = onOutcome;
        player.getAndroidShopBuyHistory().removeOldPurchases();
        AndroidShopPerIpHistory.getPurchaseInstance().removeOldPurchases(player);
    }

    private enum CanBuy {
        OK(true, "", "How many are you buying?"),
        TOO_HIGH_PRICE(false, "This item is too expensive for you.", "Sorry, but I believe that this item is too expensive for you. I won't even tell you the price."),
        NOT_ENOUGH_SHILLINGS(true, "", "Sorry, you don't have enough shillings to buy any of this item."),
        BOUGHT_TOO_FREQUENTLY(true, "", "Sorry, I don't have any more of that item at the moment. Come back tomorrow to see if I have more in stock!"),
        ;

        CanBuy(boolean showInShop, String buttonMessage, String dialogMessage) {
            this.showInShop = showInShop;
            this.buttonMessage = buttonMessage;
            this.dialogMessage = dialogMessage;
        }

        public final boolean showInShop;
        public final String buttonMessage;
        public final String dialogMessage;
    }

    private CanBuy canBuy(AndroidShopProduct product) {
        return canBuy(product, 0);
    }

    private CanBuy canBuy(AndroidShopProduct product, int quantity) {
        int maxPrice = shop.getAdjustments().getMaxPrice(player);
        int adjustedPrice = getAdjustedPrice(product);
        int account = player.getInventory().getQuantity(ItemRegistry.getItem("accessories/shillings"));
        int purchasedPlayer = player.getAndroidShopBuyHistory().getPurchases(product.getItem());
        int purchasedIp = AndroidShopPerIpHistory.getPurchaseInstance().getPurchases(player, product.getItem());
        int purchased = Math.max(purchasedPlayer, purchasedIp);

        if (adjustedPrice > maxPrice) {
            return CanBuy.TOO_HIGH_PRICE;
        } else if(adjustedPrice > account || adjustedPrice * quantity > account) {
            return CanBuy.NOT_ENOUGH_SHILLINGS;
        } else if(purchased >= product.getMaxQuantityPerDay() || purchased + quantity > product.getMaxQuantityPerDay()) {
            return CanBuy.BOUGHT_TOO_FREQUENTLY;
        } else {
            return CanBuy.OK;
        }
    }

    private int getAdjustedPrice(AndroidShopProduct product) {
        return shop.getAdjustments().getAdjustedSellPrice(player, product.getPrice());
    }

    public void showNextDialog() {
        if(!player.isOnline() || me != null && (player.getZone() != me.getZone())) {
            end(false);
            return;
        }

        try {
            if(!currentSection.isPresent()) showAllSectionsDialog();
            else if(!currentProduct.isPresent()) showSectionDialog();
            else if(!currentQuantity.isPresent()) showQuantityDialog();
            else showConfirmationDialog();
        } catch(Exception e) {
            player.notify("A problem occurred during your trade session.");
            logger.error("A problem occurred during an android trade session.", e);
        }
    }

    public void showAllSectionsDialog() {
        Dialog dialog = new Dialog().setType(DialogType.ANDROID).setTitle(me != null ? me.getName() + "'s Wares" : "Android Shop");
        dialog.addSection(new DialogSection().setText("I have items in different categories to offer. Click or tap on one to proceed!"));

        for(Map.Entry<String, ShopSection> sectionEntry : shop.getSections().entrySet()) {
            String key = sectionEntry.getKey();
            ShopSection section = sectionEntry.getValue();

            dialog.addSection(new DialogSection().setChoice(key).setText(section.getName()));
        }

        dialog.setActions("Cancel");

        player.showDialog(dialog, ans -> {
            if(ans.length == 0 || !(ans[0] instanceof String) || "cancel".equals(ans[0])) {
                end(false);
                return;
            }

            String sectionKey = (String)ans[0];
            if(shop.getSections().containsKey(sectionKey)) {
                currentSection = Optional.of(sectionKey);
            }

            showNextDialog();
        });
    }

    public DialogSection getProductSection1(AndroidShopProduct product) {
        Item item = product.getItem();

        // This is eyeballed
        int spaces = (int)Math.max(0.0f, 1.5f * (18 - item.getTitle().length()));
        String padding = String.join("", Collections.nCopies(spaces, " "));
        return new DialogSection()
            .addItem(new DialogListItem().setItem(item.getCode()).setText(padding + item.getTitle()));
    }

    public DialogSection getProductSection2(AndroidShopProduct product) {
        return new DialogSection().setText(product.getItem().getHint());
    }

    public void showSectionDialog() {
        ShopSection shopSection = shop.getSections().get(currentSection.get());

        Dialog dialog = new Dialog().setType(DialogType.ANDROID).setTitle(shopSection.getName());

        for(String productId : shopSection.getProducts()) {
            AndroidShopProduct product = shop.getProducts().get(productId);
            CanBuy canBuy = canBuy(product);
            int adjustedCost = getAdjustedPrice(product);

            // Do not show items if the player is not worth them anyway.
            if(!canBuy.showInShop) continue;

            dialog.addSection(getProductSection1(product));
            dialog.addSection(getProductSection2(product));

            DialogSection buySection = new DialogSection()
                    .setChoice(productId);

            boolean buttonReddened = canBuy != CanBuy.OK;
            String buttonMessage = canBuy == CanBuy.TOO_HIGH_PRICE
                    ? canBuy.buttonMessage
                    : String.format("Buy %s | %d shilling%s each", product.getItem().getTitle(), adjustedCost, adjustedCost == 1 ? "" : "s");

            if(buttonReddened) {
                if(player.isV3()) {
                    buySection.setText("<color=#ff8844>" + buttonMessage + "</color>");
                } else {
                    buySection.setText(buttonMessage).setTextColor("ff8844");
                }
            } else {
                buySection.setText(buttonMessage);
            }

            dialog.addSection(buySection);
        }

        dialog.setActions("Back");

        player.showDialog(dialog, ans -> {
            if(ans.length == 0 || !(ans[0] instanceof String)) {
                end(false);
                return;
            }

            if("cancel".equals(ans[0])) {
                currentSection = Optional.empty();
                showNextDialog();
                return;
            }

            String productId = (String)ans[0];
            if(shop.getProducts().containsKey(productId)) {
                currentProduct = Optional.of(productId);
            }

            showNextDialog();
        });
    }

    public void showQuantityDialog() {
        AndroidShopProduct product = shop.getProducts().get(currentProduct.get());
        CanBuy canBuy = canBuy(product);
        int adjustedCost = getAdjustedPrice(product);

        Dialog dialog = new Dialog().setType(DialogType.ANDROID).setTitle("Buying " + product.getItem().getTitle());
        dialog.addSection(getProductSection1(product));
        dialog.addSection(getProductSection2(product));

        if(canBuy != CanBuy.TOO_HIGH_PRICE) {
            DialogSection buySection = new DialogSection();

            boolean buttonReddened = canBuy != CanBuy.OK;
            String buttonMessage = String.format("I sell these for %d shilling%s each.", adjustedCost, adjustedCost == 1 ? "" : "s");

            if(buttonReddened) {
                if(player.isV3()) {
                    buySection.setText("<color=#ff8844>" + buttonMessage + "</color>");
                } else {
                    buySection.setText(buttonMessage).setTextColor("ff8844");
                }
            } else {
                buySection.setText(buttonMessage);
            }

            dialog.addSection(buySection);
        }

        if(canBuy == CanBuy.OK) {
            int allowedByPrice = player.getInventory().getQuantity(ItemRegistry.getItem("accessories/shillings")) / getAdjustedPrice(product);
            int purchased = player.getAndroidShopBuyHistory().getPurchases(product.getItem());
            int maxQuantity = Math.min(allowedByPrice, product.getMaxQuantityPerDay() - purchased);
            dialog.addSection(TradeSession.Dialogs.createQuantitySelector(maxQuantity).setTitle("How many are you buying?"));
        } else {
            dialog.addSection(new DialogSection().setText(canBuy.dialogMessage));
        }

        player.showDialog(dialog, ans -> {
            if(ans.length == 0) {
                return;
            }

            if("cancel".equals(ans[0])) {
                currentProduct = Optional.empty();
                showNextDialog();
                return;
            }

            try {
                int quantity = Integer.parseInt(ans[0].toString());
                currentQuantity = OptionalInt.of(quantity);
            } catch(NumberFormatException ignored) {}

            showNextDialog();
        });
    }

    public void showConfirmationDialog() {
        String productId = currentProduct.get();
        AndroidShopProduct product = shop.getProducts().get(productId);
        if(product == null) {
            end(false);
            return;
        }

        Item shillings = ItemRegistry.getItem("accessories/shillings");
        Item purchasedItem = ItemRegistry.getItem(productId);
        int quantity = currentQuantity.getAsInt();
        int totalPrice = quantity * shop.getAdjustments().getAdjustedSellPrice(player, product.getPrice());

        Dialog dialog = new Dialog().setType(DialogType.ANDROID).setTitle("Confirming Purchase");
        dialog.addSection(new DialogSection().setTitle("For your")
                .addItem(new DialogListItem().setItem(shillings.getCode()).setText(totalPrice + (totalPrice == 1 ? " shilling" : " shillings")))
        );
        dialog.addSection(new DialogSection().setTitle("you will get")
                .addItem(new DialogListItem().setItem(purchasedItem.getCode()).setText(purchasedItem.getTitle() + " x " + quantity))
        );
        dialog.addSection(new DialogSection().setText("Do you accept?"));

        player.showDialog(dialog, ans -> {
            if(ans.length == 0 || !"cancel".equals(ans[0])) {
                CanBuy canBuy = canBuy(product, quantity);
                if(canBuy == CanBuy.OK) {
                    if(player.isActionOnCooldown(IpAddressVsHardwareId.violationActionKey, IpAddressVsHardwareId.MIN_VIOLATIONS_INTERVAL, ChronoUnit.MILLIS)) {
                        player.notify("Sorry, an error occurred. Please try again later");
                        return;
                    }
                    int currentTotalPrice = quantity * getAdjustedPrice(product);
                    player.getInventory().removeItem(shillings, currentTotalPrice, true);
                    product.purchase(player, quantity);
                    player.getAndroidShopBuyHistory().recordPurchase(product.getItem(), quantity);
                    AndroidShopPerIpHistory.getPurchaseInstance().recordPurchase(player, product.getItem(), quantity);
                    player.getStatistics().trackAndroidShopPurchase(quantity, currentTotalPrice);
                    if(me != null) me.emote("Good trade!");
                    end(true);
                } else {
                    player.showDialog(DialogHelper.messageDialog(canBuy.dialogMessage).setType(DialogType.ANDROID));
                    end(false);
                }
            } else {
                currentQuantity = OptionalInt.empty();
                showNextDialog();
            }
        });
    }

    public void end(boolean outcome) {
        if(onOutcome != null) onOutcome.accept(outcome);
    }
}
