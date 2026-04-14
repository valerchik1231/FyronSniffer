package ru.valerchik.fyronsniffer.utils;

import org.bukkit.Bukkit;
import org.json.simple.JSONObject;
import ru.valerchik.fyronsniffer.FyronSniffer;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class Metrics {
    public static CompletableFuture<Void> sendAsync(FyronSniffer plugin) {
        plugin.getLogger().info("Starting metrics submission...");
        return CompletableFuture.runAsync(() -> {
            try {
                URL url = new URL("https://fyronstudio.store/PluginMetrics/metrics");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject json = new JSONObject();
                json.put("plugin", plugin.getName());
                json.put("version", plugin.getDescription().getVersion());
                json.put("players", Bukkit.getOnlinePlayers().size());
                json.put("port", Bukkit.getPort());
                json.put("server_version", Bukkit.getVersion());
                json.put("java", System.getProperty("java.version"));

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(json.toString().getBytes(StandardCharsets.UTF_8));
                    os.flush();
                }

                try (InputStream is = conn.getInputStream()) {
                    while (is.read() != -1) {}
                } catch (Exception ignored) {}

                plugin.getLogger().info("Metrics successfully sent.");
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to send metrics.");
            }
        });
    }
}
