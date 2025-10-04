package brainwine.gameserver.scrapmarket;

import brainwine.gameserver.dialog.Dialog;
import brainwine.gameserver.dialog.DialogListItem;
import brainwine.gameserver.dialog.DialogSection;
import brainwine.gameserver.dialog.DialogType;
import brainwine.gameserver.entity.npc.Npc;
import brainwine.gameserver.item.Item;
import brainwine.gameserver.item.ItemRegistry;
import brainwine.gameserver.player.Player;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.Optional;

public class ScrapMarketViewSession {
    private final ScrapMarket shop;
    private final Npc me;
    private final Player player;

    private static final Logger logger = LogManager.getLogger();

    private Optional<ScrapMarketProduct> currentProduct = Optional.empty();
    private Optional<String> action = Optional.empty();

    public ScrapMarketViewSession(ScrapMarket shop, Npc me, Player player) {
        this.shop = shop;
        this.me = me;
        this.player = player;
    }

    public void showNextDialog() {
        if(!player.isOnline() || me != null && (player.getZone() != me.getZone())) {
            return;
        }

        try {
            if(!currentProduct.isPresent() || !action.isPresent()) showSectionDialog();
            else {
                if("delete".equals(action.get())) {
                    confirmDelete();
                }

                else if("edit".equals(action.get())) {
                    confirmEdit();
                }

                else {
                    player.notify("Invalid action: " + action.get());
                }
            }
        } catch(Exception e) {
            player.notify("A problem occurred during your trade session.");
            logger.error("A problem occurred during an android trade session.", e);
        }
    }

    public void showSectionDialog() {
        Dialog dialog = new Dialog().setType(DialogType.ANDROID).setTitle(player.getName() + "'s Scrap Market Offering");

        dialog.addSection(new DialogSection().setText("To offer a new item, drag it onto a Bert android and click/tap on \"Offer this on the Scrap Market\""));
        if(shop.getProductsBySeller().containsKey(player.getDocumentId())) {
            for(ScrapMarketProduct product : shop.getProductsBySeller().get(player.getDocumentId()).values()) {
                Item item = ItemRegistry.getItem(product.getItemId());
                dialog.addSection(getProductSection1(product));
                dialog.addSection(new DialogSection().setText(String.format(
                        "%d for %d shilling%s, %d in stock",
                        product.getUnitQuantity(),
                        product.getPrice(),
                        product.getPrice() == 1 ? "" : "s",
                        product.getStock()
                )));
                dialog.addSection(new DialogSection().setText("Edit " + item.getTitle() + " Offering").setChoice("edit" + "." + product.getDialogId()));
                dialog.addSection(new DialogSection().setText("Take Down " + item.getTitle() + " Offering").setChoice("delete" + "." + product.getDialogId()));
            }
        }

        player.showDialog(dialog, ans -> {
            if(ans.length == 0 || "cancel".equals(ans[0]) || !(ans[0] instanceof String)) {
                return;
            }

            String choice = (String)ans[0];
            int dotIndex = choice.indexOf('.');
            action = Optional.of(choice.substring(0, dotIndex));
            String productDialogId = choice.substring(dotIndex + 1);
            String sellerId = ScrapMarketProduct.getSellerIdFromDialogId(productDialogId);
            String itemId = ScrapMarketProduct.getItemIdFromDialogId(productDialogId);
            if(shop.getProductsBySeller().containsKey(sellerId)) {
                ScrapMarketProduct product = shop.getProductsBySeller().get(sellerId).get(itemId);
                if(product != null) {
                    currentProduct = Optional.of(product);
                    showNextDialog();
                    return;
                }
            }

            player.notify("Cannot find the selected product.");
        });
    }

    public void confirmEdit() {
        ScrapMarketProduct product = currentProduct.get();
        Item item = ItemRegistry.getItem(product.getItemId());
        new ScrapMarketOfferSession(shop, player, item)
                .setUnitQuantityDefault(product.getUnitQuantity())
                .setPriceDefault(product.getPrice())
                .setStockDefault(product.getStock())
                .showNextDialog();
    }

    public void confirmDelete() {
        ScrapMarketProduct product = currentProduct.get();
        Item item = ItemRegistry.getItem(product.getItemId());
        Dialog dialog = new Dialog().setType(DialogType.ANDROID).setTitle("Taking Down Offer");

        dialog.addSection(getProductSection1(product));

        dialog.addSection(new DialogSection().setText(String.format("Are you sure you want to take down the offer on %s?", item.getTitle())));
        dialog.setActions("yesno");

        player.showDialog(dialog, ans -> {
            if(ans.length == 0 || ans[0] instanceof String && "yes".equalsIgnoreCase((String)ans[0])) {
                shop.removeProduct(product);
            }

            currentProduct = Optional.empty();
            action = Optional.empty();
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
}
