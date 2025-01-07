package me.clearedspore.easyTeams.Listener;

import me.clearedspore.easyTeams.Utils.Team;
import me.clearedspore.easyTeams.Utils.TeamManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.HashMap;
import java.util.Map;

public class CoinEditListener implements Listener {
    private static final Map<Player, String> setCoinsRequests = new HashMap<>();
    private static final Map<Player, String> addCoinsRequests = new HashMap<>();
    private static final Map<Player, String> removeCoinsRequests = new HashMap<>();
    private final TeamManager teamManager;

    public CoinEditListener(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    public static void addPlayerToSetCoinsList(Player player, String teamName) {
        setCoinsRequests.put(player, teamName);
    }

    public static void addPlayerToAddCoinsList(Player player, String teamName) {
        addCoinsRequests.put(player, teamName);
    }

    public static void addPlayerToRemoveCoinsList(Player player, String teamName) {
        removeCoinsRequests.put(player, teamName);
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage().trim();

        if (setCoinsRequests.containsKey(player)) {
            event.setCancelled(true);
            handleSetCoins(player, message);
        } else if (addCoinsRequests.containsKey(player)) {
            event.setCancelled(true);
            handleAddCoins(player, message);
        } else if (removeCoinsRequests.containsKey(player)) {
            event.setCancelled(true);
            handleRemoveCoins(player, message);
        }
    }

    private void handleSetCoins(Player player, String message) {
        String teamName = setCoinsRequests.remove(player);
        Team team = teamManager.getTeam(teamName);

        try {
            int amount = Integer.parseInt(message);
            team.setCoins(amount);
            player.sendMessage(ChatColor.GREEN + "Coins set to " + amount + " for team " + teamName + ".");
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid number. Please enter a valid integer.");
        }
    }

    private void handleAddCoins(Player player, String message) {
        String teamName = addCoinsRequests.remove(player);
        Team team = teamManager.getTeam(teamName);

        try {
            int amount = Integer.parseInt(message);
            team.setCoins(team.getCoins() + amount);
            player.sendMessage(ChatColor.GREEN + "Added " + amount + " coins to team " + teamName + ".");
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid number. Please enter a valid integer.");
        }
    }

    private void handleRemoveCoins(Player player, String message) {
        String teamName = removeCoinsRequests.remove(player);
        Team team = teamManager.getTeam(teamName);

        try {
            int amount = Integer.parseInt(message);
            team.setCoins(Math.max(0, team.getCoins() - amount));
            player.sendMessage(ChatColor.GREEN + "Removed " + amount + " coins from team " + teamName + ".");
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid number. Please enter a valid integer.");
        }
    }
}