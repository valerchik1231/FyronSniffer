package ru.valerchik.fyronsniffer.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.valerchik.fyronsniffer.FyronSniffer;

import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Configs {

    private static final Pattern HEX_PATTERN = Pattern.compile("(?i)&?#([0-9a-f]{6})");
    private static final Pattern MINI_MESSAGE_TAG_PATTERN = Pattern.compile(
            "(?i)</?(?:black|dark_blue|dark_green|dark_aqua|dark_red|dark_purple|gold|gray|grey|dark_gray|dark_grey|blue|green|aqua|red|light_purple|yellow|white|reset|bold|italic|underlined|strikethrough|obfuscated|rainbow|gradient|transition|click|hover|insertion|font|newline|lang|keybind|selector|score|nbt|translatable|color|shadow|pride|#[0-9a-f]{6})[^>]*>"
    );
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private final FyronSniffer plugin;
    private FileConfiguration messagesConfiguration;
    private boolean vanillaEnabled;

    public Configs(FyronSniffer plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        this.messagesConfiguration = YamlConfiguration.loadConfiguration(new File(this.plugin.getDataFolder(), "messages.yml"));
        FileConfiguration configuration = this.plugin.getConfig();
        this.vanillaEnabled = configuration.getBoolean("sniffer.vanilla-enabled", true);
    }

    public boolean isVanillaEnabled() {
        return this.vanillaEnabled;
    }

    public void send(CommandSender sender, String path, String... replacements) {
        String text = this.messagesConfiguration.getString(path, "");
        if (text.isEmpty()) {
            return;
        }

        String parsedText = replace(text, replacements);
        if (!parsedText.isEmpty()) {
            sender.sendMessage(parse(parsedText));
        }
    }

    public void sendList(CommandSender sender, String path, String... replacements) {
        List<String> lines = this.messagesConfiguration.getStringList(path);
        if (lines.isEmpty()) {
            return;
        }

        for (String line : lines) {
            if (line == null || line.isEmpty()) {
                continue;
            }

            String parsedText = replace(line, replacements);
            if (!parsedText.isEmpty()) {
                sender.sendMessage(parse(parsedText));
            }
        }
    }

    public Component parse(String text) {
        if (MINI_MESSAGE_TAG_PATTERN.matcher(text).find()) {
            return MINI_MESSAGE.deserialize(text);
        }
        return LEGACY_SERIALIZER.deserialize(hex(text));
    }

    private String replace(String text, String... replacements) {
        String result = text;
        for (int index = 0; index + 1 < replacements.length; index += 2) {
            result = result.replace("%" + replacements[index] + "%", replacements[index + 1]);
        }
        return result;
    }

    private String hex(String text) {
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuilder output = new StringBuilder();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("&x");
            for (char character : hex.toCharArray()) {
                replacement.append('&').append(character);
            }
            matcher.appendReplacement(output, Matcher.quoteReplacement(replacement.toString()));
        }
        matcher.appendTail(output);
        return output.toString();
    }
}