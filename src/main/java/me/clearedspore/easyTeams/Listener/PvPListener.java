package me.clearedspore.easyTeams.Listener;

import me.clearedspore.easyTeams.Utils.Team;
import me.clearedspore.easyTeams.Utils.TeamManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class PvPListener implements Listener {
    private final TeamManager teamManager;

    public PvPListener(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) {
            return;
        }

        Player damaged = (Player) event.getEntity();
        Player damager = (Player) event.getDamager();

        String damagedTeamName = teamManager.getPlayerTeam(damaged.getUniqueId());
        String damagerTeamName = teamManager.getPlayerTeam(damager.getUniqueId());

        if (damagedTeamName != null && damagedTeamName.equals(damagerTeamName)) {
            Team team = teamManager.getTeam(damagedTeamName);
            if (team != null && !team.isPvPEnabled()) {
                event.setCancelled(true);
                damager.sendMessage(ChatColor.RED + "PvP is disabled for your team.");
            }
        }
    }
}
