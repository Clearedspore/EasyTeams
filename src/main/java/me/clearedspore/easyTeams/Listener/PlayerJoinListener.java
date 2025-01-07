package me.clearedspore.easyTeams.Listener;

import me.clearedspore.easyTeams.Utils.TeamManager;
import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TabPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class PlayerJoinListener implements Listener {
    private final TeamManager teamManager;

    public PlayerJoinListener(TeamManager teamManager) {
        this.teamManager = teamManager;
    }
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Schedule a delayed task to update the player's name tag
        new BukkitRunnable() {
            @Override
            public void run() {
                teamManager.updatePlayerNameTag(player);
            }
        }.runTaskLater(teamManager.getPlugin(), 20L); // Delay of 20 ticks (1 second)
    }
}
