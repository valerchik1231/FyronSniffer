package ru.valerchik.fyronsniffer.command;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import ru.valerchik.fyronsniffer.FyronSniffer;
import ru.valerchik.fyronsniffer.utils.LootEntry;
import ru.valerchik.fyronsniffer.utils.NumberParser;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Collection;
import java.util.Locale;

public final class SnifferCommand implements CommandExecutor {

    private static final DecimalFormat CHANCE_FORMAT = new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.US));

    private final FyronSniffer plugin;

    public SnifferCommand(FyronSniffer plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] arguments) {
        if (arguments.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (arguments[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> reload(sender);
            case "list" -> list(sender);
            case "add" -> add(sender, arguments);
            case "remove" -> remove(sender, arguments);
            default -> sendHelp(sender);
        }
        return true;
    }

    private void reload(CommandSender sender) {
        this.plugin.reloadAll();
        this.plugin.getConfigs().send(sender, "messages.reload");
    }

    private void list(CommandSender sender) {
        Collection<LootEntry> lootEntries = this.plugin.getLootRegistry().getEntries();
        if (lootEntries.isEmpty()) {
            this.plugin.getConfigs().send(sender, "messages.list-empty");
            return;
        }

        this.plugin.getConfigs().send(sender, "messages.items-list");
        for (LootEntry lootEntry : lootEntries) {
            this.plugin.getConfigs().send(
                    sender,
                    "messages.list-line",
                    "item", lootEntry.identifier(),
                    "material", lootEntry.itemStack().getType().name(),
                    "chance", CHANCE_FORMAT.format(lootEntry.chance()),
                    "amount", lootEntry.minimumAmount() == lootEntry.maximumAmount()
                            ? String.valueOf(lootEntry.minimumAmount())
                            : lootEntry.minimumAmount() + "-" + lootEntry.maximumAmount()
            );
        }
    }

    private void add(CommandSender sender, String[] arguments) {
        if (arguments.length < 3) {
            sendHelp(sender);
            return;
        }

        String itemIdentifier = arguments[1];
        if (this.plugin.getLootRegistry().contains(itemIdentifier)) {
            this.plugin.getConfigs().send(sender, "messages.item-already-exists", "item", itemIdentifier);
            return;
        }

        Double chance = NumberParser.parseChance(arguments[2]);
        if (chance == null) {
            this.plugin.getConfigs().send(sender, "messages.invalid-chance");
            return;
        }

        int currentIndex = 3;
        Material material = null;
        if (arguments.length > currentIndex) {
            Material parsedMaterial = Material.matchMaterial(arguments[currentIndex]);
            if (parsedMaterial != null && !parsedMaterial.isAir()) {
                material = parsedMaterial;
                currentIndex++;
            } else if (!NumberParser.isInteger(arguments[currentIndex])) {
                this.plugin.getConfigs().send(sender, "messages.invalid-material", "material", arguments[currentIndex]);
                return;
            }
        }

        Player player = sender instanceof Player onlinePlayer ? onlinePlayer : null;
        int defaultAmount = 1;
        if (material == null && player != null && !player.getInventory().getItemInMainHand().getType().isAir()) {
            defaultAmount = Math.max(1, player.getInventory().getItemInMainHand().getAmount());
        }

        int minimumAmount = defaultAmount;
        if (arguments.length > currentIndex) {
            Integer parsedMinimumAmount = NumberParser.parsePositiveInteger(arguments[currentIndex]);
            if (parsedMinimumAmount == null) {
                this.plugin.getConfigs().send(sender, "messages.invalid-number");
                return;
            }
            minimumAmount = parsedMinimumAmount;
            currentIndex++;
        }

        int maximumAmount = minimumAmount;
        if (arguments.length > currentIndex) {
            Integer parsedMaximumAmount = NumberParser.parsePositiveInteger(arguments[currentIndex]);
            if (parsedMaximumAmount == null) {
                this.plugin.getConfigs().send(sender, "messages.invalid-number");
                return;
            }
            maximumAmount = parsedMaximumAmount;
        }

        if (material != null) {
            this.plugin.getLootRegistry().add(itemIdentifier, material, chance, minimumAmount, maximumAmount);
            this.plugin.getConfigs().send(sender, "messages.item-added", "item", itemIdentifier);
            return;
        }

        if (player == null) {
            this.plugin.getConfigs().send(sender, "messages.only-player");
            return;
        }

        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand.getType().isAir()) {
            this.plugin.getConfigs().send(sender, "messages.empty-hand");
            return;
        }

        this.plugin.getLootRegistry().add(itemIdentifier, itemInHand.clone(), chance, minimumAmount, maximumAmount);
        this.plugin.getConfigs().send(sender, "messages.item-added", "item", itemIdentifier);
    }

    private void remove(CommandSender sender, String[] arguments) {
        if (arguments.length < 2) {
            sendHelp(sender);
            return;
        }

        String itemIdentifier = arguments[1];
        if (!this.plugin.getLootRegistry().remove(itemIdentifier)) {
            this.plugin.getConfigs().send(sender, "messages.item-not-found", "item", itemIdentifier);
            return;
        }

        this.plugin.getConfigs().send(sender, "messages.item-removed", "item", itemIdentifier);
    }

    private void sendHelp(CommandSender sender) {
        this.plugin.getConfigs().sendList(sender, "messages.help");
    }
}