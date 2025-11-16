package org.divinitycraft.divinityeconomy.market.pricing;

import org.divinitycraft.divinityeconomy.market.MarketableToken;
import org.divinitycraft.divinityeconomy.utils.Converter;

/**
 * Static Pricing Model - Fixed Price System
 * This model disables all dynamic pricing and uses configured base prices.
 * Prices remain fixed regardless of supply and demand changes.
 *
 * Characteristics:
 * - No price changes based on supply
 * - Each item maintains its configured PRICE from the config files
 * - No inflation calculations
 * - Useful for servers that want stable, predictable pricing
 *
 * Note: The basePrice parameter in getPrice() is used as the item's fixed price.
 * This allows each item to have its own configured price from the YAML files.
 */
public class StaticPricingModel implements PricingModel {

    private double minItemValue;
    private double maxItemValue;

    public StaticPricingModel(double minItemValue, double maxItemValue) {
        this.minItemValue = minItemValue;
        this.maxItemValue = maxItemValue;
    }

    @Override
    public double calculatePrice(MarketableToken token, double baseQuantity, double defaultMarketSize,
                                 double marketSize, double amount, double scale, boolean purchase,
                                 boolean wholeMarketInflation) {
        // Static pricing: just multiply the fixed price by the amount and scale
        // No dynamic price changes during the transaction
        double price = token.getPrice() * amount * scale;
        return fitPriceToConstraints(price);
    }

    @Override
    public double getPrice(double baseQuantity, double currentQuantity, double scale, double inflation) {
        // Return static price with only scale applied (no supply/demand or inflation)
        // baseQuantity is repurposed to pass the item's base price from config
        double staticPrice = baseQuantity * scale;
        return fitPriceToConstraints(staticPrice);
    }

    @Override
    public int calculateStock(double baseQuantity, double price, double scale, double inflation) {
        // Since price doesn't change with stock in static mode,
        // we can't meaningfully calculate a stock level from price
        // Return 0 to indicate stock calculation is not applicable for static pricing
        return 0;
    }

    @Override
    public double getInflation(double defaultMarketSize, double actualMarketSize) {
        // No inflation in static pricing
        return 1.0;
    }

    @Override
    public String getModelName() {
        return "Static";
    }

    @Override
    public String getDescription() {
        return "Static pricing model with no dynamic price changes. Each item maintains its " +
               "configured PRICE from the config files regardless of supply and demand. Useful for " +
               "servers that want predictable, stable pricing with custom prices per item.";
    }

    /**
     * Returns the price of an item fit to the max and min constraints
     * @param price - The price to constrain
     * @return double - The constrained price
     */
    private double fitPriceToConstraints(double price) {
        return Converter.constrainDouble(price, this.minItemValue, this.maxItemValue);
    }

    /**
     * Updates the min and max item value constraints
     * @param minItemValue - The minimum item value
     * @param maxItemValue - The maximum item value
     */
    public void updateConstraints(double minItemValue, double maxItemValue) {
        this.minItemValue = minItemValue;
        this.maxItemValue = maxItemValue;
    }
}
