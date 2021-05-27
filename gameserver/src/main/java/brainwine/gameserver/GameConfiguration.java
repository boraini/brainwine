package brainwine.gameserver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import brainwine.gameserver.command.CommandManager;
import brainwine.gameserver.entity.player.Player;
import brainwine.gameserver.item.Item;
import brainwine.gameserver.item.ItemRegistry;
import brainwine.gameserver.util.MapHelper;
import brainwine.gameserver.util.VersionUtils;

@SuppressWarnings("unchecked")
public class GameConfiguration {
    
    private static final Logger logger = LogManager.getLogger();
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Map<String, Object> baseConfig = new HashMap<String, Object>();
    private static final Map<String, Map<String, Object>> configUpdates = new HashMap<>();
    private static final Map<String, Map<String, Object>> versionedConfigs = new HashMap<>();
    private static Yaml yaml;
    
    public static void init() {
        long startTime = System.currentTimeMillis();
        logger.info("Loading game configuration ...");
        mapper.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE, true);
        mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true);
        LoaderOptions options = new LoaderOptions();
        options.setMaxAliasesForCollections(Short.MAX_VALUE);
        yaml = new Yaml(options);
        loadConfigFiles();
        logger.info("Configuring ...");
        configure();
        logger.info("Caching versioned configurations ...");
        cacheVersionedConfigs();
        logger.info("Load complete! Took {} milliseconds", System.currentTimeMillis() - startTime);
    }
    
    private static void cacheVersionedConfigs() {
        configUpdates.keySet().forEach(version -> {
            Map<String, Object> config = MapHelper.copy(baseConfig);
            
            configUpdates.forEach((version2, update) -> {
                if(VersionUtils.isGreaterOrEqualTo(version2, version2)) {
                    merge(config, update);
                }
            });
            
            versionedConfigs.put(version, config);
        });
    }
    
    private static void configure() {
        // Client wants this
        MapHelper.put(baseConfig, "shop.currency", new HashMap<>());
        Map<String, Object> items = MapHelper.getMap(baseConfig, "items");
        
        // Add custom commands to the client config
        CommandManager.getCommandNames().forEach(command -> {
            MapHelper.put(baseConfig, String.format("commands.%s", command), true);
        });
        
        // Map inventory positions for items
        Map<String, int[]> inventoryPositions = new HashMap<>();
        List<Object> inventoryCategories = MapHelper.getList(baseConfig, "inventory");
        
        for(int i = 0; i < inventoryCategories.size(); i++) {
            Map<String, Object> map = (Map<String, Object>)inventoryCategories.get(i);
            List<String> itemNames = (List<String>)map.get("items");
            
            for(int j = 0; j < itemNames.size(); j++) {
                inventoryPositions.put(itemNames.get(j), new int[]{i, j});
            }
        }
        
        // Configure items
        Map<String, Object> configV3 = configUpdates.get("3.0.0");
        
        if(items != null) {
            List<String> ignoredItems = new ArrayList<>();
            items.forEach((name, v) -> {
                Map<String, Object> config = (Map<String, Object>)v;
                String[] segments = name.split("/", 2);
                String category = segments.length == 2 ? segments[0] : "unknown";
                config.put("id", name);
                
                // Create an item title if it's missing
                if(!config.containsKey("title")) {
                    String title = (segments.length > 1 ? segments[1] : segments[0]);
                    String[] words = title.split("-");
                    
                    for(int i = 0; i < words.length; i++) {
                        String word = words[i];
                        char[] chars = word.toCharArray();
                        chars[0] = Character.toUpperCase(chars[0]);
                        words[i] = new String(chars);
                    }
                    
                    config.put("title", String.join(" ", words));
                }
                
                config.putIfAbsent("category", category);
                config.putIfAbsent("inventory_position", inventoryPositions.getOrDefault(name, new int[]{16, 0}));
                config.putIfAbsent("block_size", new int[]{1, 1});
                config.putIfAbsent("size", new int[]{1, 1});
                
                if(config.containsKey("use")) {
                    Map<String, Object> useConfig = (Map<String, Object>)config.get("use");
                    useConfig.remove("sound"); // TODO
                    
                    // Move change definitions to item root, but *only* for V3 clients
                    if(useConfig.containsKey("change")) {
                        MapHelper.put(configV3, String.format("items.%s.change", name), Arrays.asList(useConfig.get("change")));
                    }
                }
                
                // Assign layers based on category. TODO this is not accurate.
                switch(category) {
                case "base":
                case "back":
                case "front":
                case "liquid":
                    config.put("layer", category);
                    break;
                case "ground":
                case "building":
                case "furniture":
                case "lighting":
                case "industrial":
                case "vegetation":
                case "mechanical":
                case "rubble":
                case "containers":
                case "arctic":
                    config.put("layer", "front");
                    break;
                }
                
                // Register item
                if(config.containsKey("code")) {
                    InjectableValues.Std injectableValues = new InjectableValues.Std();
                    injectableValues.addValue("name", name);
                    mapper.setInjectableValues(injectableValues);
                    
                    try {
                        Item item = mapper.readValue(mapper.writer().writeValueAsString(config), Item.class);
                        ItemRegistry.registerItem(item);
                    } catch (JsonProcessingException e) {
                        logger.fatal("Failed to register item {}", name, e);
                        System.exit(0);
                    }
                } else {
                    ignoredItems.add(name);
                }
            });
            
            for(String item : ignoredItems) {
                items.remove(item);
            }
        }
        
        logger.info("Successfully loaded {} item(s)", ItemRegistry.getItems().size());
    }
    
    private static void loadConfigFiles() {
        try {
            Reflections reflections = new Reflections("config", new ResourcesScanner());
            Set<String> fileNames = reflections.getResources(x -> x.endsWith(".yml"));
            
            for(String fileName : fileNames) {
                Map<String, Object> config = yaml.load(GameConfiguration.class.getResourceAsStream(String.format("/%s", fileName)));
                
                if(fileName.contains("versions")) {
                    String[] segments = fileName.replace(".yml", "").split("-");
                    
                    if(segments.length < 3) {
                        throw new IllegalArgumentException(String.format("Invalid name for config update '%s', expected format: config-versions-{version}.yml", fileName));
                    }
                    
                    String version = segments[2];
                    configUpdates.put(segments[2], (Map<String, Object>)config.get(version));
                    continue;
                }
                
                baseConfig.putAll(config);
            }
        } catch(Exception e) {
            logger.fatal("Could not load configuration files", e);
            System.exit(-1);
        }
    }
    
    private static void merge(Map<String, Object> dst, Map<String, Object> src) {
        src.forEach((k, v) -> {
            if(dst.containsKey(k)) {
                Object o = dst.get(k);
                
                if(o instanceof Map && v instanceof Map) {
                    merge((Map<String, Object>)o, (Map<String, Object>)v);
                } else {
                    dst.put(k, v);
                }
            } else {
                dst.put(k, v);
            }
        });
    }
    
    public static Map<String, Object> getBaseConfig() {
        return baseConfig;
    }
    
    public static Map<String, Object> getClientConfig(Player player) {
        Map<String, Object> config = baseConfig;
        
        for(Entry<String, Map<String, Object>> entry : versionedConfigs.entrySet()) {
            if(VersionUtils.isGreaterOrEqualTo(player.getClientVersion(), entry.getKey())) {
                config = entry.getValue();
            }
        }
        
        return config;
    }
}
