package brainwine.gameserver.scrapmarket;

import brainwine.gameserver.GameServer;
import brainwine.gameserver.item.Item;
import brainwine.gameserver.item.ItemRegistry;
import brainwine.gameserver.player.Player;
import brainwine.gameserver.player.Skill;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.OffsetDateTime;
import java.util.NoSuchElementException;

public class ScrapMarketProduct {
    private final String sellerId;
    private final String itemId;
    private final int unitQuantity;
    private final int price;
    private final OffsetDateTime offerDate;
    private int stock;

    public ScrapMarketProduct() {
        this.sellerId = "";
        this.itemId = "";
        this.unitQuantity = 0;
        this.stock = 0;
        this.price = 0;
        this.offerDate = OffsetDateTime.now();
    }

    public ScrapMarketProduct(String sellerId, String itemId, int unitQuantity, int stock, int price) {
        this.sellerId = sellerId;
        this.itemId = itemId;
        this.unitQuantity = unitQuantity;
        this.stock = stock;
        this.price = price;
        this.offerDate = OffsetDateTime.now();
    }

    public boolean validate() {
        GameServer server = GameServer.getInstance();
        Player seller = server.getPlayerManager().getPlayerById(sellerId);
        Item item = ItemRegistry.getItem(itemId);

        return seller != null && item != null;
    }

    private Player expectSeller() {
        Player seller = GameServer.getInstance().getPlayerManager().getPlayerById(sellerId);
        if(seller == null) {
            throw new NoSuchElementException("Selling player does not exist anymore.");
        }

        return seller;
    }

    private Item expectItem() {
        Item item = ItemRegistry.getItem(itemId);
        if(item.isAir()) {
            throw new NoSuchElementException("Item not found.");
        }

        return item;
    }

    private Item expectShillings() {
        Item shillings = ItemRegistry.getItem("accessories/shillings");
        if(shillings.isAir()) {
            throw new NoSuchElementException("Shillings item not found.");
        }

        return shillings;
    }

    public boolean inStock(int quantity) {
        return quantity <= stock;
    }

    public boolean availableInInventory(int quantity) {
        return expectSeller().getInventory().hasItem(expectItem(), quantity);
    }

    public boolean checkBarterLevel() {
        return expectSeller().getTotalSkillLevel(Skill.BARTER) >= ScrapMarket.MIN_BARTER_LEVEL;
    }

    public void purchase(Player buyer, int quantity) {
        Player seller = expectSeller();
        Item item = expectItem();

        int totalQuantity = quantity;

        if(!inStock(quantity)) {
            throw new IllegalArgumentException("There is not enough of this item for sale.");
        }

        if(!availableInInventory(quantity)) {
            throw new IllegalArgumentException(seller.getName() + " does not have enough of this item in their inventory.");
        }

        if(!checkBarterLevel()) {
            throw new IllegalArgumentException(seller.getName() + " does not have a sufficient barter level.");
        }

        seller.getInventory().removeItem(item, totalQuantity, true);
        buyer.getInventory().addItem(item, totalQuantity, true);
        stock -= totalQuantity;
    }

    public String getSellerId() {
        return sellerId;
    }

    public String getItemId() {
        return itemId;
    }

    public int getUnitQuantity() {
        return unitQuantity;
    }

    public int getStock() {
        return stock;
    }

    public int getPrice() {
        return price;
    }

    @JsonIgnore
    public String getDialogId() {
        return sellerId + "/" + itemId;
    }

    public OffsetDateTime getOfferDate() {
        return offerDate;
    }

    public static String getSellerIdFromDialogId(String dialogId) {
        return dialogId.substring(0, dialogId.indexOf("/"));
    }

    public static String getItemIdFromDialogId(String dialogId) {
        return dialogId.substring(dialogId.indexOf("/") + 1);
    }
}
