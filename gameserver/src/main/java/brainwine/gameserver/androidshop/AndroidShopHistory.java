package brainwine.gameserver.androidshop;

import brainwine.gameserver.item.Item;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AndroidShopHistory {
    private static final int FORGETTING_INTERVAL_HOURS = 24;

    @JsonProperty
    List<Purchase> purchases = new ArrayList<>();

    @JsonIgnore
    Map<Item, Integer> summary = new HashMap<Item, Integer>();

    public AndroidShopHistory() {
        removeOldPurchases();
    }

    public void removeOldPurchases() {
        OffsetDateTime now = OffsetDateTime.now();
        purchases.removeIf(item -> now.isAfter(item.date.plusHours(FORGETTING_INTERVAL_HOURS)));
        summary.clear();
        for(Purchase purchase : purchases) {
            summary.merge(purchase.item, 1, Integer::sum);
        }
    }

    public int getPurchases(Item item) {
        return summary.getOrDefault(item, 0);
    }

    public void recordPurchase(Item item, int quantity) {
        OffsetDateTime now = OffsetDateTime.now();
        purchases.add(new Purchase(now, item, quantity));
        summary.merge(item, 1, Integer::sum);
    }

    public static class Purchase {
        @JsonProperty("date")
        private OffsetDateTime date;
        @JsonProperty("item")
        private Item item;
        @JsonProperty("quantity")
        private int quantity;

        public Purchase() {}

        public Purchase(OffsetDateTime date, Item item, int quantity) {
            this.date = date;
            this.item = item;
            this.quantity = quantity;
        }
    }
}
