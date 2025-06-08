package brainwine.gameserver.androidshop;

import brainwine.gameserver.player.Player;
import brainwine.gameserver.player.Skill;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AndroidShopAdjustments {
    private double[] sellPriceAdjustment = new double[] { 1.0, 1.0, 0.98, 0.96, 0.94, 0.92, 0.90, 0.88, 0.86, 0.84, 0.82, 0.80, 0.78, 0.76, 0.74, 0.72 };
    private double[] buyPriceAdjustment = new double[] { 1.00, 1.00, 1.01, 1.02, 1.03, 1.04, 1.05, 1.06, 1.07, 1.08, 1.09, 1.10, 1.11, 1.12, 1.13, 1.14 };
    private double[] maxPrice = new double[] { 25, 50, 75, 100, 250, 500, 750, 1000, 1500, 2000, 2500, 3000, 5000 };

    private static double[] makeLevelLookupArray(Map<String, Object> config, double startingValue) {
        double[] result = new double[Player.MAX_SKILL_LEVEL + 1];
        result[0] = startingValue;
        for(int level = 1; level < result.length; level++) {
            Object val = config.get(Integer.toString(level));
            result[level] = result[level - 1];
            if(val != null) try {
                result[level] = Double.parseDouble(val.toString());
            } catch(NumberFormatException ignored) {}
        }

        return result;
    }

    public AndroidShopAdjustments() {}

    @JsonCreator
    public AndroidShopAdjustments(
            @JsonSetter(value = "sell_price_adjustment", nulls = Nulls.SKIP) Map<String, Object> sellPriceAdjustment,
            @JsonSetter(value = "buy_price_adjustment", nulls = Nulls.SKIP) Map<String, Object> buyPriceAdjustment,
            @JsonSetter(value = "max_price", nulls = Nulls.SKIP) Map<String, Object> maxPrice
    ) {
        if(sellPriceAdjustment != null) {
            this.sellPriceAdjustment = makeLevelLookupArray(sellPriceAdjustment, 1.00);
        }
        if(buyPriceAdjustment != null) {
            this.buyPriceAdjustment = makeLevelLookupArray(buyPriceAdjustment, 1.00);
        }
        if(maxPrice != null) {
            this.maxPrice = makeLevelLookupArray(maxPrice, 25);
        }
    }

    public int getAdjustedSellPrice(Player player, int price) {
        return (int)Math.ceil(sellPriceAdjustment[player.getTotalSkillLevel(Skill.BARTER)] * price);
    }

    public int getAdjustedBuyPrice(Player player, int price) {
        return (int)Math.ceil(buyPriceAdjustment[player.getTotalSkillLevel(Skill.BARTER)] * price);
    }

    public int getMaxPrice(Player player) {
        return (int)maxPrice[player.getTotalSkillLevel(Skill.BARTER)];
    }
}
