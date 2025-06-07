package brainwine.gameserver.androidshop;

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
import brainwine.gameserver.shop.ItemProduct;
import brainwine.gameserver.shop.Product;
import brainwine.gameserver.shop.ShopSection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
            e.printStackTrace();
            // logger.error(e);
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

    public DialogSection getProductSection(Product product) {
        DialogSection itemDescriptionSection = new DialogSection().setTitle(product.getName());

        Item item = Item.AIR;
        if (product.getImage().getBaseSprite().startsWith("inventory/")) {
            DialogListItem listItem = new DialogListItem();
            String itemId = product.getImage().getBaseSprite().substring("inventory/".length());
            item = ItemRegistry.getItem(itemId);
        }

        itemDescriptionSection.setTitle(product.getName());
        itemDescriptionSection.addItem(new DialogListItem().setItem(item.getCode()).setText(product.getDescription()));

        return itemDescriptionSection;
    }

    public void showSectionDialog() {
        ShopSection shopSection = shop.getSections().get(currentSection.get());

        Dialog dialog = new Dialog().setType(DialogType.ANDROID).setTitle(shopSection.getName());

        for(String productId : shopSection.getProducts()) {
            Product product = shop.getProducts().get(productId);
            if(product == null) continue;
            if(product instanceof ItemProduct) {
                boolean canBuy = player.getInventory().getQuantity(ItemRegistry.getItem("accessories/shillings")) >= product.getCost();
                dialog.addSection(getProductSection(product));

                DialogSection buySection = new DialogSection()
                        .setChoice(productId)
                        .setText(String.format(
                                player.isV3() && !canBuy ? "<color=#ff8844>Buy %s | %d shilling%s each</color>" : "Buy %s | %d shilling%s each",
                                product.getName(),
                                product.getCost(),
                                product.getCost() == 1 ? "" : "s"
                        ));

                if(!canBuy && !player.isV3()) buySection.setTextColor("ff8844");

                dialog.addSection(buySection);
            }
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
        Product product = shop.getProducts().get(currentProduct.get());
        Dialog dialog = new Dialog().setType(DialogType.ANDROID).setTitle("Buying " + product.getName());

        int canBuy = player.getInventory().getQuantity(ItemRegistry.getItem("accessories/shillings")) / product.getCost();
        dialog.addSection(getProductSection(product));

        DialogSection buySection = new DialogSection().setText(String.format(
                player.isV3() && (canBuy <= 0) ? "<color=#ff8844>%d shilling%s each</color>" : "%d shilling%s each",
                product.getCost(),
                product.getCost() == 1 ? "" : "s"
        ));
        if((canBuy <= 0) && !player.isV3()) buySection.setTextColor("ff8844");

        if(canBuy <= 0) {
            dialog.addSection(new DialogSection().setText("Sorry, you don't have enough shillings to buy any of this item."));
        } else {
            dialog.addSection(TradeSession.Dialogs.createQuantitySelector(canBuy).setTitle("How many are you buying?"));
        }

        player.showDialog(dialog, ans -> {
            if(ans.length == 0 || "cancel".equals(ans[0])) {
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
        Product product = shop.getProducts().get(currentProduct.get());
        if(product == null) {
            end(false);
            return;
        }

        Item shillings = ItemRegistry.getItem("accessories/shillings");
        if(player.getInventory().hasItem(shillings, product.getCost())) {
            player.getInventory().removeItem(shillings, product.getCost());
            product.purchase(player);
            if(me != null) me.emote("Good trade!");
            end(true);
        } else {
            player.showDialog(DialogHelper.messageDialog("You don't have enough shillings for this many " + product.getName() + "!").setType(DialogType.ANDROID));
            end(false);
        }
    }

    public void end(boolean outcome) {
        if(onOutcome != null) onOutcome.accept(outcome);
    }
}
