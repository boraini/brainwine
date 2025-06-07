package brainwine.gameserver.androidshop;

import brainwine.gameserver.GameConfiguration;
import brainwine.gameserver.resource.ResourceFinder;
import brainwine.gameserver.shop.Product;
import brainwine.gameserver.shop.ShopSection;
import brainwine.shared.JsonHelper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static brainwine.shared.LogMarkers.SERVER_MARKER;

public class AndroidShop {
    private static final Logger logger = LogManager.getLogger();
    private final Map<String, ShopSection> sections = new LinkedHashMap<>();
    private final Map<String, Product> products = new LinkedHashMap<>();

    private static AndroidShop instance;

    public void loadShopData() {
        logger.info(SERVER_MARKER, "Loading android shop data ...");
        sections.clear();
        products.clear();

        // Clear out default shop config
        Map<String, Object> gameConfig = GameConfiguration.getBaseConfig();

        try {
            URL url = ResourceFinder.getResourceUrl("android-shop.json");
            Map<String, Object> data = JsonHelper.readValue(url, new TypeReference<Map<String, Object>>(){});
            sections.putAll(JsonHelper.readValue(data.getOrDefault("sections", Collections.emptyMap()), new TypeReference<LinkedHashMap<String, ShopSection>>(){}));
            products.putAll(JsonHelper.readValue(data.getOrDefault("products", Collections.emptyMap()), new TypeReference<LinkedHashMap<String, Product>>(){}));
        } catch(Exception e) {
            logger.error(SERVER_MARKER, "Could not load shop data", e);
            return;
        }

        logger.info(SERVER_MARKER, "Successfully loaded the android shop with {} product{}", products.size(), products.size() == 1 ? "" : "s");
    }

    public Map<String, ShopSection> getSections() {
        return sections;
    }

    public Map<String, Product> getProducts() {
        return products;
    }

    public static AndroidShop getInstance() {
        if (instance == null) instance = new AndroidShop();
        return instance;
    }
}
