package brainwine.gameserver.androidshop;

import brainwine.gameserver.item.Item;
import brainwine.gameserver.player.Player;
import brainwine.gameserver.util.ValueWithExpiry;
import brainwine.shared.JsonHelper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class AndroidShopPerIpHistory {
    private static final Logger logger = LogManager.getLogger();

    private static AndroidShopPerIpHistory saleInstance = null;
    private static AndroidShopPerIpHistory purchaseInstance = null;
    public static AndroidShopPerIpHistory getPurchaseInstance() {
        if(purchaseInstance == null) {
            purchaseInstance = new AndroidShopPerIpHistory("android-purchases-per-ip.json");
        }

        return purchaseInstance;
    }

    public static AndroidShopPerIpHistory getSaleInstance() {
        if(saleInstance == null) {
            saleInstance = new AndroidShopPerIpHistory("android-sales-per-ip.json");
        }

        return saleInstance;
    }

    Map<String, AndroidShopHistory> historyByIp = new HashMap<>();
    private final String fileName;

    public AndroidShopPerIpHistory(String fileName) {
        this.fileName = fileName;
    }

    public void load() {
        logger.info("Loading android shop purchase history per IP...");
        try {
            historyByIp.clear();

            File file = new File(fileName);

            if(file.exists()) {
                try {
                    historyByIp.putAll(JsonHelper.readValue(file, new TypeReference<Map<String, AndroidShopHistory>>() {}));
                } catch(Exception ignored) {
                    // Old format
                    Map<String, ValueWithExpiry<AndroidShopHistory>> oldFormatHistories = JsonHelper.readValue(file, new TypeReference<Map <String, ValueWithExpiry<AndroidShopHistory>>>() {});
                    for(Map.Entry<String, ValueWithExpiry<AndroidShopHistory>> history : oldFormatHistories.entrySet()) {
                        historyByIp.put(history.getKey(), history.getValue().getValue());
                    }
                }
            }
        } catch(Exception e) {
            logger.error("Could not read the android shop purchase history by IP.", e);
        }

        purgeExpired();
    }

    public void purgeExpired() {
        List<String> entriesToRemove = new ArrayList<>();

        for(Map.Entry<String, AndroidShopHistory> entry : historyByIp.entrySet()) {
            entry.getValue().removeOldPurchases();
            if(entry.getValue().getPurchases().isEmpty()) {
                entriesToRemove.add(entry.getKey());
            }
        }

        entriesToRemove.forEach(historyByIp::remove);
    }

    public void save() {
        try {
            File file = new File(fileName);

            if(!file.exists()) {
                file.createNewFile();
            }

            JsonHelper.writeValue(file, historyByIp);
        } catch(Exception e) {
            logger.error("Could not write the android shop purchase history by IP.", e);
        }
    }

    public String getIpKey(Player player) {
        if(player.isOnline()) {
            return player.getConnection().getIpAddress().toString();
        }

        return null;
    }

    public String getHardwareIdKey(Player player) {
        if(player.isOnline()) {
            return player.getHardwareUid();
        }

        return null;
    }

    private AndroidShopHistory getIpHistory(Player player) {
        String key = getIpKey(player);
        return historyByIp.get(key);
    }

    private AndroidShopHistory getHardwareIdHistory(Player player) {
        String key = getHardwareIdKey(player);
        return historyByIp.get(key);
    }

    public void removeOldPurchases(Player player) {
        AndroidShopHistory ipHistory = getIpHistory(player);
        if(ipHistory != null) ipHistory.removeOldPurchases();
        AndroidShopHistory hardwareIdHistory = getIpHistory(player);
        if(hardwareIdHistory != null) hardwareIdHistory.removeOldPurchases();
    }

    public int getPurchases(Player player, Item item) {
        AndroidShopHistory ipHistory = getIpHistory(player);
        AndroidShopHistory hardwareIdHistory = getHardwareIdHistory(player);

        // Handle either history missing
        if(ipHistory == null && hardwareIdHistory == null) return 0;
        if(hardwareIdHistory == null) return ipHistory.getPurchases(item);
        if(ipHistory == null) return hardwareIdHistory.getPurchases(item);

        List<AndroidShopHistory.Purchase> ipPurchases = ipHistory.getPurchases();
        List<AndroidShopHistory.Purchase> hardwareIdPurchases = hardwareIdHistory.getPurchases();

        int ipIdx = 0;
        int hardwareIdIdx = 0;

        int count = 0;

        while(ipIdx < ipPurchases.size() || hardwareIdIdx < hardwareIdPurchases.size()) {
            OffsetDateTime ipDate = ipIdx < ipPurchases.size() ? ipPurchases.get(ipIdx).getDate() : null;
            OffsetDateTime hardwareIdDate = hardwareIdIdx < hardwareIdPurchases.size() ? hardwareIdPurchases.get(hardwareIdIdx).getDate() : null;
            if(Objects.equals(ipDate, hardwareIdDate)) {
                // Process both purchases at the same time
                if(Objects.equals(ipPurchases.get(ipIdx).getItem(), hardwareIdPurchases.get(hardwareIdIdx).getItem()) && Objects.equals(ipPurchases.get(ipIdx).getQuantity(), hardwareIdPurchases.get(hardwareIdIdx).getQuantity())) {
                    // Same purchase, don't count twice
                    if(item.equals(ipPurchases.get(ipIdx).getItem())) {
                        count += ipPurchases.get(ipIdx).getQuantity();
                    }
                } else {
                    // Might be different purchases, count both
                    if(item.equals(ipPurchases.get(ipIdx).getItem())) {
                        count += ipPurchases.get(ipIdx).getQuantity();
                    }
                    if(item.equals(hardwareIdPurchases.get(hardwareIdIdx).getItem())) {
                        count += hardwareIdPurchases.get(hardwareIdIdx).getQuantity();
                    }
                }
                ipIdx++;
                hardwareIdIdx++;
            }

            // Process the earliest purchase left
            boolean processIp = hardwareIdDate == null || ipDate != null && ipDate.isBefore(hardwareIdDate);

            if(processIp) {
                if(item.equals(ipPurchases.get(ipIdx).getItem())) {
                    count += ipPurchases.get(ipIdx).getQuantity();
                }
                ipIdx++;
            } else {
                if(item.equals(hardwareIdPurchases.get(hardwareIdIdx).getItem())) {
                    count += hardwareIdPurchases.get(hardwareIdIdx).getQuantity();
                }
                hardwareIdIdx++;
            }
        }

        return count;
    }

    public void recordPurchase(Player player, Item item, int quantity) {
        OffsetDateTime now = OffsetDateTime.now();
        String key = getIpKey(player);
        if(key != null) {
            AndroidShopHistory shopHistory = getIpHistory(player);
            if(shopHistory == null) {
                shopHistory = new AndroidShopHistory();
            }

            shopHistory.recordPurchase(item, quantity, now);

            historyByIp.put(key, shopHistory);
        }

        String hardwareIdKey = getHardwareIdKey(player);
        if(hardwareIdKey != null) {
            AndroidShopHistory shopHardwareIdHistory = getHardwareIdHistory(player);
            if(shopHardwareIdHistory == null) {
                shopHardwareIdHistory = new AndroidShopHistory();
            }

            shopHardwareIdHistory.recordPurchase(item, quantity, now);

            historyByIp.put(hardwareIdKey, shopHardwareIdHistory);
        }
    }
}
