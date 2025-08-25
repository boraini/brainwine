package brainwine.gameserver.scrapmarket;

import brainwine.gameserver.dialog.Dialog;
import brainwine.gameserver.dialog.DialogHelper;
import brainwine.gameserver.dialog.DialogListItem;
import brainwine.gameserver.dialog.DialogSection;
import brainwine.gameserver.dialog.DialogType;
import brainwine.gameserver.dialog.input.DialogTextInput;
import brainwine.gameserver.item.Item;
import brainwine.gameserver.player.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ScrapMarketOfferSession {
    private ScrapMarket shop;
    private Player player;
    private Item item;
    private Map<String, Object> formDefaults = new HashMap<>();
    private Dialog form = new Dialog()
            .setTitle("Offer Item")
            // .addSection(new DialogSection().setTitle("Unit Quantity").setInput(new DialogTextInput().setKey("unit_quantity")))
            .addSection(new DialogSection().setTitle("Price").setInput(new DialogTextInput().setKey("price")))
            .addSection(new DialogSection().setTitle("Quantity Listed").setInput(new DialogTextInput().setKey("stock")))
            ;

    public ScrapMarketOfferSession(ScrapMarket shop, Player player, Item item) {
        this.shop = shop;
        this.player = player;
        this.item = item;
    }

    public void showNextDialog() {
        Dialog dialog = new Dialog().setType(DialogType.ANDROID).setTitle("Offering " + item.getTitle());

        dialog.addSection(getProductSection1(item));
        Map<String, ScrapMarketProduct> playersOldProducts = shop.getProductsBySeller().get(player.getDocumentId());
        ScrapMarketProduct oldProduct = playersOldProducts != null ? playersOldProducts.get(item.getId()) : null;

        // Show a warning if the player already has this item for offer.
        if(oldProduct != null) {
            String message = String.format(
                    "You already have this item on the Scrap Market, %s for %d shilling%s, with %d left in stock. Submitting this offer will take that offer down.",
                    oldProduct.getUnitQuantity() == 1 ? "each" : Integer.toString(oldProduct.getUnitQuantity()),
                    oldProduct.getPrice(),
                    oldProduct.getPrice() == 1 ? "" : "s",
                    oldProduct.getStock()
            );
            if(player.isV3()) {
                dialog.addSection(new DialogSection().setText("<color=#ff8844>" + message + "</color>"));
            } else {
                dialog.addSection(new DialogSection().setText(message).setTextColor("ff8844"));
            }
        }

        for(DialogSection formSection : form.getSections()) {
            if(formSection.getInput() != null) {
                if("stock".equals(formSection.getInput().getKey())) {
                    formSection.setText(String.format(
                            "You have %d of %s in your inventory, thus you can stock at most this much.",
                            player.getInventory().getQuantity(item),
                            item.getTitle()
                    ));
                }

                // form is cloned per instance thus it is safe to modify it.
                if(formDefaults.containsKey(formSection.getInput().getKey())) {
                    formSection.getInput().setValue(formDefaults.get(formSection.getInput().getKey()));
                }
            }

            dialog.addSection(formSection);
        }

        player.showDialog(dialog, this::confirm);
    }

    public DialogSection getProductSection1(Item item) {
        // This is eyeballed
        int spaces = (int)Math.max(0.0f, 1.5f * (18 - item.getTitle().length()));
        String padding = String.join("", Collections.nCopies(spaces, " "));
        return new DialogSection()
                .addItem(new DialogListItem().setItem(item.getCode()).setText(padding + item.getTitle()));
    }

    public void confirm(Object[] ans) {
        if(ans.length == 0 || "cancel".equals(ans[0])) {
            return;
        }

        Map<String, Integer> responses = new HashMap<>();
        int i = 0;
        for(DialogSection section : form.getSections()) {
            if(section.getInput() != null) {
                if(i >= ans.length) {
                    player.showDialog(DialogHelper.messageDialog("Error", "Invalid dialog input."));
                    return;
                }
                Object val = ans[i++];
                if(val instanceof Number) {
                    responses.put(section.getInput().getKey(), (int)val);
                } else if(val instanceof String) {
                    if(((String) val).isBlank()) {
                        continue;
                    }
                    try {
                        responses.put(section.getInput().getKey(), Integer.parseInt((String)val));
                    } catch(NumberFormatException e) {
                        player.showDialog(DialogHelper.messageDialog("Error", "Failed to parse number."));
                        return;
                    }
                } else {
                    player.showDialog(DialogHelper.messageDialog("Error", "Unexpected data type."));
                    return;
                }
            }
        }

        confirm(responses);
    }

    public void confirm(Map<String, Integer> responses) {
        int inventory = player.getInventory().getQuantity(item);
        Integer unitQuantity = responses.getOrDefault("unit_quantity", 1);
        Integer price = responses.get("price");
        Integer stock = responses.getOrDefault("stock", inventory);

        if(unitQuantity == null) {
            fail("Unit quantity is missing.");
            return;
        }

        if(price == null) {
            fail("Price is missing.");
            return;
        }

        if(stock == null) {
            fail("Stock is missing.");
            return;
        }

        if(unitQuantity <= 0) {
            fail("Unit quantity must be positive.");
            return;
        }

        if(price <= 0) {
            fail("Price must be positive.");
            return;
        }

        if(stock <= 0) {
            fail("Stock must be positive.");
            return;
        }

        if(stock > inventory) {
            fail(String.format("You only have %d of this item.", inventory));
            return;
        }

        shop.removeProduct(player.getDocumentId(), item.getId());
        shop.addProduct(new ScrapMarketProduct(player.getDocumentId(), item.getId(), unitQuantity, stock, price));
    }

    public void fail(String message) {
        player.showDialog(DialogHelper.messageDialog("Error", message));
    }

    public ScrapMarketOfferSession setUnitQuantityDefault(Object unitQuantity) {
        formDefaults.put("unit_quantity", unitQuantity.toString());
        return this;
    }

    public ScrapMarketOfferSession setPriceDefault(Object price) {
        formDefaults.put("price", price.toString());
        return this;
    }

    public ScrapMarketOfferSession setStockDefault(Object stock) {
        formDefaults.put("stock", stock.toString());
        return this;
    }
}
