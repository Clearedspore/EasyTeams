package me.clearedspore.easyTeams;

import me.clearedspore.easyTeams.Commands.TeamCommand;
import me.clearedspore.easyTeams.Commands.TeamTabCompleter;
import me.clearedspore.easyTeams.Listener.*;
import me.clearedspore.easyTeams.Utils.TeamManager;
import org.bukkit.plugin.java.JavaPlugin;
public final class EasyTeams extends JavaPlugin {
    private TeamManager teamManager;
    @Override
    public void onEnable() {
        teamManager = new TeamManager(this);

        teamManager.updateAllPlayersNameTags();
        TeamCommand teamCommand = new TeamCommand(teamManager, this);
        getServer().getPluginManager().registerEvents(new TeamManageListener(teamManager), this);
        getServer().getPluginManager().registerEvents(new RenameListener(teamManager), this);
        getServer().getPluginManager().registerEvents(new CoinEditListener(teamManager), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(teamManager), this);
        getServer().getPluginManager().registerEvents(new PvPListener(teamManager), this);
        getServer().getPluginManager().registerEvents(teamCommand, this);
        getCommand("team").setExecutor(teamCommand);
        getCommand("team").setTabCompleter(new TeamTabCompleter(teamManager));

    }
    public TeamManager getTeamManager() {
        return teamManager;
    }

    @Override
    public void onDisable() {
        teamManager.saveTeams();
    }
}
