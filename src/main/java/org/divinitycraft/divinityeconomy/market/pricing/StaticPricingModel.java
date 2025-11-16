package org.divinitycraft.divinityeconomy.market.pricing;

import org.divinitycraft.divinityeconomy.utils.Converter;

/**
 * Static Pricing Model - Fixed Price System
 * This model disables all dynamic pricing and uses a constant base price.
 * Prices remain fixed regardless of supply and demand changes.
 *
 * Characteristics:
 * - No price changes based on supply
 * - All items maintain a constant price of 15.0
 * - No inflation calculations
 * - Useful for servers that want stable, predictable pricing
 */
public class StaticPricingModel implements PricingModel {

    private double minItemValue;
    private double maxItemValue;

    private static final double STATIC_BASE_PRICE = 15.0;

    public StaticPricingModel(double minItemValue, double maxItemValue) {
        this.minItemValue = minItemValue;
        this.maxItemValue = maxItemValue;
    }

    @Override
    public double calculatePrice(double baseQuantity, double currentQuantity, double defaultMarketSize,
                                 double marketSize, double amount, double scale, boolean purchase,
                                 boolean wholeMarketInflation) {
        // Static pricing: just multiply the fixed price by the amount
        // No dynamic price changes during the transaction
        double unitPrice = getPrice(baseQuantity, currentQuantity, scale, 1.0);
        return unitPrice * amount;
    }

    @Override
    public double getPrice(double baseQuantity, double currentQuantity, double scale, double inflation) {
        // Return static price with only scale applied (no supply/demand or inflation)
        double staticPrice = STATIC_BASE_PRICE * scale;
        return fitPriceToConstraints(staticPrice);
    }

    @Override
    public int calculateStock(double baseQuantity, double price, double scale, double inflation) {
        // Since price doesn't change with stock in static mode,
        // return the base quantity as the "default" stock level
        return (int) baseQuantity;
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
        return "Static pricing model with no dynamic price changes. All items maintain a constant " +
               "base price of 15.0 regardless of supply and demand. Useful for servers that want " +
               "predictable, stable pricing.";
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
