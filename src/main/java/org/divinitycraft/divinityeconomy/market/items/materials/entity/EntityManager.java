package org.divinitycraft.divinityeconomy.market.items.materials.entity;

import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
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

public class EntityManager extends MaterialManager {
    // Stores the default items.json file location
    public static final String entitiesFile = "entities.yml";
    public static final String aliasesFile = "entityAliases.yml";

    /**
     * Constructor You will likely need to call loadMaterials and loadAliases to
     * populate the aliases and items with data from the program
     *
     * @param main - The plugin
     */
    public EntityManager(DEPlugin main) {
        super(main, entitiesFile, aliasesFile, new ConcurrentHashMap<String, MarketableEntity>());
        new SpawnerPlaceListener(main);
    }

    @Override
    public void init() {
        this.saveMessagesDisabled = this.getConfMan().getBoolean(Setting.IGNORE_SAVE_MESSAGE_BOOLEAN);
        this.buyScale = this.getConfMan().getDouble(Setting.MARKET_ENTITIES_BUY_TAX_FLOAT);
        this.sellScale = this.getConfMan().getDouble(Setting.MARKET_ENTITIES_SELL_TAX_FLOAT);
        this.baseQuantity = this.getConfMan().getInt(Setting.MARKET_ENTITIES_BASE_QUANTITY_INTEGER);
        this.wholeMarketInflation = this.getConfMan().getBoolean(Setting.MARKET_ENTITIES_WHOLE_MARKET_INF_BOOLEAN);
        this.maxItemValue = this.getConfMan().getDouble(Setting.MARKET_MAX_ITEM_VALUE_DOUBLE);
        this.ignoreNamedItems = this.getConfMan().getBoolean(Setting.MARKET_ENTITIES_IGNORE_NAMED_ITEMS_BOOLEAN);
        if (this.maxItemValue < 0) {
            this.maxItemValue = Double.MAX_VALUE;
        }
        this.minItemValue = this.getConfMan().getDouble(Setting.MARKET_MIN_ITEM_VALUE_DOUBLE);
        if (this.minItemValue < 0) {
            this.minItemValue = Double.MIN_VALUE;
        }

        // Initialize pricing model
        String pricingModelName = this.getConfMan().getString(Setting.MARKET_ENTITIES_PRICING_MODEL_STRING);
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
        return "ENTITY";
    }


    @Override
    public Set<String> getLocalKeys() {
        Set<String> entityKeys = new HashSet<>();
        for (EntityType type : EntityType.values()) {
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
        MarketableEntity entityData = (MarketableEntity) this.getItem(itemStack);


        // If the item data is null, return 0
        if (entityData == null)
            return (MaterialValueResponse) response.setFailure(LangEntry.MARKET_ItemCannotBeFound.get(getMain(), itemStack.getType().name()));


        // Get value and add token to response
        double value = this.calculatePrice(entityData, amount, this.sellScale, false);
        response.addToken(entityData, amount, value, new ItemStack[]{itemStack});


        // Check item is allowed
        if (!entityData.getAllowed())
            return (MaterialValueResponse) response.setFailure(LangEntry.MARKET_ItemIsBanned.get(getMain(), entityData.getName()));


        // If value is less than 0, return 0
        if (value <= 0)
            return (MaterialValueResponse) response.setFailure(LangEntry.MARKET_ItemIsWorthless.get(getMain(), entityData.getName()));


        // Return value
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
        // Create response
        MaterialValueResponse response = new MaterialValueResponse(EconomyResponse.ResponseType.SUCCESS, null);

        // Get the item data
        MarketableEntity entityData = (MarketableEntity) this.getItem(itemStack);


        // If the item data is null, return 0
        if (entityData == null)
            return (MaterialValueResponse) response.setFailure(LangEntry.MARKET_ItemCannotBeFound.get(getMain(), itemStack.getType().name()));


        // Get value and add token to response
        double value = this.calculatePrice(entityData, amount, this.buyScale, true);
        response.addToken(entityData, amount, value, new ItemStack[]{itemStack});


        // Check if item is banned
        if (!entityData.getAllowed())
            return (MaterialValueResponse) response.setFailure(LangEntry.MARKET_ItemIsBanned.get(getMain(), entityData.getName()));


        // If value is less than 0, return 0
        if (value <= 0)
            return (MaterialValueResponse) response.setFailure(LangEntry.MARKET_ItemIsWorthless.get(getMain(), entityData.getName()));


        // Return value
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
        return new MarketableEntity(getMain(), this, ID, data, defaultData);
    }
}
