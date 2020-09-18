package bestem0r.villagermarket.events.chat;

import bestem0r.villagermarket.VMPlugin;
import bestem0r.villagermarket.utilities.Color;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.UUID;

public class ChangeName implements Listener {

    private final Player player;
    private final Entity villager;

    public ChangeName(Player player, String entityUUID) {
        this.player = player;
        this.villager = Bukkit.getEntity(UUID.fromString(entityUUID));
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (event.getPlayer() == this.player) {
            if (event.getMessage().equalsIgnoreCase("cancel")) {
                player.sendMessage(VMPlugin.getPrefix() + new Color.Builder().path("messages.cancelled").build());
            } else {
                String name = ChatColor.translateAlternateColorCodes('&', event.getMessage());
                villager.setCustomName(name);
                player.sendMessage(VMPlugin.getPrefix() + new Color.Builder()
                        .path("messages.change_name_set")
                        .replace("%name%", name)
                        .build());
            }

            event.setCancelled(true);
            HandlerList.unregisterAll(this);
        }
    }
}