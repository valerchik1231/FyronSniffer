package ru.valerchik.fyronsniffer.utils;

import org.bukkit.inventory.ItemStack;

public record LootEntry(
        String identifier,
        ItemStack itemStack,
        double chance,
        int minimumAmount,
        int maximumAmount
) {
}
