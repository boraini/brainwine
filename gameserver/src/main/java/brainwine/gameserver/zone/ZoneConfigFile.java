package brainwine.gameserver.zone;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ZoneConfigFile {
    
    @JsonSetter(nulls = Nulls.FAIL)
    private String name = "Mystery Zone";
    
    @JsonSetter(nulls = Nulls.SKIP)
    private Biome biome = Biome.PLAIN;
    
    @JsonSetter(nulls = Nulls.FAIL)
    private int width;
    
    @JsonSetter(nulls = Nulls.FAIL)
    private int height;
    
    @JsonSetter(nulls = Nulls.SKIP)
    private float acidity = 1.0F;
    
    @JsonSetter(nulls = Nulls.SKIP)
    private OffsetDateTime creationDate = OffsetDateTime.now();

    @JsonSetter(nulls = Nulls.SKIP)
    private Map<String, Integer> ecologicalMachineParts = new HashMap<>();
    
    public ZoneConfigFile(Zone zone) {
        this(zone.getName(), zone.getBiome(), zone.getWidth(), zone.getHeight(), zone.getAcidity(), zone.getCreationDate(), zone.getEcologicalMachineParts());
    }
    
    public ZoneConfigFile(String name, Biome biome, int width, int height, float acidity, OffsetDateTime creationDate, Map<String, Integer> ecologicalMachineParts) {
        this.name = name;
        this.biome = biome;
        this.width = width;
        this.height = height;
        this.acidity = acidity;
        this.creationDate = creationDate;
        this.ecologicalMachineParts = ecologicalMachineParts;
    }
    
    @JsonCreator
    private ZoneConfigFile(@JsonProperty(value = "name", required = true) String name,
            @JsonProperty(value = "width", required = true) int width,
            @JsonProperty(value = "height", required = true) int height) {
        this.name = name;
        this.width = width;
        this.height = height;
    }
    
    public String getName() {
        return name;
    }
    
    public Biome getBiome() {
        return biome;
    }
    
    public int getWidth() {
        return width;
    }
    
    public int getHeight() {
        return height;
    }
    
    public float getAcidity() {
        return acidity;
    }
    
    public OffsetDateTime getCreationDate() {
        return creationDate;
    }

    public Map<? extends String, ? extends Integer> getEcologicalMachineParts() {
        return ecologicalMachineParts;
    }
}
