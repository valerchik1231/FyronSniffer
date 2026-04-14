package ru.valerchik.fyronsniffer.listeners;

import org.bukkit.entity.Item;
import org.bukkit.entity.Sniffer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.inventory.ItemStack;
import ru.valerchik.fyronsniffer.FyronSniffer;

public final class SnifferListener implements Listener {

    private final FyronSniffer plugin;

    public SnifferListener(FyronSniffer plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSnifferDrop(EntityDropItemEvent event) {
        if (!(event.getEntity() instanceof Sniffer)) {
            return;
        }

        Item vanillaDrop = event.getItemDrop();
        ItemStack resultItemStack = this.plugin.getLootRegistry().randomItem().orElse(null);
        if (resultItemStack == null) {
            if (this.plugin.getConfigs().isVanillaEnabled()) {
                resultItemStack = vanillaDrop.getItemStack().clone();
            } else {
                resultItemStack = this.plugin.getLootRegistry().getOnlyCustomItem().orElse(null);
            }
        }

        if (resultItemStack == null || resultItemStack.getType().isAir()) {
            event.setCancelled(true);
            return;
        }

        event.getItemDrop().setItemStack(resultItemStack);
    }
}