package ru.valerchik.fyronsniffer.command;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import ru.valerchik.fyronsniffer.FyronSniffer;
import ru.valerchik.fyronsniffer.utils.LootEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SnifferTabCompleter implements TabCompleter {

    private final FyronSniffer plugin;

    public SnifferTabCompleter(FyronSniffer plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] arguments) {
        if (arguments.length == 1) {
            return copyMatches(arguments[0], List.of("reload", "list", "add", "remove"));
        }

        String subCommand = arguments[0].toLowerCase(Locale.ROOT);
        if (subCommand.equals("remove") && arguments.length == 2) {
            List<String> identifiers = this.plugin.getLootRegistry().getEntries().stream()
                    .map(LootEntry::identifier)
                    .toList();
            return copyMatches(arguments[1], identifiers);
        }

        if (subCommand.equals("add") && arguments.length == 4) {
            List<String> materials = new ArrayList<>();
            for (Material material : Material.values()) {
                if (!material.isLegacy() && material.isItem()) {
                    materials.add(material.name());
                }
            }
            return copyMatches(arguments[3], materials);
        }

        return List.of();
    }

    private List<String> copyMatches(String token, List<String> values) {
        List<String> matches = new ArrayList<>();
        StringUtil.copyPartialMatches(token, values, matches);
        return matches;
    }
}
