package me.clearedspore.easyTeams.Commands;

import me.clearedspore.Features.Logs.LogManager;
import me.clearedspore.easyTeams.Utils.Team;
import me.clearedspore.easyTeams.Utils.TeamManager;
import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TabPlayer;
import me.neznamy.tab.api.nametag.NameTagManager;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import javax.swing.plaf.synth.Region;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.spi.AbstractResourceBundleProvider;

public class TeamCommand implements CommandExecutor, Listener {
    private final TeamManager teamManager;
    private final Plugin plugin;
    private LogManager logManager;
    private BossBar coinEventBossBar;
    private BossBar glowEventBossBar;
    private int coinMultiplier = 1;
    private Map<UUID, Location[]> playerSelections = new HashMap<>();
    private Map<UUID, Region> playerRegions = new HashMap<>();
    private BukkitTask glowEventTask;
    private BukkitTask coinEventTask;
    private Set<UUID> glowingTeamMembers = new HashSet<>();
    private File eventFile;


    public TeamCommand(TeamManager teamManager, Plugin plugin) {
        this.teamManager = teamManager;
        this.plugin = plugin;
        this.coinEventBossBar = Bukkit.createBossBar("Coin Event", BarColor.YELLOW, BarStyle.SOLID);
        this.glowEventBossBar = Bukkit.createBossBar("Glow Event", BarColor.BLUE, BarStyle.SEGMENTED_10);
        this.eventFile = new File(plugin.getDataFolder(), "events.yml");
        loadRegionFromFile();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can do team commands!");
            return true;
        }

        Player p = (Player) sender;
        if (args.length == 0) {
            if(p.hasPermission("easyteams.team")) {
                p.sendMessage(ChatColor.RED + "Correct usage: /team <create|invite|join|kick|rename|info|disband|pvp|>");
            return true;
            }
            if(p.hasPermission("easyteams.admin")) {
                p.sendMessage(ChatColor.RED + "Correct usage: /team <create|invite|join|kick|rename|info|disband|pvp|forcejoin|forceleader>");
            }
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create":
                handleCreateCommand(p, args);
                break;
            case "invite":
                handleInviteCommand(p, args);
                break;
            case "join":
                handleJoinCommand(p, args);
                break;
            case "rename":
                handleRenameCommand(p, args);
                break;
            case "demote":
                handleDemoteCommand(p, args);
                break;
            case "info":
                handleInfoCommand(p, args);
                break;
            case "kick":
                handleKickCommand(p, args);
                break;
            case "pvp":
                handlePvpCommand(p);
                break;
            case "disband":
                handleDisbandCommand(p);
                break;
            case "deleteconfirm":
                handleDeleteConfirmCommand(p, args);
                break;
            case "promote":
                handlePromoteCommand(p, args);
                break;
            case "delete":
                handleDeleteCommand(p, args);
                break;
            case "reload":
                handleReloadCommand(p);
                break;
            case "manage":
                handleManageCommand(p, args);
                break;
            case "forcejoin":
                handleForceJoinCommand(p, args);
                break;
            case "forceleader":
                handleForceLeaderCommand(p);
                break;
            case "event":
                p.sendMessage(ChatColor.RED + "Coming soon!");
                break;
            case "leave":
                handleLeaveCommand(p);
                break;
            case "help":
                handleHelpCommand(p);
                break;
            case "region":
                handleRegionCommand(p, args);
                break;
            default:
                p.sendMessage(ChatColor.RED + "Unknown subcommand.");
                break;
        }

        return true;
    }
    private void handleRegionCommand(Player p, String[] args) {
        if (args.length < 2) {
            p.sendMessage(ChatColor.RED + "Usage: /team region <wand|set|delete>");
            return;
        }

        switch (args[1].toLowerCase()) {
            case "wand":
                p.sendMessage(ChatColor.RED + "Coming soon!");
//                giveRegionWand(p);
                break;
            case "set":
                p.sendMessage(ChatColor.RED + "Coming soon!");
//                setRegion(p);
                break;
            case "delete":
                p.sendMessage(ChatColor.RED + "Coming soon!");
//                deleteRegion(p);
                break;
            default:
                p.sendMessage(ChatColor.RED + "Coming soon!");
//                p.sendMessage(ChatColor.RED + "Unknown region subcommand.");
                break;
        }
    }
    private void giveRegionWand(Player player) {
        ItemStack wand = new ItemStack(Material.STICK);
        ItemMeta meta = wand.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Region Wand");
        wand.setItemMeta(meta);
        player.getInventory().addItem(wand);
        player.sendMessage(ChatColor.GREEN + "You have received the region wand.");
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType() == Material.STICK && item.getItemMeta().getDisplayName().equals(ChatColor.GOLD + "Region Wand")) {
            Location clickedBlock = event.getClickedBlock().getLocation();
            UUID playerId = player.getUniqueId();

            if (event.getAction().toString().contains("LEFT_CLICK")) {
                playerSelections.computeIfAbsent(playerId, k -> new Location[2])[0] = clickedBlock;
                player.sendMessage(ChatColor.GREEN + "Position 1 set.");
            } else if (event.getAction().toString().contains("RIGHT_CLICK")) {
                playerSelections.computeIfAbsent(playerId, k -> new Location[2])[1] = clickedBlock;
                player.sendMessage(ChatColor.GREEN + "Position 2 set.");
            }

            event.setCancelled(true);
        }
    }

    private void setRegion(Player player) {
        Location[] positions = playerSelections.get(player.getUniqueId());
        if (positions == null || positions[0] == null || positions[1] == null) {
            player.sendMessage(ChatColor.RED + "You must select two positions first.");
            return;
        }
        playerRegions.put(player.getUniqueId(), new Region(positions[0], positions[1]));
        saveRegionToFile(positions[0], positions[1]);
        player.getInventory().remove(Material.STICK);
        player.sendMessage(ChatColor.GREEN + "Region set successfully.");
    }

    private void deleteRegion(Player player) {
        playerRegions.remove(player.getUniqueId());
        player.sendMessage(ChatColor.GREEN + "Region deleted successfully.");
    }

    private void saveRegionToFile(Location pos1, Location pos2) {
        if (!eventFile.exists()) {
            try {
                eventFile.getParentFile().mkdirs();
                eventFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Save the region to the file (using your preferred method, e.g., YAML, JSON)
        // Example: Using Bukkit's YamlConfiguration
        YamlConfiguration config = YamlConfiguration.loadConfiguration(eventFile);
        config.set("region.pos1", pos1);
        config.set("region.pos2", pos2);
        try {
            config.save(eventFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadRegionFromFile() {
        if (!eventFile.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(eventFile);
        Location pos1 = (Location) config.get("region.pos1");
        Location pos2 = (Location) config.get("region.pos2");

        if (pos1 != null && pos2 != null) {
            // Assuming you want to load this region for a specific player or globally
            // Example: Load for a specific player
            // playerRegions.put(playerUUID, new Region(pos1, pos2));
        }
    }

    private void startGlowEvent(Player player, int time) {
        Region region = playerRegions.get(player.getUniqueId());
        if (region == null) {
            player.sendMessage(ChatColor.RED + "No region set. Use /team region set first.");
            return;
        }

        Team team = teamManager.getTeam(teamManager.getPlayerTeam(player.getUniqueId()));
        if (team == null) {
            player.sendMessage(ChatColor.RED + "You are not in a team.");
            return;
        }

        String teamName = team.getName();
        glowingTeamMembers.clear();
        glowingTeamMembers.addAll(team.getMembers().keySet());

        glowEventBossBar.setVisible(true);
        glowEventBossBar.setProgress(1.0);
        Bukkit.getOnlinePlayers().forEach(glowEventBossBar::addPlayer);

        Bukkit.broadcastMessage(ChatColor.GREEN + "Glowing Event: kill the players that are glowing for 10 points!");

        glowEventTask = new BukkitRunnable() {
            int remainingTime = time;

            @Override
            public void run() {
                if (remainingTime <= 0 || glowingTeamMembers.isEmpty()) {
                    stopGlowEvent();
                    Bukkit.broadcastMessage(ChatColor.GREEN + "Glowing Event ended all players have died!");
                    cancel();
                    return;
                }

                double progress = (double) remainingTime / time;
                glowEventBossBar.setProgress(progress);
                glowEventBossBar.setTitle("Glowing Event: Kill " + teamName + " (" + remainingTime + "s)");
                remainingTime--;
            }
        }.runTaskTimer(plugin, 0, 20); // Run every second (20 ticks)

        player.sendMessage(ChatColor.GREEN + "Glow event started for " + time + " seconds.");
    }
    private static class Region {
        private final Location pos1;
        private final Location pos2;

        public Region(Location pos1, Location pos2) {
            this.pos1 = pos1;
            this.pos2 = pos2;
        }
    }

    private void stopGlowEvent() {
        if (glowEventTask != null) {
            glowEventTask.cancel();
        }
        glowEventBossBar.setVisible(false);
        Bukkit.broadcastMessage(ChatColor.GREEN + "The glow event has ended.");
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        Team team = teamManager.getTeam(teamManager.getPlayerTeam(killer.getUniqueId()));

        if (glowingTeamMembers.contains(victim.getUniqueId())) {
            glowingTeamMembers.remove(victim.getUniqueId());

            if (killer != null) {
                team.addCoin(10);
                Bukkit.broadcastMessage(ChatColor.GOLD + killer.getName() + " just got 10 points for killing " + victim.getName() + " because of the glowing event!!");
            }

            if (glowingTeamMembers.isEmpty()) {
                stopGlowEvent();
                Bukkit.broadcastMessage(ChatColor.GREEN + "Glowing Event ended all players have died!");
            }
        }
    }

    private void handleCreateCommand(Player p, String[] args) {
        if (!p.hasPermission("easyteams.team")) {
            p.sendMessage(ChatColor.RED + "You do not have permission to create a team.");
            return;
        }
        if (args.length < 2) {
            p.sendMessage(ChatColor.RED + "Usage: /team create <name>");
            return;
        }
        String teamName = args[1];
        if (teamName == null || teamName.trim().isEmpty()) {
            p.sendMessage(ChatColor.RED + "Team name cannot be empty.");
            return;
        }
        if (teamManager.createTeam(teamName, p)) {
            p.sendMessage(ChatColor.GREEN + "Team " + teamName + " created successfully.");
            new BukkitRunnable() {
                @Override
                public void run() {
                    updateAllPlayersNameTags();
                }
            }.runTaskLater(teamManager.getPlugin(), 20L);

        } else {
            p.sendMessage(ChatColor.RED + "A team with that name already exists or you are already in a team.");
        }
    }

    private void handleInviteCommand(Player p, String[] args) {
        String playerTeamName = teamManager.getPlayerTeam(p.getUniqueId());
        Team team = teamManager.getTeam(playerTeamName);
        if (team == null) {
            p.sendMessage(ChatColor.RED + "Team not found.");
            return;
        }

        String prank = team.getMembers().get(p.getUniqueId());
        if (prank == null) {
            p.sendMessage(ChatColor.RED + "Your rank in the team could not be determined.");
            return;
        }

        if (args.length < 2) {
            p.sendMessage(ChatColor.RED + "Usage: /team invite <player>");
            return;
        }
        Player invitee = Bukkit.getPlayer(args[1]);
        if (invitee == null) {
            p.sendMessage(ChatColor.RED + "Player not found.");
            return;
        }
        if (!"leader".equalsIgnoreCase(prank) && !"co-leader".equalsIgnoreCase(prank)) {
            p.sendMessage(ChatColor.RED + "You must be the leader/co-leader to invite members.");
            return;
        }
        if (team.getMembers().containsKey(invitee.getUniqueId())) {
            p.sendMessage(ChatColor.RED + "Player is already in the team.");
            return;
        }
        if (teamManager.invitePlayer(p, invitee)) {
            updateAllPlayersNameTags();
            p.sendMessage(ChatColor.GREEN + "Invitation sent to " + invitee.getName() + ".");
            team.broadcastMessage(ChatColor.BLUE + "Player " + ChatColor.WHITE + invitee.getName() + ChatColor.BLUE + " has been invited to the team.");
        } else {
            p.sendMessage(ChatColor.RED + "You are not in a team.");
        }
    }

    private void handleJoinCommand(Player p, String[] args) {
        if (args.length < 2) {
            p.sendMessage(ChatColor.RED + "Usage: /team join <team>");
            return;
        }

        if(teamManager.getPlayerTeam(p.getUniqueId()) != null){
            p.sendMessage(ChatColor.RED + "You are already in a team.");
            return;
            }
        if (teamManager.joinTeam(p, args[1])) {
            updatePlayerNameTag(p);
            p.sendMessage(ChatColor.GREEN + "You joined the team " + args[1] + ".");
            Team team = teamManager.getTeam(args[1]);
            if (team != null) {
                team.broadcastMessage(ChatColor.BLUE + "Player " + ChatColor.WHITE + p.getName() + ChatColor.BLUE + " has joined the team.");
            }
        } else {
            p.sendMessage(ChatColor.RED + "Failed to join the team.");
        }
    }

    private void handleRenameCommand(Player p, String[] args) {
        String playerTeamName = teamManager.getPlayerTeam(p.getUniqueId());
        Team team = teamManager.getTeam(playerTeamName);
        if (team == null) {
            p.sendMessage(ChatColor.RED + "Team not found.");
            return;
        }

        String prank = team.getMembers().get(p.getUniqueId());
        if (prank == null) {
            p.sendMessage(ChatColor.RED + "Your rank in the team could not be determined.");
            return;
        }

        if (args.length < 2) {
            p.sendMessage(ChatColor.RED + "Usage: /team rename <newname>");
            return;
        }
        if (!"leader".equalsIgnoreCase(prank) && !"co-leader".equalsIgnoreCase(prank)) {
            p.sendMessage(ChatColor.RED + "You must be the leader/co-leader to rename the team.");
            return;
        }
        String oldName = teamManager.getPlayerTeam(p.getUniqueId());
        if (oldName == null || !teamManager.renameTeam(oldName, args[1])) {
            p.sendMessage(ChatColor.RED + "Failed to rename the team.");
        } else {
            p.sendMessage(ChatColor.GREEN + "Team renamed to " + args[1] + ".");
            team.broadcastMessage(ChatColor.BLUE + "The team has been renamed to " + ChatColor.WHITE + args[1] + ChatColor.BLUE + ".");
        }
    }

    private void handleDemoteCommand(Player p, String[] args) {
        String playerTeamName = teamManager.getPlayerTeam(p.getUniqueId());
        Team team = teamManager.getTeam(playerTeamName);
        if (team == null) {
            p.sendMessage(ChatColor.RED + "Team not found.");
            return;
        }

        String prank = team.getMembers().get(p.getUniqueId());
        if (prank == null) {
            p.sendMessage(ChatColor.RED + "Your rank in the team could not be determined.");
            return;
        }

        if (args.length < 2) {
            p.sendMessage(ChatColor.RED + "Usage: /team demote <player>");
            return;
        }

        Player targetDemote = Bukkit.getPlayer(args[1]);
        if (targetDemote == null) {
            p.sendMessage(ChatColor.RED + "Player not found.");
            return;
        }

        if (targetDemote.equals(p)) {
            p.sendMessage(ChatColor.RED + "You cannot demote yourself.");
            return;
        }

        if (!"leader".equalsIgnoreCase(prank) && !"co-leader".equalsIgnoreCase(prank)) {
            p.sendMessage(ChatColor.RED + "You must be the team leader/co-leader to demote members.");
            return;
        }

        if (!team.getMembers().containsKey(targetDemote.getUniqueId())) {
            p.sendMessage(ChatColor.RED + "This player is not in your team.");
            return;
        }

        String currentRankDemote = team.getMembers().get(targetDemote.getUniqueId());
        if ("leader".equalsIgnoreCase(currentRankDemote)) {
            p.sendMessage(ChatColor.RED + "You cannot demote the team leader.");
            return;
        }

        String newRankDemote = switch (currentRankDemote.toLowerCase()) {
            case "co-leader" -> "captain";
            case "captain" -> "member";
            default -> null;
        };

        if (newRankDemote == null) {
            p.sendMessage(ChatColor.RED + "Cannot demote further.");
        } else {
            team.getMembers().put(targetDemote.getUniqueId(), newRankDemote);
            p.sendMessage(ChatColor.GREEN + targetDemote.getName() + " has been demoted to " + newRankDemote + ".");
            team.broadcastMessage(ChatColor.BLUE + "Player " + ChatColor.WHITE + targetDemote.getName() + ChatColor.BLUE + " has been demoted to " + ChatColor.WHITE + newRankDemote + ChatColor.BLUE + ".");
        }
    }

    private void handleInfoCommand(Player p, String[] args) {
        if (args.length == 1) {
            String playerTeamName = teamManager.getPlayerTeam(p.getUniqueId());
            Team team = teamManager.getTeam(playerTeamName);
            if (team == null) {
                p.sendMessage(ChatColor.RED + "You are not in a team.");
            } else if (team != null) {
                sendTeamInfo(p, team);
            }
        } else {
            Team team = teamManager.getTeam(args[1]);
            if (team == null) {
                p.sendMessage(ChatColor.RED + "Team not found.");
            } else if (team != null) {
                sendTeamInfo(p, team);
            }
        }
    }

    private void sendTeamInfo(Player p, Team team) {
        String teamInfo = team.formatInfo();
        if (p.hasPermission("easyteams.admin") || p.isOp()) {
            TextComponent deleteButton = new TextComponent(ChatColor.RED + "[Delete Team]");
            deleteButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/team deleteconfirm " + team.getName()));

            TextComponent manageButton = new TextComponent(ChatColor.GREEN + " [Manage Team]");
            manageButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/team manage " + team.getName()));

            String[] infoParts = teamInfo.split("\n");
            p.spigot().sendMessage(new TextComponent(infoParts[0]));
            p.spigot().sendMessage(deleteButton, manageButton);
            for (int i = 1; i < infoParts.length; i++) {
                p.sendMessage(infoParts[i]);
            }
        } else {
            p.sendMessage(teamInfo);
        }
    }
    public void updatePlayerNameTag(Player player) {
        Plugin tabPlugin = Bukkit.getPluginManager().getPlugin("TAB");
        if (tabPlugin == null || !tabPlugin.isEnabled()) {
            return;
        }

        TabPlayer tabPlayer = TabAPI.getInstance().getPlayer(player.getUniqueId());
        if (tabPlayer == null) return;

        String playerTeamName = teamManager.getPlayerTeam(player.getUniqueId());
        String nameTag = "";

        if (playerTeamName != null) {
            nameTag = ChatColor.GRAY + " [" + ChatColor.YELLOW + playerTeamName + ChatColor.GRAY + "]";
        }

        teamManager.setPrefix(tabPlayer, nameTag);
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

    private void handleKickCommand(Player p, String[] args) {
        String playerTeamName = teamManager.getPlayerTeam(p.getUniqueId());
        Team team = teamManager.getTeam(playerTeamName);
        if (team == null) {
            p.sendMessage(ChatColor.RED + "Team not found.");
            return;
        }

        String prank = team.getMembers().get(p.getUniqueId());
        if (prank == null) {
            p.sendMessage(ChatColor.RED + "Your rank in the team could not be determined.");
            return;
        }

        if (args.length < 2) {
            p.sendMessage(ChatColor.RED + "Usage: /team kick <player>");
            return;
        }
        Player targetKick = Bukkit.getPlayer(args[1]);
        if (targetKick == null) {
            p.sendMessage(ChatColor.RED + "Player not found.");
            return;
        }
        if (!"leader".equalsIgnoreCase(prank) && !"co-leader".equalsIgnoreCase(prank)) {
            p.sendMessage(ChatColor.RED + "You must be the leader/co-leader to kick members.");
            return;
        }
        if (teamManager.kickPlayer(p, targetKick)) {
            updateAllPlayersNameTags();
            p.sendMessage(ChatColor.GREEN + "You kicked " + targetKick.getName() + ".");
            team.broadcastMessage(ChatColor.BLUE + "Player " + ChatColor.WHITE + targetKick.getName() + ChatColor.BLUE + " has been kicked from the team.");
        } else {
            p.sendMessage(ChatColor.RED + "Failed to kick the player.");
        }
    }

    private void handlePvpCommand(Player p) {
        String playerTeamName = teamManager.getPlayerTeam(p.getUniqueId());
        Team team = teamManager.getTeam(playerTeamName);
        if (team == null) {
            p.sendMessage(ChatColor.RED + "You are not in a team.");
            return;
        }

        String prank = team.getMembers().get(p.getUniqueId());
        if (prank == null) {
            p.sendMessage(ChatColor.RED + "Your rank in the team could not be determined.");
            return;
        }

        if (!"leader".equalsIgnoreCase(prank) && !"co-leader".equalsIgnoreCase(prank)) {
            p.sendMessage(ChatColor.RED + "You must be the leader/co-leader to toggle PvP.");
            return;
        }

        boolean pvpEnabled = !team.isPvPEnabled();
        team.setPvPEnabled(pvpEnabled);
        p.sendMessage(ChatColor.BLUE + "PvP has been " + (pvpEnabled ? ChatColor.WHITE + "enabled" : ChatColor.WHITE + "disabled") + ChatColor.BLUE + " for your team.");
        team.broadcastMessage(ChatColor.BLUE + "PvP has been " + (pvpEnabled ? ChatColor.WHITE + "enabled" : ChatColor.WHITE + "disabled") + ChatColor.BLUE + " for the team.");
    }

    private void handleDisbandCommand(Player p) {
        String playerTeamName = teamManager.getPlayerTeam(p.getUniqueId());
        Team team = teamManager.getTeam(playerTeamName);
        if (team == null || !"leader".equalsIgnoreCase(team.getMembers().get(p.getUniqueId()))) {
            p.sendMessage(ChatColor.RED + "You must be the leader to disband the team.");
            return;
        }
        teamManager.deleteTeam(playerTeamName);
        updateAllPlayersNameTags();
        p.sendMessage(ChatColor.GREEN + "Team " + playerTeamName + " has been disbanded.");
        team.broadcastMessage(ChatColor.BLUE + "The team has been disbanded by the leader.");
    }

    private void handleDeleteConfirmCommand(Player p, String[] args) {
        if (args.length < 2 || (!p.hasPermission("easyteams.admin") && !p.isOp())) {
            p.sendMessage(ChatColor.RED + "You do not have permission to delete teams.");
            return;
        }
        Team teamToDelete = teamManager.getTeam(args[1]);
        if (teamToDelete == null) {
            p.sendMessage(ChatColor.RED + "Team not found.");
            return;
        }
        TextComponent confirmDelete = new TextComponent(ChatColor.RED + "Are you sure you want to delete this team? ");
        TextComponent yesOption = new TextComponent(ChatColor.GREEN + "[Yes]");
        TextComponent noOption = new TextComponent(ChatColor.RED + " [No]");

        yesOption.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/team delete " + args[1]));
        noOption.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/team info " + args[1]));

        p.spigot().sendMessage(confirmDelete, yesOption, noOption);
    }

    private void handlePromoteCommand(Player p, String[] args) {
        String playerTeamName = teamManager.getPlayerTeam(p.getUniqueId());
        Team team = teamManager.getTeam(playerTeamName);
        if (team == null) {
            p.sendMessage(ChatColor.RED + "Team not found.");
            return;
        }

        String prank = team.getMembers().get(p.getUniqueId());
        if (prank == null) {
            p.sendMessage(ChatColor.RED + "Your rank in the team could not be determined.");
            return;
        }

        if (args.length < 2) {
            p.sendMessage(ChatColor.RED + "Usage: /team promote <player>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            p.sendMessage(ChatColor.RED + "Player not found.");
            return;
        }

        if (target.equals(p)) {
            p.sendMessage(ChatColor.RED + "You cannot promote yourself.");
            return;
        }

        if (!"leader".equalsIgnoreCase(prank) && !"co-leader".equalsIgnoreCase(prank)) {
            p.sendMessage(ChatColor.RED + "You must be the team leader/co-leader to promote members.");
            return;
        }

        if (!team.getMembers().containsKey(target.getUniqueId())) {
            p.sendMessage(ChatColor.RED + "This player is not in your team.");
            return;
        }

        String currentRank = team.getMembers().get(target.getUniqueId());
        String newRank = switch (currentRank.toLowerCase()) {
            case "member" -> "captain";
            case "captain" -> "co-leader";
            default -> null;
        };

        if (newRank == null) {
            p.sendMessage(ChatColor.RED + "Cannot promote further.");
        } else {
            team.getMembers().put(target.getUniqueId(), newRank);
            p.sendMessage(ChatColor.GREEN + target.getName() + " has been promoted to " + newRank + ".");
            team.broadcastMessage(ChatColor.BLUE + "Player " + ChatColor.WHITE + target.getName() + ChatColor.BLUE + " has been promoted to " + ChatColor.WHITE + newRank + ChatColor.BLUE + ".");
        }
    }

    private void handleDeleteCommand(Player p, String[] args) {
        if (args.length < 2 || (!p.hasPermission("easyteams.admin") && !p.isOp())) {
            p.sendMessage(ChatColor.RED + "You do not have permission to delete teams.");
            return;
        }
        Team teamToRemove = teamManager.getTeam(args[1]);
        if (teamToRemove == null) {
            p.sendMessage(ChatColor.RED + "Team not found.");
            return;
        }
        teamManager.getAllTeams().remove(args[1].toLowerCase());
        teamToRemove.getMembers().keySet().forEach(uuid -> teamManager.playerTeams.remove(uuid));
        p.sendMessage(ChatColor.GREEN + "Team " + args[1] + " has been deleted.");
        updateAllPlayersNameTags();
        teamToRemove.broadcastMessage(ChatColor.RED + "Your team has been deleted by an admin.");
    }

    private void handleReloadCommand(Player p) {
        if (!p.hasPermission("easyteams.admin")) {
            p.sendMessage(ChatColor.RED + "You do not have permission to reload the plugin.");
            return;
        }
        teamManager.saveTeams();
        teamManager.loadTeams();
        updateAllPlayersNameTags();
        p.sendMessage(ChatColor.GREEN + "Plugin reloaded.");
    }

    private void handleManageCommand(Player p, String[] args) {
        if (args.length < 2 || (!p.hasPermission("easyteams.admin") && !p.isOp())) {
            p.sendMessage(ChatColor.RED + "You do not have permission to manage teams.");
            return;
        }
        Team teamToManage = teamManager.getTeam(args[1]);
        if (teamToManage == null) {
            p.sendMessage(ChatColor.RED + "Team not found.");
            return;
        }

        Inventory inventory = Bukkit.createInventory(null, 36, "Manage Team: " + teamToManage.getName());

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

        p.openInventory(inventory);
        teamManager.updateItems(p, teamToManage);
    }

    private void handleForceJoinCommand(Player sender, String[] args) {
        if (!sender.hasPermission("easyteams.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to force join teams.");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /team forcejoin <team> [player]");
            return;
        }

        String teamName = args[1];
        Player targetPlayer = sender;

        if (args.length > 2) {
            targetPlayer = Bukkit.getPlayer(args[2]);
            if (targetPlayer == null) {
                sender.sendMessage(ChatColor.RED + "Player not found.");
                return;
            }
        }

        if (!teamManager.teamExists(teamName)) {
            sender.sendMessage(ChatColor.RED + "Team not found.");
            return;
        }

        if (teamManager.forceJoinTeam(targetPlayer, teamName)) {
            sender.sendMessage(ChatColor.GREEN + "Player " + targetPlayer.getName() + " has been forcefully added to the team " + teamName + ".");
            updateAllPlayersNameTags();
            Team team = teamManager.getTeam(teamName);
            if (team != null) {
                String funMessage;
                if (targetPlayer.equals(sender)) {
                    funMessage = ChatColor.GREEN + "=============================\n" +
                            ChatColor.RED + "" + ChatColor.BOLD + sender.getName() + " has force joined your team. Be scared!\n" +
                            ChatColor.GREEN + "=============================";
                } else {
                    funMessage = ChatColor.GREEN + "=============================\n" +
                            ChatColor.RED + "" + ChatColor.BOLD + targetPlayer.getName() + " has been forcefully joined to your team by " + sender.getName() + ". Be scared!\n" +
                            ChatColor.GREEN + "=============================";
                }
                team.broadcastMessage(funMessage);
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Failed to force join the player to the team.");
        }
    }

    private void handleForceLeaderCommand(Player p) {
        String playerTeamName = teamManager.getPlayerTeam(p.getUniqueId());
        Team team = teamManager.getTeam(playerTeamName);
        if (team == null || (!p.hasPermission("easyteams.admin") && !p.isOp())) {
            p.sendMessage(ChatColor.RED + "You do not have permission to force leader.");
            return;
        }

        team.changeLeader(p.getUniqueId());
        team.broadcastMessage(ChatColor.GREEN + "===========================================");
        team.broadcastMessage(ChatColor.RED + "" + ChatColor.BOLD + p.getName() + " has hijacked your team and is now the leader. WATCH OUT!");
        team.broadcastMessage(ChatColor.GREEN + "===========================================");
    }

    private void handleLeaveCommand(Player p) {
        String playerTeamName = teamManager.getPlayerTeam(p.getUniqueId());
        Team team = teamManager.getTeam(playerTeamName);
        if (team == null) {
            p.sendMessage(ChatColor.RED + "You are not in a team.");
            return;
        }
        if ("leader".equals(team.getMembers().get(p.getUniqueId()))) {
            p.sendMessage(ChatColor.RED + "You cannot leave while being the leader of the team.");
            return;
        }
        if (team.kickMember(p.getUniqueId())) {
            p.sendMessage(ChatColor.GREEN + "You left the team.");
            updateAllPlayersNameTags();
            team.broadcastMessage(ChatColor.BLUE + "Player " + ChatColor.WHITE + p.getName() + ChatColor.BLUE + " has left the team.");
        } else {
            p.sendMessage(ChatColor.RED + "Failed to leave the team.");
        }

    }
    private void handleHelpCommand(Player p) {
        p.sendMessage(ChatColor.GREEN + "=============================");
        p.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "EasyTeams Help:");
        p.sendMessage(ChatColor.GREEN + "");
        p.sendMessage(ChatColor.GREEN + "[] < not required <> < required");
        p.sendMessage(ChatColor.GREEN + "/team create <name>: Create a new team.");
        p.sendMessage(ChatColor.GREEN + "/team join <name>: Join a team.");
        p.sendMessage(ChatColor.GREEN + "/team leave: Leave your current team.");
        p.sendMessage(ChatColor.GREEN + "/team promote <player>: Promote a member a member.");
        p.sendMessage(ChatColor.GREEN + "/team demote <player>: Demote a member); ");
        p.sendMessage(ChatColor.GREEN + "/team kick <player>: Kick a member from the team.");
        p.sendMessage(ChatColor.GREEN + "/team info <name>: Get information about a team.");
        p.sendMessage(ChatColor.GREEN + "/team list: List all available teams.");
        p.sendMessage(ChatColor.GREEN + "/team rename <new name>: Rename your team");
        p.sendMessage(ChatColor.GREEN + "/team pvp: Toggle PvP status for your team.");
        p.sendMessage(ChatColor.GREEN + "/team disband: Disband your team.");
      if(p.hasPermission("easyteams.admin")) {
          p.sendMessage(ChatColor.GREEN + "/team reload: Reload the plugin configuration.");
          p.sendMessage(ChatColor.GREEN + "/team manage <name>: Manage a team as an admin.");
          p.sendMessage(ChatColor.GREEN + "/team delete <name>: Delete a team as an admin.");
          p.sendMessage(ChatColor.GREEN + "/team forcejoin <team> [player]: Forcefully add a player to a team.");
          p.sendMessage(ChatColor.GREEN + "/team forceleader: Take over leadership of your own team.");
      }
        p.sendMessage(ChatColor.GREEN + "=============================");
      return;
    }

    private void handleEventCommand(Player p, String[] args) {
        if (args.length < 2) {
            p.sendMessage(ChatColor.RED + "Usage: /team event <coin|glowing> <enable|disable> [time] [multiplier]");
            return;
        }
        switch (args[1].toLowerCase()) {
            case "coin":
                handleCoinEventCommand(p, args);
                break;
            case "glowing":
                handleGlowingEventCommand(p, args);
                break;
            default:
                p.sendMessage(ChatColor.RED + "Unknown event type.");
                break;
        }
    }

    private void handleCoinEventCommand(Player p, String[] args) {
        if (!p.hasPermission("easyteams.admin")) {
            p.sendMessage(ChatColor.RED + "You do not have permission to manage coin events.");
            return;
        }

        if (args.length < 3) {
            p.sendMessage(ChatColor.RED + "Usage: /team event coin <enable|disable> [time] [multiplier]");
            return;
        }

        String action = args[2].toLowerCase();
        if (action.equals("enable")) {
            if (args.length < 5) {
                p.sendMessage(ChatColor.RED + "Usage: /team event coin enable <time> <multiplier>");
                return;
            }

            int time;
            int multiplier;
            try {
                time = parseTime(args[3]);
                multiplier = Integer.parseInt(args[4]);
            } catch (IllegalArgumentException e) {
                p.sendMessage(ChatColor.RED + "Invalid time or multiplier format.");
                return;
            }

            startCoinEvent(p, time, multiplier);
        } else if (action.equals("disable")) {
            stopCoinEvent();
            Bukkit.broadcastMessage(ChatColor.RED + "Coin Event has been cancelled!");
        } else {
            p.sendMessage(ChatColor.RED + "Unknown action. Use enable or disable.");
        }
    }
    private void startCoinEvent(Player player, int time, int multiplier) {
        teamManager.setCoinEventActive(true);
        teamManager.setCoinMultiplier(multiplier);

        coinEventBossBar.setVisible(true);
        coinEventBossBar.setProgress(1.0);
        Bukkit.getOnlinePlayers().forEach(coinEventBossBar::addPlayer);

        Bukkit.broadcastMessage(ChatColor.GREEN + "Coin Event: Earn more coins!");

        coinEventTask = new BukkitRunnable() {
            int remainingTime = time;

            @Override
            public void run() {
                if (remainingTime <= 0) {
                    stopCoinEvent();
                    Bukkit.broadcastMessage(ChatColor.GREEN + "Coin Event has ended!");
                    cancel();
                    return;
                }

                double progress = (double) remainingTime / time;
                coinEventBossBar.setProgress(progress);
                coinEventBossBar.setTitle("Coin Event: " + remainingTime + "s remaining");
                remainingTime--;
            }
        }.runTaskTimer(plugin, 0, 20); // Run every second (20 ticks)

        player.sendMessage(ChatColor.GREEN + "Coin event started for " + time + " seconds with a multiplier of " + multiplier + ".");
    }

    private void stopCoinEvent() {
        if (coinEventTask != null) {
            coinEventTask.cancel();
        }
        coinEventBossBar.setVisible(false);
        teamManager.setCoinEventActive(false);
        Bukkit.broadcastMessage(ChatColor.GREEN + "The coin event has ended.");
    }

    private int parseTime(String time) {
        int duration = 0;
        try {
            if (time.endsWith("s")) {
                duration = Integer.parseInt(time.replace("s", "")) * 20; // seconds to ticks
            } else if (time.endsWith("m")) {
                duration = Integer.parseInt(time.replace("m", "")) * 60 * 20; // minutes to ticks
            } else if (time.endsWith("h")) {
                duration = Integer.parseInt(time.replace("h", "")) * 60 * 60 * 20; // hours to ticks
            } else if (time.endsWith("d")) {
                duration = Integer.parseInt(time.replace("d", "")) * 24 * 60 * 60 * 20; // days to ticks
            } else {
                throw new IllegalArgumentException("Invalid time format");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid time format", e);
        }
        return duration;
    }

    private void handleGlowingEventCommand(Player p, String[] args) {
        if (!p.hasPermission("easyteams.admin")) {
            p.sendMessage(ChatColor.RED + "You do not have permission to manage glowing events.");
            return;
        }

        if (args.length < 3) {
            p.sendMessage(ChatColor.RED + "Usage: /team event glowing <enable|disable> [time]");
            return;
        }

        String action = args[2].toLowerCase();
        if (action.equals("enable")) {
            if (args.length < 4) {
                p.sendMessage(ChatColor.RED + "Usage: /team event glowing enable <time>");
                return;
            }

            int time;
            try {
                time = parseTime(args[3]);
            } catch (IllegalArgumentException e) {
                p.sendMessage(ChatColor.RED + "Invalid time format.");
                return;
            }

            startGlowEvent(p, time);
        } else if (action.equals("disable")) {
            stopGlowEvent();
            Bukkit.broadcastMessage(ChatColor.RED + "Glowing Event has been cancelled!");
        } else {
            p.sendMessage(ChatColor.RED + "Unknown action. Use enable or disable.");
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Player) {
            Player victim = (Player) entity;
            Player killer = victim.getKiller();
            String victimTeamName = teamManager.getPlayerTeam(victim.getUniqueId());
            if (victimTeamName != null) {
                Team victimTeam = teamManager.getTeam(victimTeamName);
                if (victimTeam != null) {
                    victimTeam.addDeath();
                    victimTeam.removecoin(1);
                    victimTeam.broadcastMessage(ChatColor.BLUE + "Your team lost 1 coin because " + ChatColor.WHITE + victim.getName() + ChatColor.BLUE + " died.");
                }
            }
            if (killer != null) {
                String killerTeamName = teamManager.getPlayerTeam(killer.getUniqueId());
                if (killerTeamName != null) {
                    Team killerTeam = teamManager.getTeam(killerTeamName);
                    if (killerTeam != null) {
                        killerTeam.addKill();
                        int coinsEarned = 1 * coinMultiplier;
                        killerTeam.addCoins(coinsEarned);
                        killerTeam.broadcastMessage(ChatColor.BLUE + "Your has earned " + ChatColor.WHITE + coinsEarned + ChatColor.BLUE + " coin(s) for killing " + ChatColor.WHITE + victim.getName());
                    }
                }
            }
        }
    }
}
