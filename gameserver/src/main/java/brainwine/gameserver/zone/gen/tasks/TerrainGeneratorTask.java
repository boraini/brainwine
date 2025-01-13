package brainwine.gameserver.zone.gen.tasks;

import brainwine.gameserver.item.Item;
import brainwine.gameserver.item.Layer;
import brainwine.gameserver.util.MathUtils;
import brainwine.gameserver.util.SimplexNoise;
import brainwine.gameserver.util.WeightedMap;
import brainwine.gameserver.zone.gen.GeneratorConfig;
import brainwine.gameserver.zone.gen.GeneratorContext;
import brainwine.gameserver.zone.gen.models.LayerSeparator;
import brainwine.gameserver.zone.gen.models.TerrainType;
import brainwine.gameserver.zone.gen.surface.SurfaceRegion;
import brainwine.gameserver.zone.gen.surface.SurfaceRegionType;

public class TerrainGeneratorTask implements GeneratorTask {
    
    private final TerrainType type;
    private final double minAmplitude;
    private final double maxAmplitude;
    private final LayerSeparator layerSeparator;
    private final int surfaceRegionSize;
    private final WeightedMap<SurfaceRegionType> surfaceRegionTypes;
    private final WeightedMap<SurfaceRegionType> underwaterRegionTypes;
    
    public TerrainGeneratorTask(GeneratorConfig config) {
        type = config.getTerrainType();
        minAmplitude = config.getMinAmplitude();
        maxAmplitude = config.getMaxAmplitude();
        layerSeparator = config.getLayerSeparator();
        surfaceRegionSize = config.getSurfaceRegionSize();
        surfaceRegionTypes = config.getSurfaceRegionTypes();
        underwaterRegionTypes = config.getUnderwaterRegionTypes();
    }
    
    @Override
    public void generate(GeneratorContext ctx) {
        int width = ctx.getWidth();
        int height = ctx.getHeight();
        int surfaceLevel;
        if(type == TerrainType.ASTEROIDS) {
            surfaceLevel = Math.min(height / 6, 100);
        } else if(type == TerrainType.OCEAN) {
            surfaceLevel = Math.min(5 * height / 6, 600);
        } else {
            surfaceLevel = Math.min(height / 3, 200);
        }
        int lowestSurfaceLevel = 0;
        
        // Determine surface first, then start placing blocks.
        if(type == TerrainType.FILLED) {
            for(int x = 0; x < width; x++) {
                ctx.setSurface(x, 0);
            }
        } else if(type == TerrainType.OCEAN) {
            double amplitude = ctx.nextDouble() * (maxAmplitude - minAmplitude) + minAmplitude;
            int oceanY = OceanGeneratorTask.getOceanY(ctx);
            for(int x = 0; x < width; x++) {
                double surfaceNoise = SimplexNoise.noise2(ctx.getSeed(), x / 256.0, 0, 7);

                double islandBiasRaw = 2.0 * SimplexNoise.noise2(ctx.getSeed(), x / 256.0, 0, 2) - 1.0;
                double islandBiasGained = Math.signum(islandBiasRaw) * Math.pow(Math.abs(islandBiasRaw), 0.6);
                double islandBias = -surfaceLevel * (0.5 + 0.25 * islandBiasGained);

                double erosionFactor = (oceanY < islandBias + surfaceLevel) ? 2.0 : amplitude;

                int surface = Math.max(0, (int)(surfaceLevel + islandBias + erosionFactor * surfaceNoise));

                ctx.setSurface(x, surface);

                if(surface > lowestSurfaceLevel) {
                    lowestSurfaceLevel = surface;
                }
            }

            // Init surface regions
            if(!surfaceRegionTypes.isEmpty() && !underwaterRegionTypes.isEmpty()) {
                int regionStart = 0;
                boolean isUnderwater = ctx.getSurface(0) >= oceanY;
                for(int x = 0; x < width; x++) {
                    int surface = ctx.getSurface(x);
                    // Transitioning to land
                    if(isUnderwater && surface < oceanY) {
                        isUnderwater = false;
                        regionStart = x;

                        if(x - regionStart >= 4) {
                            ctx.addSurfaceRegion(new SurfaceRegion(underwaterRegionTypes.next(ctx.getRandom()), regionStart, x));
                        }
                    }

                    // Transitioning to underwater
                    if(!isUnderwater && surface >= oceanY) {
                        isUnderwater = true;
                        regionStart = x;

                        if(x - regionStart >= 4) {
                            ctx.addSurfaceRegion(new SurfaceRegion(surfaceRegionTypes.next(ctx.getRandom()), regionStart, x));
                        }
                    }

                    // Region is getting too large
                    if(x - regionStart >= surfaceRegionSize) {
                        ctx.addSurfaceRegion(new SurfaceRegion(
                                (isUnderwater ? underwaterRegionTypes : surfaceRegionTypes).next(ctx.getRandom()),
                                regionStart,
                                x
                        ));

                        regionStart = x;
                    }
                }

                if(ctx.getWidth() - regionStart > 4) {
                    ctx.addSurfaceRegion(new SurfaceRegion(
                            (isUnderwater ? underwaterRegionTypes : surfaceRegionTypes).next(ctx.getRandom()),
                            regionStart,
                            ctx.getWidth()
                    ));
                }
            }
        } else {
            double amplitude = ctx.nextDouble() * (maxAmplitude - minAmplitude) + minAmplitude;

            for(int x = 0; x < width; x++) {
                int surface = (int)(SimplexNoise.noise2(ctx.getSeed(), x / 256.0, 0, 7) * amplitude) + surfaceLevel;
                ctx.setSurface(x, surface);

                if(surface > lowestSurfaceLevel) {
                    lowestSurfaceLevel = surface;
                }

                // Init surface regions
                if(!surfaceRegionTypes.isEmpty() && x % surfaceRegionSize == 0) {
                    int regionEnd = Math.min(width, x + surfaceRegionSize);
                    ctx.addSurfaceRegion(new SurfaceRegion(surfaceRegionTypes.next(ctx.getRandom()), x, regionEnd));
                }
            }
        }
        
        int heightBelowSurface = height - lowestSurfaceLevel;
        ctx.getZone().setDepths(
                (int)(heightBelowSurface * 0.25 + lowestSurfaceLevel),
                (int)(heightBelowSurface * 0.5 + lowestSurfaceLevel),
                (int)(heightBelowSurface * 0.75 + lowestSurfaceLevel));
        
        // Place the blocks!
        for(int x = 0; x < width; x++) {
            int surface = ctx.getSurface(x);
            
            // Only generate a thin layer for asteroids, cave gen will take care of the rest.
            for(int y = surface; y < (type == TerrainType.ASTEROIDS ? surface + 6 : height); y++) {
                ctx.updateBlock(x, y, Layer.FRONT, ctx.getEarthLayer(y));
                ctx.updateBlock(x, y, Layer.BASE, "base/earth");
            }
        }
        
        // Generate layer separators
        if(layerSeparator != null) {
            Item item = layerSeparator.getItem();
            int minThickness = layerSeparator.getMinThickness();
            int maxThickness = layerSeparator.getMaxThickness();
            double amplitude = ctx.nextDouble() * (layerSeparator.getMaxAmplitude() - layerSeparator.getMinAmplitude()) + layerSeparator.getMinAmplitude();
            
            for(int depth : ctx.getZone().getDepths()) {            
                for(int x = 0; x < width; x++) {
                    int start = (int)(SimplexNoise.noise2(ctx.getSeed(), x / 256.0, 0, 7) * amplitude) + depth - maxThickness / 2;
                    int size = ctx.nextInt(maxThickness - minThickness) + minThickness;
                    
                    for(int y = start; y < start + size; y++) {
                        ctx.updateBlock(x, y, item.getLayer(), item);
                    }
                }
            }
        }
    }
}
