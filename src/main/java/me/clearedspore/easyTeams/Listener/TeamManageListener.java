package me.clearedspore.easyTeams.Listener;

import me.clearedspore.easyTeams.Commands.TeamCommand;
import me.clearedspore.easyTeams.Utils.Team;
import me.clearedspore.easyTeams.Utils.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class TeamManageListener implements Listener {
    private TeamManager teamManager;

    public TeamManageListener(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        if (event.getView().getTitle().startsWith("Manage Team: ")) {
            event.setCancelled(true); // Prevent taking items from the GUI

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

            String teamName = event.getView().getTitle().substring("Manage Team: ".length());
            if (!teamManager.teamExists(teamName)) {
                player.sendMessage(ChatColor.RED + "Team no longer exists.");
                player.closeInventory();
                return;
            }

            if (clickedItem.getType() == Material.DIAMOND) {
                openPromoteGUI(player, teamName);
            } else if (clickedItem.getType() == Material.BARRIER) {
                openKickGUI(player, teamName);
            } else if (clickedItem.getType() == Material.IRON_SWORD) {
                togglePvP(player, teamName);
            } else if (clickedItem.getType() == Material.NAME_TAG) {
                renameTeam(player, teamName);
            } else if (clickedItem.getType() == Material.WOODEN_SWORD) {
                openDemoteGUI(player, teamName);
            } else if (clickedItem.getType() == Material.GOLD_INGOT) {
                openEditCoinsGUI(player, teamName);
            } else if(clickedItem.getType() == Material.TNT){
                handleDeleteClick(player, teamName);
            }
        } else if (event.getView().getTitle().startsWith("Promote Members: ")) {
            event.setCancelled(true);
            handlePromoteClick(event, player);
        } else if (event.getView().getTitle().startsWith("Kick Members: ")) {
            event.setCancelled(true);
            handleKickClick(event, player);
        } else if (event.getView().getTitle().startsWith("Demote Members: ")) {
            event.setCancelled(true);
            handleDemoteClick(event, player);
        } else if (event.getView().getTitle().startsWith("Edit Coins: ")) {
            event.setCancelled(true);
            handleEditCoinsClick(event, player);
        }
    }

    private void openPromoteGUI(Player player, String teamName) {
        Inventory promoteInventory = Bukkit.createInventory(null, 27, "Promote Members: " + teamName);
        List<String> members = teamManager.getPlayers(teamName);

        for (int i = 0; i < members.size() && i < 27; i++) {
            ItemStack memberItem = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta meta = memberItem.getItemMeta();
            meta.setDisplayName(ChatColor.GREEN + members.get(i));
            memberItem.setItemMeta(meta);
            promoteInventory.setItem(i, memberItem);
        }

        player.openInventory(promoteInventory);
    }

    private void openKickGUI(Player player, String teamName) {
        Inventory kickInventory = Bukkit.createInventory(null, 27, "Kick Members: " + teamName);
        List<String> members = teamManager.getPlayers(teamName);

        for (int i = 0; i < members.size() && i < 27; i++) {
            ItemStack memberItem = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta meta = memberItem.getItemMeta();
            meta.setDisplayName(ChatColor.RED + members.get(i));
            memberItem.setItemMeta(meta);
            kickInventory.setItem(i, memberItem);
        }

        player.openInventory(kickInventory);
    }

    private void openDemoteGUI(Player player, String teamName) {
        Inventory demoteInventory = Bukkit.createInventory(null, 27, "Demote Members: " + teamName);
        List<String> members = teamManager.getPlayers(teamName);

        for (int i = 0; i < members.size() && i < 27; i++) {
            ItemStack memberItem = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta meta = memberItem.getItemMeta();
            meta.setDisplayName(ChatColor.YELLOW + members.get(i));
            memberItem.setItemMeta(meta);
            demoteInventory.setItem(i, memberItem);
        }

        player.openInventory(demoteInventory);
    }

    private void togglePvP(Player player, String teamName) {
        Team team = teamManager.getTeam(teamName);
        boolean currentPvPStatus = team.isPvPEnabled();
        team.setPvPEnabled(!currentPvPStatus);
        player.sendMessage(ChatColor.GREEN + "PvP is now " + (team.isPvPEnabled() ? "enabled" : "disabled") + " for team " + teamName + ".");

        // Use the method from TeamManager or a utility class
        teamManager.updateItems(player, team);
    }

    private void renameTeam(Player player, String teamName) {
        player.closeInventory();
        player.sendMessage(ChatColor.YELLOW + "Please type the new name for the team in chat.");
        RenameListener.addPlayerToRenameList(player, teamName);
    }

    private void openEditCoinsGUI(Player player, String teamName) {
        Inventory coinsInventory = Bukkit.createInventory(null, 9, "Edit Coins: " + teamName);

        ItemStack setCoinsItem = new ItemStack(Material.EMERALD);
        ItemMeta setCoinsMeta = setCoinsItem.getItemMeta();
        setCoinsMeta.setDisplayName(ChatColor.GREEN + "Set Coins");
        setCoinsItem.setItemMeta(setCoinsMeta);

        ItemStack addCoinsItem = new ItemStack(Material.GOLD_NUGGET);
        ItemMeta addCoinsMeta = addCoinsItem.getItemMeta();
        addCoinsMeta.setDisplayName(ChatColor.YELLOW + "Add Coins");
        addCoinsItem.setItemMeta(addCoinsMeta);

        ItemStack removeCoinsItem = new ItemStack(Material.REDSTONE);
        ItemMeta removeCoinsMeta = removeCoinsItem.getItemMeta();
        removeCoinsMeta.setDisplayName(ChatColor.RED + "Remove Coins");
        removeCoinsItem.setItemMeta(removeCoinsMeta);

        coinsInventory.setItem(2, setCoinsItem);
        coinsInventory.setItem(4, addCoinsItem);
        coinsInventory.setItem(6, removeCoinsItem);

        player.openInventory(coinsInventory);
    }

    private void handleEditCoinsClick(InventoryClickEvent event, Player player) {
        String teamName = event.getView().getTitle().substring("Edit Coins: ".length());
        Team team = teamManager.getTeam(teamName);
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null) return;

        if (clickedItem.getType() == Material.EMERALD) {
            player.closeInventory();
            player.sendMessage(ChatColor.YELLOW + "Please type the amount to set the coins to in chat.");
            CoinEditListener.addPlayerToSetCoinsList(player, teamName);
        } else if (clickedItem.getType() == Material.GOLD_NUGGET) {
            player.closeInventory();
            player.sendMessage(ChatColor.YELLOW + "Please type the amount to add to the coins in chat.");
            CoinEditListener.addPlayerToAddCoinsList(player, teamName);
        } else if (clickedItem.getType() == Material.REDSTONE) {
            player.closeInventory();
            player.sendMessage(ChatColor.YELLOW + "Please type the amount to remove from the coins in chat.");
            CoinEditListener.addPlayerToRemoveCoinsList(player, teamName);
        }
    }

    private void handlePromoteClick(InventoryClickEvent event, Player player) {
        String teamName = event.getView().getTitle().substring("Promote Members: ".length());
        Team team = teamManager.getTeam(teamName);
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() != Material.PLAYER_HEAD) return;

        String playerName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Player not found.");
            return;
        }

        if (!team.promoteMember(target.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Cannot promote further.");
        } else {
            player.sendMessage(ChatColor.GREEN + target.getName() + " has been promoted.");
        }
    }

    private void handleKickClick(InventoryClickEvent event, Player player) {
        String teamName = event.getView().getTitle().substring("Kick Members: ".length());
        Team team = teamManager.getTeam(teamName);
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() != Material.PLAYER_HEAD) return;

        String playerName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Player not found.");
            return;
        }

        if (!team.kickMember(target.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Cannot kick this player.");
        } else {
            player.sendMessage(ChatColor.GREEN + target.getName() + " has been kicked from the team.");
        }
    }

    private void handleDemoteClick(InventoryClickEvent event, Player player) {
        String teamName = event.getView().getTitle().substring("Demote Members: ".length());
        Team team = teamManager.getTeam(teamName);
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() != Material.PLAYER_HEAD) return;

        String playerName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Player not found.");
            return;
        }

        if (!team.demoteMember(target.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Cannot demote further.");
        } else {
            player.sendMessage(ChatColor.GREEN + target.getName() + " has been demoted.");
        }
    }

    private void handleDeleteClick(Player player, String teamName) {
        if (teamName == null) {}
        Team team = teamManager.getTeam(teamName);

        player.closeInventory();
        player.performCommand("team deleteconfirm " + teamName);

        teamManager.updateItems(player, team);
    }
}
