package ru.valerchik.fyronsniffer.utils;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import ru.valerchik.fyronsniffer.FyronSniffer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public final class LootRegistry {

    private final FyronSniffer plugin;
    private final Map<String, LootEntry> lootEntries = new LinkedHashMap<>();
    private final List<LootEntry> lootEntriesView = new ArrayList<>();

    public LootRegistry(FyronSniffer plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        this.lootEntries.clear();
        this.lootEntriesView.clear();

        ConfigurationSection lootSection = this.plugin.getConfig().getConfigurationSection("loot");
        if (lootSection == null) {
            return;
        }

        for (String identifier : lootSection.getKeys(false)) {
            ConfigurationSection itemSection = lootSection.getConfigurationSection(identifier);
            readItem(identifier, itemSection).ifPresent(item -> this.lootEntries.put(normalize(identifier), item));
        }

        this.lootEntriesView.addAll(this.lootEntries.values());
    }

    public boolean contains(String identifier) {
        return this.lootEntries.containsKey(normalize(identifier));
    }

    public Collection<LootEntry> getEntries() {
        return this.lootEntries.values().stream()
                .sorted(Comparator.comparing(LootEntry::identifier))
                .toList();
    }

    public Optional<ItemStack> randomItem() {
        if (this.lootEntriesView.isEmpty()) {
            return Optional.empty();
        }

        List<LootEntry> passed = new ArrayList<>();

        for (LootEntry entry : this.lootEntriesView) {
            if (ThreadLocalRandom.current().nextDouble(100.0D) <= entry.chance()) {
                passed.add(entry);
            }
        }

        if (passed.isEmpty()) {
            return Optional.empty();
        }

        LootEntry selected = passed.get(ThreadLocalRandom.current().nextInt(passed.size()));
        return Optional.of(createItemStack(selected));
    }

    public Optional<ItemStack> getOnlyCustomItem() {
        if (this.lootEntriesView.isEmpty()) {
            return Optional.empty();
        }

        double totalWeight = 0.0D;
        for (LootEntry entry : this.lootEntriesView) {
            totalWeight += entry.chance();
        }

        if (totalWeight <= 0.0D) {
            LootEntry random = this.lootEntriesView.get(
                    ThreadLocalRandom.current().nextInt(this.lootEntriesView.size())
            );
            return Optional.of(createItemStack(random));
        }

        double random = ThreadLocalRandom.current().nextDouble(totalWeight);
        double current = 0.0D;

        for (LootEntry entry : this.lootEntriesView) {
            current += entry.chance();
            if (random <= current) {
                return Optional.of(createItemStack(entry));
            }
        }

        LootEntry last = this.lootEntriesView.get(this.lootEntriesView.size() - 1);
        return Optional.of(createItemStack(last));
    }

    public void add(String identifier, ItemStack itemStack, double chance, int minimumAmount, int maximumAmount) {
        String normalizedIdentifier = normalize(identifier);
        int safeMinimumAmount = Math.max(1, minimumAmount);
        int safeMaximumAmount = Math.max(safeMinimumAmount, maximumAmount);
        double safeChance = Math.max(0.0D, Math.min(100.0D, chance));

        String basePath = "loot." + normalizedIdentifier;
        this.plugin.getConfig().set(basePath + ".material", null);
        this.plugin.getConfig().set(basePath + ".item-stack", itemStack.clone());
        this.plugin.getConfig().set(basePath + ".chance", safeChance);
        this.plugin.getConfig().set(basePath + ".minimum-amount", safeMinimumAmount);
        this.plugin.getConfig().set(basePath + ".maximum-amount", safeMaximumAmount);
        this.plugin.saveConfig();

        this.lootEntries.put(normalizedIdentifier, new LootEntry(normalizedIdentifier, itemStack.clone(), safeChance, safeMinimumAmount, safeMaximumAmount));
        refreshView();
    }

    public void add(String identifier, Material material, double chance, int minimumAmount, int maximumAmount) {
        String normalizedIdentifier = normalize(identifier);
        int safeMinimumAmount = Math.max(1, minimumAmount);
        int safeMaximumAmount = Math.max(safeMinimumAmount, maximumAmount);
        double safeChance = Math.max(0.0D, Math.min(100.0D, chance));

        String basePath = "loot." + normalizedIdentifier;
        this.plugin.getConfig().set(basePath + ".item-stack", null);
        this.plugin.getConfig().set(basePath + ".material", material.name());
        this.plugin.getConfig().set(basePath + ".chance", safeChance);
        this.plugin.getConfig().set(basePath + ".minimum-amount", safeMinimumAmount);
        this.plugin.getConfig().set(basePath + ".maximum-amount", safeMaximumAmount);
        this.plugin.saveConfig();

        this.lootEntries.put(normalizedIdentifier, new LootEntry(normalizedIdentifier, new ItemStack(material), safeChance, safeMinimumAmount, safeMaximumAmount));
        refreshView();
    }

    public boolean remove(String identifier) {
        String normalizedIdentifier = normalize(identifier);
        if (this.lootEntries.remove(normalizedIdentifier) == null) {
            return false;
        }

        this.plugin.getConfig().set("loot." + normalizedIdentifier, null);
        this.plugin.saveConfig();
        refreshView();
        return true;
    }

    private Optional<LootEntry> readItem(String identifier, ConfigurationSection itemSection) {
        if (itemSection == null) {
            return Optional.empty();
        }

        ItemStack itemStack = itemSection.getItemStack("item-stack");
        if (itemStack == null || itemStack.getType().isAir()) {
            String materialName = itemSection.getString("material", "");
            Material material = Material.matchMaterial(materialName);
            if (material == null || material.isAir()) {
                return Optional.empty();
            }
            itemStack = new ItemStack(material);
        }

        double chance = Math.max(0.0D, Math.min(100.0D, itemSection.getDouble("chance", 0.0D)));
        int minimumAmount = Math.max(1, itemSection.getInt("minimum-amount", itemStack.getAmount()));
        int maximumAmount = Math.max(minimumAmount, itemSection.getInt("maximum-amount", minimumAmount));
        return Optional.of(new LootEntry(normalize(identifier), itemStack, chance, minimumAmount, maximumAmount));
    }

    private ItemStack createItemStack(LootEntry lootEntry) {
        ItemStack itemStack = lootEntry.itemStack().clone();
        itemStack.setAmount(randomAmount(lootEntry.minimumAmount(), lootEntry.maximumAmount()));
        return itemStack;
    }

    private int randomAmount(int minimumAmount, int maximumAmount) {
        if (minimumAmount >= maximumAmount) {
            return minimumAmount;
        }
        return ThreadLocalRandom.current().nextInt(minimumAmount, maximumAmount + 1);
    }

    private void refreshView() {
        this.lootEntriesView.clear();
        this.lootEntriesView.addAll(this.lootEntries.values());
    }

    private String normalize(String identifier) {
        return identifier.toLowerCase(Locale.ROOT);
    }
}