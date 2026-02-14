package org.divinitycraft.divinityeconomy.market.items;

import org.divinitycraft.divinityeconomy.DEPlugin;
import org.divinitycraft.divinityeconomy.market.TokenManager;
import org.divinitycraft.divinityeconomy.market.TokenValueResponse;
import org.divinitycraft.divinityeconomy.market.items.enchants.EnchantManager;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import javax.annotation.Nonnull;
import java.util.*;

public abstract class ItemManager extends TokenManager {

    /**
     * Constructor You will likely need to call loadMaterials and loadAliases to
     * populate the aliases and items with data from the program
     *
     * @param main      - The plugin
     * @param itemFile
     * @param aliasFile
     * @param itemMap
     */
    public ItemManager(DEPlugin main, String itemFile, String aliasFile, Map<String, ? extends MarketableItem> itemMap) {
        super(main, itemFile, aliasFile, itemMap);
    }

    /**
     * Returns if the item is named
     * @param itemStack - The item stack to check
     * @return If the item is named
     */
    public static boolean itemIsNamed(@Nonnull ItemStack itemStack) {
        ItemMeta meta = itemStack.getItemMeta();
        // 修复: 添加 null 检查，防止 ItemMeta 为 null 时抛出 NPE
        if (meta == null) {
            return false;
        }
        return meta.hasDisplayName();
    }

    /**
     * Returns if the item has lore
     * @param itemStack - The item stack to check
     * @return If the item has lore
     */
    public static boolean itemHasLore(@Nonnull ItemStack itemStack) {
        ItemMeta meta = itemStack.getItemMeta();
        // 修复: 添加 null 检查
        if (meta == null) {
            return false;
        }
        return meta.hasLore();
    }

    /**
     * Removes all enchanted items from the given array
     * @param itemStacks - The item stacks to filter
     * @return The filtered array without enchanted items
     */
    public static ItemStack[] removeEnchantedItems(ItemStack[] itemStacks) {
        ArrayList<ItemStack> nonEnchanted = new ArrayList<>();
        Arrays.stream(itemStacks).forEach(stack -> {
            // 修复: 先检查 stack 是否为 null，以及 ItemMeta 是否为 null
            if (stack == null) {
                return; // 跳过 null 的 ItemStack
            }
            ItemMeta itemMeta = stack.getItemMeta();
            if (itemMeta == null) {
                // 没有 ItemMeta 说明不可能有附魔，直接加入非附魔列表
                nonEnchanted.add(stack);
                return;
            }
            if (EnchantManager.getEnchantments(stack).isEmpty()) {
                nonEnchanted.add(stack);
            } else {
                if (itemMeta instanceof EnchantmentStorageMeta meta) {
                    if (meta.getStoredEnchants().isEmpty()) {
                        nonEnchanted.add(stack);
                    }
                }
            }
        });
        return nonEnchanted.toArray(new ItemStack[0]);
    }

    /**
     * Removes all items that are named or have lore
     * @param itemStacks
     * @return
     */
    public static ItemStack[] removeNamedItems(ItemStack[] itemStacks) {
        ArrayList<ItemStack> nonNamed = new ArrayList<>();
        Arrays.stream(itemStacks).forEach(stack -> {
            // 修复: 添加 null 检查
            if (stack == null) {
                return;
            }
            if (!(itemIsNamed(stack) || itemHasLore(stack))) {
                nonNamed.add(stack);
            }
        });
        return nonNamed.toArray(new ItemStack[0]);
    }

    /**
     * Clones the given array and returns a non-related array
     *
     * @param itemStacks - The items to clone
     */
    public static ItemStack[] cloneItems(ItemStack[] itemStacks) {
        ArrayList<ItemStack> clones = new ArrayList<>();
        Arrays.stream(itemStacks).forEach(stack -> {
            if (stack != null) {
                clones.add(clone(stack));
            }
        });
        return clones.toArray(new ItemStack[0]);
    }

    public static ItemStack clone(ItemStack itemStack) {
        // Create a new item stack
        ItemStack newItemStack = new ItemStack(itemStack.getType(), itemStack.getAmount());

        // Set item meta (修复: 添加 null 检查)
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            newItemStack.setItemMeta(meta);
        }

        // Add enchantments
        newItemStack.addUnsafeEnchantments(EnchantManager.getEnchantments(itemStack));
        return newItemStack;
    }

    /**
     * Calculates the total count of all materials in the ItemStacks
     *
     * @param iStacks - The array of item stacks
     * @return int - Total count of materials
     */
    public static int getMaterialCount(ItemStack[] iStacks) {
        int count = 0;
        for (ItemStack iStack : iStacks) {
            if (iStack != null) {
                count += iStack.getAmount();
            }
        }
        return count;
    }

    /**
     * Checks if the given item has been assigned a UUID
     * @param itemStack
     * @return
     */
    public static boolean itemIsUnidentified(ItemStack itemStack) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        // 修复: 添加 null 检查
        if (itemMeta == null) {
            return false;
        }
        PersistentDataContainer container = itemMeta.getPersistentDataContainer();
        return container.has(NamespacedKey.minecraft("de-uuid"), PersistentDataType.STRING);
    }

    /**
     * Returns the UUID of the given item stack
     */
    public static String getIdentity(ItemStack itemStack) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        // 修复: 添加 null 检查
        if (itemMeta == null) {
            return null;
        }
        PersistentDataContainer container = itemMeta.getPersistentDataContainer();
        return container.get(NamespacedKey.minecraft("de-uuid"), PersistentDataType.STRING);
    }

    /**
     * Identifies the given item stack with a UUID
     * @param itemStack
     */
    public static void generateIdentity(ItemStack itemStack) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        // 修复: 添加 null 检查
        if (itemMeta == null) {
            return;
        }
        PersistentDataContainer container = itemMeta.getPersistentDataContainer();
        container.set(NamespacedKey.minecraft("de-uuid"), PersistentDataType.STRING, UUID.randomUUID().toString());
        itemStack.setItemMeta(itemMeta);
    }

    public static String getOrSetIdentity(ItemStack itemStack) {
        if (itemIsUnidentified(itemStack)) {
            return getIdentity(itemStack);
        } else {
            generateIdentity(itemStack);
            return getIdentity(itemStack);
        }
    }

    public static List<ItemStack> removeIdentity(List<ItemStack> itemStacks) {
        List<ItemStack> nonIdentified = new ArrayList<>();
        for (ItemStack itemStack : itemStacks) {
            if (itemStack != null) {
                nonIdentified.add(removeIdentity(itemStack));
            }
        }
        return nonIdentified;
    }

    public static ItemStack removeIdentity(ItemStack itemStack) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        // 修复: 添加 null 检查
        if (itemMeta == null) {
            return itemStack;
        }
        PersistentDataContainer container = itemMeta.getPersistentDataContainer();
        container.remove(NamespacedKey.minecraft("de-uuid"));
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    /**
     * Returns the names and aliases for the itemstack given
     */
    public abstract Set<String> getItemNames(ItemStack itemStack);

    /**
     * Returns the names and aliases for the itemstack given starting with startswith
     */
    public abstract Set<String> getItemNames(ItemStack itemStack, String startswith);

    /**
     * Returns the combined sell value of all the items given
     */
    public abstract TokenValueResponse getSellValue(ItemStack[] itemStacks);

    /**
     * Returns the sell value for a single type of items.
     */
    public abstract TokenValueResponse getSellValue(ItemStack itemStack, int amount);

    /**
     * Returns the price of buying the given items.
     */
    public abstract TokenValueResponse getBuyValue(ItemStack[] itemStacks);

    /**
     * Returns the buy value for a single type of items.
     */
    public abstract TokenValueResponse getBuyValue(ItemStack itemStack, int amount);
}
