package me.clearedspore.easyTeams.Utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Team {
    private String name;
    private final UUID leader;
    private final Map<UUID, String> members; // Member UUID -> Rank
    private final Map<UUID, String> coleaders; // Member UUID -> Rank
    private final Map<UUID, String> captains; // Member UUID -> Rank
    private final Map<UUID, Boolean> invites; // Invited players
    private boolean pvpEnabled;
    private int coins;
    private int kills;
    private int deaths;

    public Team(String name, UUID leader, Map<UUID, String> coleaders, Map<UUID, String> captains) {
        this.name = name;
        this.leader = leader;
        this.coleaders = coleaders;
        this.captains = captains;
        this.members = new HashMap<>();
        this.invites = new HashMap<>();
        this.members.put(leader, "leader");
        this.coins = 0;
        this.kills = 0;
        this.deaths = 0;
    }

    public Team(String name, UUID leader, Map<UUID, String> members, Map<UUID, String> coleaders, Map<UUID, String> captains, Map<UUID, Boolean> invites) {
        this.name = name;
        this.leader = leader;
        this.members = members;
        this.coleaders = coleaders;
        this.captains = captains;
        this.invites = invites;
        this.coins = 0;
        this.deaths = 0;
        this.kills = 0;
    }

    public boolean isPvPEnabled() {
        return pvpEnabled;
    }

    public void setPvPEnabled(boolean pvpEnabled) {
        this.pvpEnabled = pvpEnabled;
    }

    public void broadcastMessage(String message) {
        for (UUID memberId : members.keySet()) {
            Player player = Bukkit.getPlayer(memberId);
            if (player != null && player.isOnline()) {
                player.sendMessage(ChatColor.BLUE + "[Team] " + ChatColor.RESET + message);
            }
        }
    }
    public int getKills() {
        return kills;
    }

    public void addKill() {
        this.kills++;
    }

    public int getDeaths() {
        return deaths;
    }

    public void addDeath() {
        this.deaths++;
    }

    public UUID getLeader() {
        return leader;
    }
    public Map<UUID, String> getcoleader(){
        return coleaders;
    }
    public Map<UUID, String> getcaptain(){
        return captains;
    }


    public void setMembers(Map<UUID, String> members) {
        this.members.clear();
        this.members.putAll(members);
    }

    public void setCoins(int coins) {
        this.coins = coins;
    }

    public void addCoins(int amount) {
        this.coins += amount;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<UUID, String> getMembers() {
        return members;
    }

    public boolean changeLeader(UUID newLeaderUUID) {
        if (!members.containsKey(newLeaderUUID)) {
            return false; // New leader must be a member of the team
        }
        members.put(leader, "member"); // Demote current leader
        members.put(newLeaderUUID, "leader"); // Promote new leader
        return true;
    }

    public void sendInvite(Player player) {
        invites.put(player.getUniqueId(), true);
        player.sendMessage(ChatColor.GREEN + "You have been invited to join the team " + name + ".");
        net.md_5.bungee.api.chat.TextComponent message = new net.md_5.bungee.api.chat.TextComponent(ChatColor.YELLOW + "[Click to join]");
        message.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/team join " + name));
        player.spigot().sendMessage(message);
    }

    public void expireInvite(Player player) {
        invites.remove(player.getUniqueId());
    }

    public boolean addMember(UUID playerUUID) {
        if (!invites.containsKey(playerUUID)) {
            return false; // Not invited
        }
        invites.remove(playerUUID);
        members.put(playerUUID, "member");
        return true;
    }

    public boolean kickMember(UUID playerUUID) {
        if (!members.containsKey(playerUUID) || leader.equals(playerUUID)) {
            return false; // Cannot kick leader or non-member
        }
        members.remove(playerUUID);
        return true;
    }

    public void addCoin(int amount) {
        this.coins += amount;
    }
    public void removecoin(int amount){
        this.coins -=amount;
    }

    public int getCoins( ) {
        return coins;
    }

    public String formatInfo() {
        StringBuilder sb = new StringBuilder(ChatColor.WHITE + "-----------------------------\n" + ChatColor.WHITE+ name + "\n");
        appendRankInfo(sb, "Leader", "leader");
        appendRankInfo(sb, "Co-Leader", "co-leader");
        appendRankInfo(sb, "Captain", "captain");
        appendRankInfo(sb, "Members", "member");
        sb.append(ChatColor.BLUE).append("Coins: ").append(ChatColor.WHITE).append(coins).append("\n");
        sb.append(ChatColor.BLUE).append("Kills: ").append(ChatColor.WHITE).append(kills).append("\n");
        sb.append(ChatColor.BLUE).append("Deaths: ").append(ChatColor.WHITE).append(deaths).append("\n");
        return sb.toString();
    }

    public boolean promoteMember(UUID playerUUID) {
        String currentRank = members.get(playerUUID);
        String newRank = switch (currentRank.toLowerCase()) {
            case "member" -> "captain";
            case "captain" -> "co-leader";
            case "co-leader" -> "leader";
            default -> null;
        };

        if (newRank == null) {
            return false;
        } else {
            members.put(playerUUID, newRank);
            return true;
        }
    }

    public boolean demoteMember(UUID playerUUID) {
        String currentRank = members.get(playerUUID);
        String newRank = switch (currentRank.toLowerCase()) {
            case "leader" -> "co-leader";
            case "co-leader" -> "captain";
            case "captain" -> "member";
            default -> null;
        };

        if (newRank == null) {
            return false;
        } else {
            members.put(playerUUID, newRank);
            return true;
        }
    }

    private void appendRankInfo(StringBuilder sb, String rankTitle, String rank) {
        sb.append(ChatColor.BLUE).append(rankTitle).append("\n");
        members.entrySet().stream()
                .filter(entry -> entry.getValue().equalsIgnoreCase(rank))
                .forEach(entry -> {
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(entry.getKey());
                    String playerName = offlinePlayer.getName() != null ? offlinePlayer.getName() : "Unknown";
                    sb.append(ChatColor.WHITE).append("- ").append(playerName).append("\n");
                });
    }
}
