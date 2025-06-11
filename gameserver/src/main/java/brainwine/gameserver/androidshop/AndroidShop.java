package brainwine.gameserver.androidshop;

import brainwine.gameserver.item.Item;
import brainwine.gameserver.item.ItemRegistry;
import brainwine.gameserver.resource.ResourceFinder;
import brainwine.gameserver.shop.ItemProduct;
import brainwine.gameserver.shop.Product;
import brainwine.gameserver.shop.ProductImage;
import brainwine.gameserver.shop.ShopSection;
import brainwine.gameserver.util.MapHelper;
import brainwine.shared.JsonHelper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static brainwine.shared.LogMarkers.SERVER_MARKER;

public class AndroidShop {
    private static final Logger logger = LogManager.getLogger();
    private final Map<String, ShopSection> sections = new LinkedHashMap<>();
    private final Map<String, Product> products = new LinkedHashMap<>();
    private AndroidShopAdjustments adjustments = new AndroidShopAdjustments();

    private static AndroidShop instance;

    public void loadShopData() {
        logger.info(SERVER_MARKER, "Loading android shop data ...");
        sections.clear();
        products.clear();

        try {
            URL url = ResourceFinder.getResourceUrl("android-shop.json");
            Map<String, Object> data = JsonHelper.readValue(url, new TypeReference<Map<String, Object>>() {});
            Map<String, Map<String, Object>> sectionData = (Map<String, Map<String, Object>>)data.get("sections");
            for(String sectionId : sectionData.keySet()) {
                String name = (String)sectionData.get(sectionId).get("name");
                String icon = (String)sectionData.get(sectionId).get("icon");
                Map<String, Integer> items = MapHelper.getMap(sectionData.get(sectionId), "items");
                List<String> productKeys = new ArrayList<>(items.keySet());

                for(String productId : items.keySet()) {
                    Item item = ItemRegistry.getItem(productId);
                    if(item.isAir()) {
                        productKeys.remove(productId);
                        continue;
                    }

                    Product product = new ItemProduct(
                            item.getTitle(),
                            item.getHint(),
                            new ProductImage("inventory/" + productId),
                            items.get(productId),
                            MapHelper.map(ItemRegistry.getItem(productId), 1)
                    );

                    products.put(productId, product);
                }

                if(!productKeys.isEmpty()) {
                    ShopSection section = new ShopSection(name, icon, productKeys.toArray(new String[0]));
                    sections.put(sectionId, section);
                }
            }

            adjustments = JsonHelper.readValue(url, AndroidShopAdjustments.class);
        } catch(Exception e) {
            logger.error(SERVER_MARKER, "Could not load android shop data", e);
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

    public AndroidShopAdjustments getAdjustments() {
        return adjustments;
    }

    public static AndroidShop getInstance() {
        if (instance == null) instance = new AndroidShop();
        return instance;
    }
}
