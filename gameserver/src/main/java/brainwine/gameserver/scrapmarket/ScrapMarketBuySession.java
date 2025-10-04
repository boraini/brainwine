package brainwine.gameserver.scrapmarket;

import brainwine.gameserver.GameServer;
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
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Consumer;

public class ScrapMarketBuySession {
    private final ScrapMarket shop;
    private final Npc me;
    private final Player player;
    private final Consumer<Boolean> onOutcome;
    private Optional<String> currentSection = Optional.empty();
    private Optional<ScrapMarketProduct> currentProduct = Optional.empty();
    private OptionalInt currentQuantity = OptionalInt.empty();

    private static final Logger logger = LogManager.getLogger();

    public ScrapMarketBuySession(ScrapMarket shop, Npc me, Player player) {
        this(shop, me, player, null);
    }

    public ScrapMarketBuySession(ScrapMarket shop, Npc me, Player player, Consumer<Boolean> onOutcome) {
        this.shop = shop;
        this.me = me;
        this.player = player;
        this.onOutcome = onOutcome;
    }

    private int getAdjustedPrice(ScrapMarketProduct product) {
        return product.getPrice();
    }

    private enum CanBuy {
        OK(true, "", "How many are you buying?"),
        NOT_ENOUGH_SHILLINGS(true, "", "Sorry, you don't have enough shillings to buy any of this item."),
        NOT_ENOUGH_IN_STOCK(true, "", "Sorry, but the seller does not have enough of this item in stock."),
        EXPIRED(true, "", "Sorry, but this offer is not available anymore."),
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

    private ScrapMarketBuySession.CanBuy canBuy(ScrapMarketProduct product) {
        return canBuy(product, 0);
    }

    private ScrapMarketBuySession.CanBuy canBuy(ScrapMarketProduct product, int quantity) {
        int account = player.getInventory().getQuantity(ItemRegistry.getItem("accessories/shillings"));
        int adjustedCost = getAdjustedPrice(product);

        if(!shop.getProducts().contains(product)) {
            return CanBuy.EXPIRED;
        } else if(adjustedCost * quantity > account || product.getUnitQuantity() * adjustedCost > account) {
            return CanBuy.NOT_ENOUGH_SHILLINGS;
        } else if(!product.inStock(quantity) || !product.availableInInventory(quantity)) {
            return CanBuy.NOT_ENOUGH_IN_STOCK;
        } else {
            return CanBuy.OK;
        }
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
        Dialog dialog = new Dialog().setType(DialogType.ANDROID).setTitle("Scrap Market");

        for(String tab : shop.getProductsByInventoryTab().keySet()) {
            if(player.isGodMode() || shop.getProductsByInventoryTab().get(tab).stream().anyMatch(p -> !player.getDocumentId().equals(p.getSellerId()))) {
                dialog.addSection(new DialogSection().setChoice(tab).setText(StringUtils.capitalize(tab)));
            }
        }

        dialog.setActions("Cancel");

        player.showDialog(dialog, ans -> {
            if(ans.length == 0 || !(ans[0] instanceof String) || "cancel".equals(ans[0])) {
                end(false);
                return;
            }

            String sectionKey = (String)ans[0];
            if(shop.getProductsByInventoryTab().containsKey(sectionKey)) {
                currentSection = Optional.of(sectionKey);
            }

            showNextDialog();
        });
    }

    public DialogSection getProductSection1(ScrapMarketProduct product) {
        Item item = ItemRegistry.getItem(product.getItemId());

        // This is eyeballed
        int spaces = (int)Math.max(0.0f, 1.5f * (18 - item.getTitle().length()));
        String padding = String.join("", Collections.nCopies(spaces, " "));
        return new DialogSection()
                .addItem(new DialogListItem().setItem(item.getCode()).setText(padding + item.getTitle()));
    }

    public DialogSection getProductSection2(ScrapMarketProduct product) {
        return new DialogSection().setText(ItemRegistry.getItem(product.getItemId()).getHint());
    }

    public void showSectionDialog() {
        LinkedHashSet<ScrapMarketProduct> products = shop.getProductsByInventoryTab().get(currentSection.get());

        Dialog dialog = new Dialog().setType(DialogType.ANDROID).setTitle(StringUtils.capitalize(currentSection.get()));

        for(ScrapMarketProduct product : products) {
            if(!player.isGodMode() && player.getDocumentId().equals(product.getSellerId())) continue;

            int adjustedCost = getAdjustedPrice(product);
            Player seller = GameServer.getInstance().getPlayerManager().getPlayerById(product.getSellerId());
            Item item = ItemRegistry.getItem(product.getItemId());

            CanBuy canBuy = canBuy(product);

            dialog.addSection(getProductSection1(product));
            dialog.addSection(getProductSection2(product));

            DialogSection buySection = new DialogSection()
                    .setChoice(product.getDialogId());

            boolean buttonReddened = canBuy != CanBuy.OK;
            String buttonMessage = String.format(
                    "Buy %s from %s | %d shilling%s for %s",
                    item.getTitle(),
                    seller.getName(),
                    adjustedCost,
                    adjustedCost == 1 ? "" : "s",
                    product.getUnitQuantity() == 1 ? "each" : Integer.toString(product.getUnitQuantity())
            );

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

            String sellerId = ScrapMarketProduct.getSellerIdFromDialogId((String)ans[0]);
            String itemId = ScrapMarketProduct.getItemIdFromDialogId((String)ans[0]);
            if(shop.getProductsBySeller().containsKey(sellerId) && shop.getProductsBySeller().get(sellerId).containsKey(itemId)) {
                currentProduct = Optional.of(shop.getProductsBySeller().get(sellerId).get(itemId));
            }

            showNextDialog();
        });
    }

    public void showQuantityDialog() {
        ScrapMarketProduct product = currentProduct.get();
        Player seller = GameServer.getInstance().getPlayerManager().getPlayerById(product.getSellerId());
        Item item = ItemRegistry.getItem(product.getItemId());
        CanBuy canBuy = canBuy(product);
        int adjustedCost = product.getPrice();

        Dialog dialog = new Dialog().setType(DialogType.ANDROID).setTitle("Buying " + item.getTitle());
        dialog.addSection(getProductSection1(product));
        dialog.addSection(getProductSection2(product));

        DialogSection buySection = new DialogSection();

        boolean buttonReddened = canBuy != CanBuy.OK;
        String buttonMessage = String.format(
                "%s sells %s of these for %d shilling%s.",
                seller.getName(),
                product.getUnitQuantity() == 1 ? "each" : Integer.toString(product.getUnitQuantity()),
                adjustedCost,
                adjustedCost == 1 ? "" : "s"
        );

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

        if(canBuy == CanBuy.OK) {
            int maxByPrice = player.getInventory().getQuantity(ItemRegistry.getItem("accessories/shillings")) / getAdjustedPrice(product);
            int maxByStock = product.getStock();
            dialog.addSection(TradeSession.Dialogs.createQuantitySelector(Math.min(maxByStock, maxByPrice), product.getUnitQuantity()).setTitle("How many are you buying?"));
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
        ScrapMarketProduct product = currentProduct.get();
        if(product == null) {
            end(false);
            return;
        }

        Item shillings = ItemRegistry.getItem("accessories/shillings");
        Item purchasedItem = ItemRegistry.getItem(product.getItemId());
        int buyQuantity = currentQuantity.getAsInt();
        int totalQuantity = buyQuantity;
        int totalPrice = totalQuantity * getAdjustedPrice(product);

        Dialog dialog = new Dialog().setType(DialogType.ANDROID).setTitle("Confirming Purchase");
        dialog.addSection(new DialogSection().setTitle("For your")
                .addItem(new DialogListItem().setItem(shillings.getCode()).setText(totalPrice + (totalPrice == 1 ? " shilling" : " shillings")))
        );
        dialog.addSection(new DialogSection().setTitle("you will get")
                .addItem(new DialogListItem().setItem(purchasedItem.getCode()).setText(purchasedItem.getTitle() + " x " + totalQuantity))
        );
        dialog.addSection(new DialogSection().setText("Do you accept?"));

        player.showDialog(dialog, ans -> {
            if(ans.length == 0 || !"cancel".equals(ans[0])) {
                CanBuy canBuy = canBuy(product, buyQuantity);
                if(canBuy == CanBuy.OK) {
                    Player seller = expectSeller(product);

                    if(!player.isGodMode()) {
                        seller.getInventory().addItem(shillings, totalPrice, true);
                        player.getInventory().removeItem(shillings, getAdjustedPrice(product), true);
                    }

                    product.purchase(player, buyQuantity);
                    if(product.getStock() <= 0) {
                        shop.removeProduct(product);
                    }

                    if(seller.isOnline()) {
                        seller.notify(String.format(
                                "%s has bought your %s %s on the Scrap Market for %d shillings.",
                                player.getName(),
                                totalQuantity == 1 ? "" : Integer.toString(totalQuantity),
                                purchasedItem.getTitle(),
                                totalPrice
                        ));
                    }

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

    private Player expectSeller(ScrapMarketProduct product) {
        Player seller = GameServer.getInstance().getPlayerManager().getPlayerById(product.getSellerId());
        if(seller == null) {
            throw new NoSuchElementException("Selling player does not exist anymore.");
        }

        return seller;
    }
}
