package brainwine.gameserver.scrapmarket;

import brainwine.gameserver.GameConfiguration;
import brainwine.gameserver.util.MapHelper;
import brainwine.shared.JsonHelper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.*;

import static brainwine.shared.LogMarkers.SERVER_MARKER;

public class ScrapMarket {
    private static String FILE_NAME = "scrap-market.json";
    private static final Logger logger = LogManager.getLogger();
    private static ScrapMarket instance;

    private Set<ScrapMarketProduct> products = new LinkedHashSet<>();
    private Map<String, Map<String, ScrapMarketProduct>> productsBySeller = new HashMap<>();
    private Map<String, LinkedHashSet<ScrapMarketProduct>> productsByInventoryTab = new HashMap<>();
    private Map<String, String> itemToInventoryTab = new HashMap<>();

    public void loadScrapMarketData() {
        logger.info(SERVER_MARKER, "Loading android shop data ...");
        loadInventoryTabs();
        loadJson();
    }

    private void loadInventoryTabs() {
        itemToInventoryTab.clear();

        List<Map<String, Object>> inventoryConfig = MapHelper.getList(GameConfiguration.getBaseConfig(), "inventory");
        for(Map<String, Object> tab : inventoryConfig) {
            String tabName = MapHelper.getString(tab, "name");
            List<String> itemIds = MapHelper.getList(tab, "items");
            itemIds.forEach(id -> itemToInventoryTab.put(id, tabName));
        }
    }

    private void loadJson() {
        List<ScrapMarketProduct> newProducts = new ArrayList<>();
        try {
            File file = new File(FILE_NAME);
            if(file.exists()) {
                newProducts = JsonHelper.readValue(file, new TypeReference<List<ScrapMarketProduct>>() {});
                Objects.requireNonNull(newProducts);
            }
        } catch(Exception e) {
            logger.error(SERVER_MARKER, "Could not load scrap market data", e);
            return;
        }

        products.clear();
        productsBySeller.clear();
        productsByInventoryTab.clear();

        for(ScrapMarketProduct product : newProducts) {
            if(product.validate()) {
                addProduct(product);
            }
        }
        logger.info(SERVER_MARKER, "Successfully loaded {} scrap market products. Skipped {} due to not found seller or item.",
                products.size(),
                newProducts.size() - products.size()
        );
    }

    public void saveJson() {
        try {
            JsonHelper.writeValue(new File(FILE_NAME), products);
        } catch(Exception e) {
            logger.error("Failed to write {}", FILE_NAME, e);
        }
    }

    public void addProduct(ScrapMarketProduct product) {
        products.add(product);
        String tabName = itemToInventoryTab.getOrDefault(product.getItemId(), "other");
        productsByInventoryTab.computeIfAbsent(tabName, k -> new LinkedHashSet<>()).add(product);
        productsBySeller.computeIfAbsent(product.getSellerId(), k -> new LinkedHashMap<>()).put(product.getItemId(), product);
    }

    public void removeProduct(ScrapMarketProduct product) {
        products.remove(product);
        String tabName = itemToInventoryTab.getOrDefault(product.getItemId(), "other");
        Set<ScrapMarketProduct> tabProducts = productsByInventoryTab.get(tabName);
        if(tabProducts != null) {
            tabProducts.remove(product);
            if(tabProducts.isEmpty()) {
                productsByInventoryTab.remove(tabName);
            }
        }
        if(productsBySeller.get(product.getSellerId()) != null) {
            productsBySeller.get(product.getSellerId()).remove(product.getItemId());
        }
    }

    public void removeProduct(String sellerId, String itemId) {
        Map<String, ScrapMarketProduct> products = productsBySeller.get(sellerId);

        if(products != null) {
            ScrapMarketProduct product = products.get(itemId);

            if(product != null) {
                removeProduct(product);
            }
        }
    }

    public Set<ScrapMarketProduct> getProducts() {
        return Collections.unmodifiableSet(products);
    }

    public Map<String, Map<String, ScrapMarketProduct>> getProductsBySeller() {
        return productsBySeller;
    }

    public Map<String, LinkedHashSet<ScrapMarketProduct>> getProductsByInventoryTab() {
        return productsByInventoryTab;
    }

    public static ScrapMarket getInstance() {
        if(instance == null) instance = new ScrapMarket();
        return instance;
    }
}
