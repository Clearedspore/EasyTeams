package me.clearedspore.easyTeams.Utils;

import me.clearedspore.easyTeams.EasyTeams;
import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TabPlayer;
import me.neznamy.tab.api.nametag.NameTagManager;
import me.neznamy.tab.api.tablist.TabListFormatManager;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class TeamManager {
    private final Plugin plugin;
    private final Map<String, Team> teams = new HashMap<>();
    public final Map<UUID, String> playerTeams = new HashMap<>();
    private boolean coinEventActive = false;
    private int coinMultiplier = 1;

    public TeamManager(EasyTeams plugin) {
        this.plugin = plugin;
        loadTeams();
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public boolean isCoinEventActive() {
        return coinEventActive;
    }

    public void setCoinEventActive(boolean coinEventActive) {
        this.coinEventActive = coinEventActive;
    }

    public int getCoinMultiplier() {
        return coinMultiplier;
    }

    public void setCoinMultiplier(int coinMultiplier) {
        if (coinMultiplier < 2) {
            this.coinMultiplier = 2;
        } else if (coinMultiplier > 4) {
            this.coinMultiplier = 4;
        } else {
            this.coinMultiplier = coinMultiplier;
        }
    }

    public void saveTeams() {
        File file = new File(plugin.getDataFolder(), "teams.yml");
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

        YamlConfiguration config = new YamlConfiguration();

        for (Map.Entry<String, Team> entry : teams.entrySet()) {
            String teamName = entry.getKey();
            Team team = entry.getValue();

            config.set(teamName + ".leader", team.getLeader().toString());
            config.set(teamName + ".coleader", team.getcoleader());
            config.set(teamName + ".captain", team.getcaptain());
            config.set(teamName + ".members", team.getMembers());
            config.set(teamName + ".coins", team.getCoins());
            config.set(teamName + ".kills", team.getKills());
            config.set(teamName + ".deaths", team.getDeaths());

            Map<String, String> memberMap = team.getMembers().entrySet().stream()
                    .collect(Collectors.toMap(
                            entryMember -> entryMember.getKey().toString(),
                            Map.Entry::getValue
                    ));
            config.createSection(teamName + ".members", memberMap);
        }

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadTeams() {
        File file = new File(plugin.getDataFolder(), "teams.yml");
        if (!file.exists()) {
            return; // No file to load
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        for (String teamName : config.getKeys(false)) {
            UUID leaderUUID = UUID.fromString(config.getString(teamName + ".leader"));
            Map<String, Object> membersMap = config.getConfigurationSection(teamName + ".members").getValues(false);
            Map<UUID, String> members = new HashMap<>();
            Map<UUID, String> coleaders = new HashMap<>();
            Map<UUID, String> captains = new HashMap<>();

            for (Map.Entry<String, Object> entry : membersMap.entrySet()) {
                UUID memberUUID = UUID.fromString(entry.getKey());
                String rank = entry.getValue().toString();
                members.put(memberUUID, rank);
                if (rank.equalsIgnoreCase("co-leader")) {
                    coleaders.put(memberUUID, rank);
                } else if (rank.equalsIgnoreCase("captain")) {
                    captains.put(memberUUID, rank);
                }
            }

            int coins = config.getInt(teamName + ".coins");

            Team team = new Team(teamName, leaderUUID, members, coleaders, captains, new HashMap<>());
            team.setCoins(coins);

            teams.put(teamName.toLowerCase(), team);
            members.keySet().forEach(uuid -> playerTeams.put(uuid, teamName.toLowerCase()));
        }
    }


    public boolean createTeam(String name, Player creator) {
        if (playerTeams.containsKey(creator.getUniqueId())) {
            creator.sendMessage(ChatColor.RED + "You are already in a team.");
            return false;
        }
        if (teams.containsKey(name.toLowerCase())) {
            return false;
        }
        Map<UUID, String> coleader = new HashMap<>();
        Map<UUID, String> captain = new HashMap<>();
        Team team = new Team(name, creator.getUniqueId(), coleader, captain);
        teams.put(name.toLowerCase(), team);
        playerTeams.put(creator.getUniqueId(), name.toLowerCase());
        saveTeams();
        return true;
    }

    public boolean renameTeam(String oldName, String newName) {
        if (!teams.containsKey(oldName.toLowerCase()) || teams.containsKey(newName.toLowerCase())) {
            return false;
        }
        Team team = teams.remove(oldName.toLowerCase());
        team.setName(newName);
        teams.put(newName.toLowerCase(), team);

        for (UUID memberUUID : team.getMembers().keySet()) {
            playerTeams.put(memberUUID, newName.toLowerCase());
        }

        saveTeams();
        updateAllPlayersNameTags();
        return true;
    }

    public boolean deleteTeam(String name) {
        if (!teams.containsKey(name.toLowerCase())) {
            return true;
        }
        Team team = teams.remove(name.toLowerCase());
        team.getMembers().keySet().forEach(uuid -> playerTeams.remove(uuid));
        saveTeams();
        return true;
    }
    public int getPlayerTeamKills(UUID playerUUID) {
        String TeamName = playerTeams.get(playerUUID);
        if (TeamName == null) {
            return 0;
        }

        Team team = teams.get(TeamName.toLowerCase());
        if (team != null) {
            return team.getKills();
        }
        return 0;
    }
    public int getPlayerTeamDeaths(UUID playerUUID) {
        String TeamName = playerTeams.get(playerUUID);
        if (TeamName == null) {
            return 0;
        }

        Team team = teams.get(TeamName.toLowerCase());
        if (team != null) {
            return team.getDeaths();
        }
        return 0;
    }
    public int getPlayerTeamCoins(UUID playerUUID) {
        String teamName = playerTeams.get(playerUUID);
        if (teamName == null) {
            return 0;
        }
        Team team = teams.get(teamName.toLowerCase());
        if (team != null) {
            return team.getCoins();
        }
        return 0;
    }

    public boolean forceJoinTeam(Player player, String teamName) {
        if (!teams.containsKey(teamName.toLowerCase())) {
            return false;
        }
        Team team = teams.get(teamName.toLowerCase());

        team.getMembers().put(player.getUniqueId(), "member");
        playerTeams.put(player.getUniqueId(), teamName.toLowerCase());
        updatePlayerNameTag(player);

        return true;
    }

    public boolean invitePlayer(Player inviter, Player invitee) {
        String teamName = playerTeams.get(inviter.getUniqueId());
        if (teamName == null) {
            return false; // Inviter is not in a team
        }
        Team team = teams.get(teamName);
        team.sendInvite(invitee);

        // Schedule invite expiration
        new BukkitRunnable() {
            @Override
            public void run() {
                team.expireInvite(invitee);
            }
        }.runTaskLater(plugin, 20 * 60);
        return true;
    }

    public boolean joinTeam(Player player, String teamName) {
        if (!teams.containsKey(teamName.toLowerCase())) {
            player.sendMessage(ChatColor.RED + "This team does not exist.");
            return true;
        }
        Team team = teams.get(teamName.toLowerCase());
        if (!team.addMember(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED +"You are already in a team.");
            return true;
        }

        playerTeams.put(player.getUniqueId(), teamName.toLowerCase());
        return true;
    }

    public boolean kickPlayer(Player kicker, Player target) {
        String teamName = playerTeams.get(kicker.getUniqueId());
        if (teamName == null || !playerTeams.containsKey(target.getUniqueId())) {
            kicker.sendMessage(ChatColor.RED + "You cannot kick this player");
        }
        Team team = teams.get(teamName);
        if (!team.kickMember(target.getUniqueId())) {
            kicker.sendMessage(ChatColor.RED + "You cannot kick this player");
            return true;
        }
        playerTeams.remove(target.getUniqueId());
        return true;
    }

    public List<String> getPlayers(String teamName) {
        if (!teams.containsKey(teamName.toLowerCase())) {
            return Collections.emptyList();
        }
        return teams.get(teamName.toLowerCase()).getMembers().keySet().stream()
                .map(uuid -> {
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                    return (offlinePlayer.hasPlayedBefore() || offlinePlayer.isOnline()) ? offlinePlayer.getName() : "Unknown";
                })
                .collect(Collectors.toList());
    }
    public void updatePlayerNameTag(Player player) {
        Plugin tabPlugin = Bukkit.getPluginManager().getPlugin("TAB");
        if (tabPlugin == null || !tabPlugin.isEnabled()) {
            return;
        }

        TabPlayer tabPlayer = TabAPI.getInstance().getPlayer(player.getUniqueId());
        if (tabPlayer == null) return;

        String playerTeamName = getPlayerTeam(player.getUniqueId());
        String nameTag = "";

        if (playerTeamName != null) {
            nameTag = ChatColor.GRAY + " [" + ChatColor.YELLOW + playerTeamName + ChatColor.GRAY + "]";
        }

        setPrefix(tabPlayer, nameTag);
    }

    public void updateAllPlayersNameTags() {
        Plugin tabPlugin = Bukkit.getPluginManager().getPlugin("TAB");
        if (tabPlugin == null || !tabPlugin.isEnabled()) {
            System.out.println("TAB plugin is not enabled.");
            return;
        }
        System.out.println("Updating all players' name tags.");
        for (Player player : Bukkit.getOnlinePlayers()) {
            System.out.println("Updating name tag for player: " + player.getName());
            updatePlayerNameTag(player);
        }
    }
    public void setPrefix(TabPlayer tabPlayer, String nameTag) {
        if (tabPlayer == null) return;

        TabListFormatManager tabManager = TabAPI.getInstance().getTabListFormatManager();
        NameTagManager nameTagManager = TabAPI.getInstance().getNameTagManager();
        if (nameTagManager != null) {
            nameTagManager.setSuffix(tabPlayer, nameTag);
            tabManager.setSuffix(tabPlayer, nameTag);
        }
    }


    public void updateItems(Player p, Team teamToManage) {
        if (p.getOpenInventory() != null && p.getOpenInventory().getTitle().equals("Manage Team: " + teamToManage.getName())) {
            Inventory inventory = p.getOpenInventory().getTopInventory();

            ItemStack promoteItem = new ItemStack(Material.DIAMOND);
            ItemMeta promoteMeta = promoteItem.getItemMeta();
            promoteMeta.setDisplayName(ChatColor.GREEN + "Promote Members");
            promoteItem.setItemMeta(promoteMeta);

            ItemStack kickItem = new ItemStack(Material.BARRIER);
            ItemMeta kickMeta = kickItem.getItemMeta();
            kickMeta.setDisplayName(ChatColor.RED + "Kick Members");
            kickItem.setItemMeta(kickMeta);

            ItemStack demoteItem = new ItemStack(Material.WOODEN_SWORD);
            ItemMeta demoteMeta = demoteItem.getItemMeta();
            demoteMeta.setDisplayName(ChatColor.YELLOW + "Demote Members");
            demoteItem.setItemMeta(demoteMeta);

            ItemStack pvpItem = new ItemStack(Material.IRON_SWORD);
            ItemMeta pvpMeta = pvpItem.getItemMeta();
            pvpMeta.setDisplayName(ChatColor.BLUE + "Toggle PvP");
            pvpMeta.setLore(List.of(ChatColor.GRAY + "Current: " + (teamToManage.isPvPEnabled() ? "Enabled" : "Disabled")));
            pvpItem.setItemMeta(pvpMeta);

            ItemStack renameItem = new ItemStack(Material.NAME_TAG);
            ItemMeta renameMeta = renameItem.getItemMeta();
            renameMeta.setDisplayName(ChatColor.AQUA + "Rename Team");
            renameItem.setItemMeta(renameMeta);

            ItemStack editCoinsItem = new ItemStack(Material.GOLD_INGOT);
            ItemMeta editCoinsMeta = editCoinsItem.getItemMeta();
            editCoinsMeta.setDisplayName(ChatColor.GOLD + "Edit Coins");
            editCoinsItem.setItemMeta(editCoinsMeta);

            ItemStack deleteTeamItem = new ItemStack(Material.TNT);
            ItemMeta deleteTeamMeta = deleteTeamItem.getItemMeta();
            deleteTeamMeta.setDisplayName(ChatColor.RED + "Delete Team");
            deleteTeamItem.setItemMeta(deleteTeamMeta);

            inventory.setItem(10, promoteItem);
            inventory.setItem(12, kickItem);
            inventory.setItem(14, demoteItem);
            inventory.setItem(16, pvpItem);
            inventory.setItem(19, renameItem);
            inventory.setItem(21, editCoinsItem);
            inventory.setItem(23, deleteTeamItem);

            p.updateInventory();
        }
    }

    public Team getTeam(String name) {
        return teams.get(name.toLowerCase());
    }

    public String getPlayerTeam(UUID playerId) {
        return playerTeams.get(playerId);
    }

    public Map<String, Team> getAllTeams() {
        return teams;
    }
    public boolean teamExists(String teamName) {
        return teams.containsKey(teamName.toLowerCase());
    }
}
