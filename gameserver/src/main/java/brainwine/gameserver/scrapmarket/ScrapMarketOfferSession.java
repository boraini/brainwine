package brainwine.gameserver.scrapmarket;

import brainwine.gameserver.dialog.Dialog;
import brainwine.gameserver.dialog.DialogHelper;
import brainwine.gameserver.dialog.DialogListItem;
import brainwine.gameserver.dialog.DialogSection;
import brainwine.gameserver.dialog.DialogType;
import brainwine.gameserver.dialog.input.DialogTextInput;
import brainwine.gameserver.item.Item;
import brainwine.gameserver.item.ItemRegistry;
import brainwine.gameserver.player.Player;
import brainwine.gameserver.player.Skill;

import java.text.DecimalFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ScrapMarketOfferSession {
    private static final double SERVICE_CHARGE_FACTOR = 0.05;

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
        if(!player.isGodMode()) {
            if(getAllowedListings() == 0) {
                player.showDialog(DialogHelper.messageDialog(
                    "Low Barter Skill",
                    "You must have at least barter level 10 to be able to list items."
                ).setType(DialogType.ANDROID));
                return;
            }

            Map<String, ScrapMarketProduct> playerProducts = shop.getProductsBySeller().get(player.getDocumentId());

            if(playerProducts != null
                && !playerProducts.containsKey(item.getId())
                && playerProducts.size() >= getAllowedListings()
            ) {
                player.showDialog(DialogHelper.messageDialog(
                    "Too Many Listings",
                    String.format("You must have higher barter level to list more items. You have already listed your maximum of %d.", getAllowedListings())
                ).setType(DialogType.ANDROID));
                return;
            }
        }

        Dialog dialog = new Dialog().setType(DialogType.ANDROID).setTitle("Offering " + item.getTitle());

        dialog.addSection(getProductSection1(item));
        Map<String, ScrapMarketProduct> playersOldProducts = shop.getProductsBySeller().get(player.getDocumentId());
        ScrapMarketProduct oldProduct = playersOldProducts != null ? playersOldProducts.get(item.getId()) : null;

        DecimalFormat df = new DecimalFormat();
        df.setMaximumFractionDigits(2);
        dialog.addSection(new DialogSection().setText(String.format(
            "All listings are subject to a %s%% service charge based on the amount listed.",
            df.format(SERVICE_CHARGE_FACTOR * 100.0)
        )));

        // Show a warning if the player already has this item for offer.
        if(oldProduct != null) {
            String message = String.format(
                    "You already have this item on the Scrap Market, %s for %d shilling%s, with %d left listed. Submitting this offer will take that offer down. You may still pay a service charge.",
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
                            "You have %d of %s in your inventory, thus you can list at most this much.",
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
                    player.showDialog(DialogHelper.messageDialog("Error", "Invalid dialog input.").setType(DialogType.ANDROID));
                    return;
                }
                Object val = ans[i++];
                if(val instanceof Number) {
                    responses.put(section.getInput().getKey(), (int)val);
                } else if(val instanceof String) {
                    if(((String) val).matches("^\\s*$")) {
                        continue;
                    }
                    try {
                        responses.put(section.getInput().getKey(), Integer.parseInt((String)val));
                    } catch(NumberFormatException e) {
                        player.showDialog(DialogHelper.messageDialog("Error", "Failed to parse number.").setType(DialogType.ANDROID));
                        return;
                    }
                } else {
                    player.showDialog(DialogHelper.messageDialog("Error", "Unexpected data type.").setType(DialogType.ANDROID));
                    return;
                }
            }
        }

        confirm(responses);
    }

    public void confirm(Map<String, Integer> responses) {
        if(!player.isGodMode()) {
            if(getAllowedListings() == 0) {
                fail("You must have at least barter level 10 to be able to list items.");
                return;
            }

            Map<String, ScrapMarketProduct> playerProducts = shop.getProductsBySeller().get(player.getDocumentId());

            if(playerProducts != null
                && !playerProducts.containsKey(item.getId())
                && playerProducts.size() >= getAllowedListings()
            ) {
                fail(String.format("You can list maximum %d items.", getAllowedListings()));
                return;
            }
        }

        int inventory = player.getInventory().getQuantity(item);
        Integer unitQuantity = responses.getOrDefault("unit_quantity", 1);
        Integer price = responses.get("price");
        Integer stock = responses.getOrDefault("stock", inventory);
        Item shillings = ItemRegistry.getItem("accessories/shillings");

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

        int totalCost = price * stock;
        int serviceCharge = (int)Math.ceil(SERVICE_CHARGE_FACTOR * totalCost);
        if(!player.isGodMode()) {
            if(!player.getInventory().hasItem(shillings, serviceCharge)) {
                fail(String.format("You do not have %d shillings to pay the service charge.", serviceCharge));
                return;
            }
        }

        Dialog confirmationDialog = new Dialog().setType(DialogType.ANDROID).setTitle("Confirmation");

        confirmationDialog.addSection(new DialogSection().setText(String.format(
                "You are about to list %d %s for %d shilling%s for %s",
                stock,
                item.getTitle(),
                price,
                price == 1 ? "" : "s",
                unitQuantity == 1 ? "each" : Integer.toString(unitQuantity)
        )));
        
        if(!player.isGodMode() && serviceCharge > 0) {
            confirmationDialog.addSection(new DialogSection().setText(String.format(
                "There will be a %d shilling%s service charge.",
                serviceCharge,
                serviceCharge == 1 ? "" : "s"
            )));
        }

        confirmationDialog.addSection(new DialogSection().setText("Are you sure?"));

        confirmationDialog.setActions("yesno");

        player.showDialog(confirmationDialog, ans -> {
            if(ans.length > 0 && "Yes".equals(ans[0])) {
                if(!player.getInventory().hasItem(shillings, serviceCharge)) return;

                if(!player.isGodMode()) player.getInventory().removeItem(shillings, serviceCharge, true);
                shop.removeProduct(player.getDocumentId(), item.getId());
                shop.addProduct(new ScrapMarketProduct(player.getDocumentId(), item.getId(), unitQuantity, stock, price));
            }
        });
    }

    public void fail(String message) {
        player.showDialog(DialogHelper.messageDialog("Error", message).setType(DialogType.ANDROID));
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

    private int getAllowedListings() {
        int barterLevel = player.getTotalSkillLevel(Skill.BARTER);
        if(barterLevel >= 13) return 4;
        if(barterLevel >= 12) return 3;
        if(barterLevel >= 11) return 2;
        if(barterLevel >= 10) return 1;
        return 0;
    }
}
