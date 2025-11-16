package org.divinitycraft.divinityeconomy.market.items.materials.potion;

import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;
import org.divinitycraft.divinityeconomy.DEPlugin;
import org.divinitycraft.divinityeconomy.config.Setting;
import org.divinitycraft.divinityeconomy.lang.LangEntry;
import org.divinitycraft.divinityeconomy.market.items.materials.MarketableMaterial;
import org.divinitycraft.divinityeconomy.market.items.materials.MaterialManager;
import org.divinitycraft.divinityeconomy.market.items.materials.MaterialValueResponse;
import org.divinitycraft.divinityeconomy.utils.Converter;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PotionManager extends MaterialManager {
    public static final String PotionFile = "potions.yml";
    public static final String PotionAliasFile = "potionAliases.yml";

    /**
     * Constructor You will likely need to call loadMaterials and loadAliases to
     * populate the aliases and items with data from the program
     *
     * @param main - The plugin
     */
    public PotionManager(DEPlugin main) {
        super(main, PotionFile, PotionAliasFile, new ConcurrentHashMap<>());
    }

    @Override
    public void init() {
        this.saveMessagesDisabled = this.getConfMan().getBoolean(Setting.IGNORE_SAVE_MESSAGE_BOOLEAN);
        this.buyScale = this.getConfMan().getDouble(Setting.MARKET_POTIONS_BUY_TAX_FLOAT);
        this.sellScale = this.getConfMan().getDouble(Setting.MARKET_POTIONS_SELL_TAX_FLOAT);
        this.baseQuantity = this.getConfMan().getInt(Setting.MARKET_POTIONS_BASE_QUANTITY_INTEGER);
        this.wholeMarketInflation = this.getConfMan().getBoolean(Setting.MARKET_POTIONS_WHOLE_MARKET_INF_BOOLEAN);
        this.maxItemValue = this.getConfMan().getDouble(Setting.MARKET_MAX_ITEM_VALUE_DOUBLE);
        this.ignoreNamedItems = this.getConfMan().getBoolean(Setting.MARKET_POTIONS_IGNORE_NAMED_ITEMS_BOOLEAN);
        if (this.maxItemValue < 0) {
            this.maxItemValue = Double.MAX_VALUE;
        }
        this.minItemValue = this.getConfMan().getDouble(Setting.MARKET_MIN_ITEM_VALUE_DOUBLE);
        if (this.minItemValue < 0) {
            this.minItemValue = Double.MIN_VALUE;
        }

        // Initialize pricing model
        String pricingModelName = this.getConfMan().getString(Setting.MARKET_POTIONS_PRICING_MODEL_STRING);
        this.initializePricingModel(pricingModelName);

        int timer = Converter.getTicks(this.getConfMan().getInt(Setting.MARKET_SAVE_TIMER_INTEGER));
        this.saveTimer = new BukkitRunnable() {
            @Override
            public void run() {
                saveItems();
            }
        };
        this.saveTimer.runTaskTimerAsynchronously(getMain(), timer, timer);
        this.loadItems();
        this.loadAliases();
        // this.checkLoadedItems(); - This is for internal debugging only. Hi! :)
        this.getMarkMan().addManager(this);
    }

    @Override
    public void deinit() {
        this.saveTimer.cancel();
        this.saveItems();
        this.getMarkMan().removeManager(this);
    }

    @Override
    public String getType() {
        return "POTION";
    }


    @Override
    public Set<String> getLocalKeys() {
        Set<String> entityKeys = new HashSet<>();
        for (PotionType type : PotionType.values()) {
            try {
                NamespacedKey key = type.getKey();
                entityKeys.add(key.getKey().toUpperCase());
            } catch (Exception ignored) {}
        }
        return entityKeys;
    }


    /**
     * Returns the sell value for a single stack of items.
     *
     * @param itemStack - The itemStack to get the value of
     * @return MaterialValue - The price of the itemstack if no errors occurred.
     */
    @Override
    public MaterialValueResponse getSellValue(ItemStack itemStack, int amount) {
        // Create value response
        MaterialValueResponse response = new MaterialValueResponse(EconomyResponse.ResponseType.SUCCESS, null);

        // Get the item data
        MarketablePotion potionData = (MarketablePotion) this.getItem(itemStack);


        // If the item data is null, return 0
        if (potionData == null)
            return (MaterialValueResponse) response.setFailure(LangEntry.MARKET_ItemCannotBeFound.get(getMain(), itemStack.getType().name()));


        // Get value and add token to response
        double value = this.calculatePrice(potionData, amount, this.sellScale, false);
        response.addToken(potionData, amount, value, new ItemStack[]{itemStack});


        // Check item is allowed
        if (!potionData.getAllowed())
            return (MaterialValueResponse) response.setFailure(LangEntry.MARKET_ItemIsBanned.get(getMain(), potionData.getName()));


        // Check if the market is saturated
        if (value <= 0)
            return (MaterialValueResponse) response.setFailure(LangEntry.MARKET_ItemIsWorthless.get(getMain(), potionData.getName()));


        // Return the response
        return response;
    }

    /**
     * Returns the value of an itemstack
     *
     * @param itemStack - The item stack to get the value of
     * @return MaterialValue
     */
    @Override
    public MaterialValueResponse getBuyValue(ItemStack itemStack, int amount) {
        // Create value response
        MaterialValueResponse response = new MaterialValueResponse(EconomyResponse.ResponseType.SUCCESS, null);


        // Get the item data
        MarketablePotion potionData = (MarketablePotion) this.getItem(itemStack);


        // If the item data is null, return 0
        if (potionData == null)
            return (MaterialValueResponse) response.setFailure(LangEntry.MARKET_ItemCannotBeFound.get(getMain(), itemStack.getType().name()));


        // Get value and add token to response
        double value = this.calculatePrice(potionData, amount, this.buyScale, true);
        response.addToken(potionData, amount, value, new ItemStack[]{itemStack});


        // Check item is allowed
        if (!potionData.getAllowed())
            return (MaterialValueResponse) response.setFailure(LangEntry.MARKET_ItemIsBanned.get(getMain(), potionData.getName()));


        // Check if the market is saturated
        if (value <= 0)
            return (MaterialValueResponse) response.setFailure(LangEntry.MARKET_ItemIsWorthless.get(getMain(), potionData.getName()));


        // Return the response
        return response;
    }

    /**
     * Returns the item given as base class DivinityItem is abstract and cannot be instantiated
     *
     * @param data
     * @param defaultData
     * @return
     */
    @Override
    public MarketableMaterial loadItem(String ID, ConfigurationSection data, ConfigurationSection defaultData) {
        return new MarketablePotion(getMain(), this, ID, data, defaultData);
    }
}
