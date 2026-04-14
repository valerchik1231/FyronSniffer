package ru.valerchik.fyronsniffer;

import org.bukkit.Bukkit;
import java.io.File;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import ru.valerchik.fyronsniffer.command.SnifferCommand;
import ru.valerchik.fyronsniffer.command.SnifferTabCompleter;
import ru.valerchik.fyronsniffer.config.Configs;
import ru.valerchik.fyronsniffer.listeners.SnifferListener;
import ru.valerchik.fyronsniffer.utils.LootRegistry;
import ru.valerchik.fyronsniffer.utils.Metrics;

public final class FyronSniffer extends JavaPlugin {

    private Configs configs;
    private LootRegistry lootRegistry;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (!new File(getDataFolder(), "messages.yml").exists()) {
            saveResource("messages.yml", false);
        }

        this.configs = new Configs(this);
        this.lootRegistry = new LootRegistry(this);
        reloadAll();

        PluginCommand pluginCommand = getCommand("fyronsniffer");
        if (pluginCommand == null) {
            getLogger().severe("command fyronsniffer not found!");
            return;
        }

        pluginCommand.setExecutor(new SnifferCommand(this));
        pluginCommand.setTabCompleter(new SnifferTabCompleter(this));
        getServer().getPluginManager().registerEvents(new SnifferListener(this), this);

        boolean metricsE = getConfig().getBoolean("metrics", true);

        if (metricsE) {
            Metrics.sendAsync(this).whenComplete((res, ex) -> {
                if (ex != null) {
                    getLogger().severe("Failed to send metrics: " + ex.getMessage());
                }
            });

            Bukkit.getScheduler().runTaskTimer(this, () ->
                    Metrics.sendAsync(this).whenComplete((res, ex) -> {
                        if (ex != null) {
                            getLogger().warning("Failed to resend metrics: " + ex.getMessage());
                        }
                    }), 72000L, 72000L);
        }
    }

    public void reloadAll() {
        reloadConfig();
        this.configs.reload();
        this.lootRegistry.reload();
    }

    public Configs getConfigs() {
        return this.configs;
    }

    public LootRegistry getLootRegistry() {
        return this.lootRegistry;
    }
}