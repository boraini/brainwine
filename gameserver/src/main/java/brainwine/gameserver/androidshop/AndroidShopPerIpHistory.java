package brainwine.gameserver.androidshop;

import brainwine.gameserver.item.Item;
import brainwine.gameserver.player.Player;
import brainwine.gameserver.util.ValueWithExpiry;
import brainwine.shared.JsonHelper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AndroidShopPerIpHistory {
    private static final String FILE_NAME = "android-purchases-per-ip.json";
    Map<String, AndroidShopHistory> historyByIp = new HashMap<>();
    private static final Logger logger = LogManager.getLogger();

    private static AndroidShopPerIpHistory instance = null;

    public void load() {
        logger.info("Loading android shop purchase history per IP...");
        try {
            historyByIp.clear();

            File file = new File(FILE_NAME);

            if(file.exists()) {
                try {
                    historyByIp.putAll(JsonHelper.readValue(file, new TypeReference<Map<String, AndroidShopHistory>>() {}));
                } catch(Exception ignored) {
                    System.out.println("trying old format");
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
            File file = new File(FILE_NAME);

            if(!file.exists()) {
                file.createNewFile();
            }

            JsonHelper.writeValue(file, historyByIp);
        } catch(Exception e) {
            logger.error("Could not write the android shop purchase history by IP.", e);
        }
    }

    public String getKey(Player player) {
        if(player.isOnline()) {
            return player.getConnection().getIpAddress().toString();
        }

        return null;
    }

    public AndroidShopHistory getHistory(Player player) {
        String key = getKey(player);
        return historyByIp.get(key);
    }

    public void recordPurchase(Player player, Item item, int quantity) {
        String key = getKey(player);
        if(key == null) return;

        AndroidShopHistory shopHistory = getHistory(player);
        if(shopHistory == null) {
            shopHistory = new AndroidShopHistory();
        }

        shopHistory.recordPurchase(item, quantity);

        historyByIp.put(key, shopHistory);
    }

    public static AndroidShopPerIpHistory getInstance() {
        if(instance == null) {
            instance = new AndroidShopPerIpHistory();
        }

        return instance;
    }
}
