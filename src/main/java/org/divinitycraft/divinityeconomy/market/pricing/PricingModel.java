package org.divinitycraft.divinityeconomy.market.pricing;

import org.divinitycraft.divinityeconomy.market.MarketableToken;

/**
 * Interface for different pricing models in the market system.
 * Implementations define how prices are calculated based on supply and demand.
 */
public interface PricingModel {

    /**
     * Calculates the price of an amount of items, factoring in dynamic supply changes.
     *
     * @param token             - The marketable token being priced
     * @param baseQuantity      - The base quantity of the item
     * @param defaultMarketSize - The default market size
     * @param marketSize        - The current market size
     * @param amount            - The amount of the item to buy/sell
     * @param scale             - The price scaling (e.g. tax)
     * @param purchase          - Whether this is a purchase (true) or sale (false)
     * @param wholeMarketInflation - Whether to apply market-wide inflation
     * @return double - Total price for the transaction
     */
    double calculatePrice(MarketableToken token, double baseQuantity, double defaultMarketSize,
                         double marketSize, double amount, double scale, boolean purchase,
                         boolean wholeMarketInflation);

    /**
     * Returns the price of a single unit at the current stock level.
     *
     * @param baseQuantity    - Base quantity of the product in the market
     * @param currentQuantity - Current quantity of the product in the market
     * @param scale           - Scaling factor to apply to the price
     * @param inflation       - Inflation factor in the market
     * @return double - The price of one unit
     */
    double getPrice(double baseQuantity, double currentQuantity, double scale, double inflation);

    /**
     * Returns the price of a single unit at the current stock level with custom elasticity.
     * For pricing models that support per-item elasticity (like V2).
     *
     * @param baseQuantity    - Base quantity of the product in the market
     * @param currentQuantity - Current quantity of the product in the market
     * @param scale           - Scaling factor to apply to the price
     * @param inflation       - Inflation factor in the market
     * @param elasticity      - The elasticity coefficient for this specific item
     * @return double - The price of one unit
     */
    default double getPrice(double baseQuantity, double currentQuantity, double scale, double inflation, double elasticity) {
        // Default implementation ignores elasticity and calls standard method
        return getPrice(baseQuantity, currentQuantity, scale, inflation);
    }

    /**
     * Calculates the stock level required to achieve a given price.
     *
     * @param baseQuantity - The base quantity for the item
     * @param price        - The desired price of the item
     * @param scale        - The scale of the price
     * @param inflation    - The inflation of the price
     * @return int - The level of stock required for this price
     */
    int calculateStock(double baseQuantity, double price, double scale, double inflation);

    /**
     * Gets the market-wide inflation factor.
     *
     * @param defaultMarketSize - Default quantity of materials in the market
     * @param actualMarketSize  - Actual quantity of materials in the market
     * @return double - The inflation factor
     */
    double getInflation(double defaultMarketSize, double actualMarketSize);

    /**
     * Returns the name of this pricing model.
     *
     * @return String - The model name
     */
    String getModelName();

    /**
     * Returns a description of how this pricing model works.
     *
     * @return String - The model description
     */
    String getDescription();
}
