package me.clearedspore.easyTeams.Listener;

import me.clearedspore.easyTeams.Utils.TeamManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.HashMap;
import java.util.Map;

public class RenameListener implements Listener {
    private static final Map<Player, String> renameRequests = new HashMap<>();
    private final TeamManager teamManager;

    public RenameListener(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    public static void addPlayerToRenameList(Player player, String teamName) {
        renameRequests.put(player, teamName);
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!renameRequests.containsKey(player)) return;

        event.setCancelled(true); // Cancel the chat event to prevent the message from being broadcast

        String newTeamName = event.getMessage().trim();
        String oldTeamName = renameRequests.remove(player);

        if (teamManager.renameTeam(oldTeamName, newTeamName)) {
            player.sendMessage(ChatColor.GREEN + "Team renamed to " + newTeamName + ".");
            teamManager.updateAllPlayersNameTags();
        } else {
            player.sendMessage(ChatColor.RED + "Failed to rename the team. The name might already be taken.");
        }
    }
}
