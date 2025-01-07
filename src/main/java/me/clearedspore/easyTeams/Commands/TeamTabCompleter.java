package me.clearedspore.easyTeams.Commands;

import me.clearedspore.easyTeams.Utils.TeamManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Collectors;

public class TeamTabCompleter implements TabCompleter {
    private final TeamManager teamManager;

    public TeamTabCompleter(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            if (sender.hasPermission("easyteams.team")) {
                completions.addAll(Arrays.asList("create", "invite", "join", "kick", "rename", "info", "disband", "pvp", "promote", "demote", "help"));
            }
            if (sender.hasPermission("easyteams.admin")) {
                completions.addAll(Arrays.asList("reload", "manage", "delete", "forcejoin", "forceleader", "event", "region", "troll"));
            }
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("manage"))) {
            completions.addAll(teamManager.getAllTeams().keySet());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("event")) {
            completions.addAll(Arrays.asList("coin", "glowing"));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("event") && args[1].equalsIgnoreCase("coin") || args[1].equalsIgnoreCase("glowing")) {
            completions.addAll(Arrays.asList("disable", "enable"));
        } else if (args.length == 4 && args[0].equalsIgnoreCase("event") && args[2].equalsIgnoreCase("enable")) {
            completions.addAll(Arrays.asList("1s", "1m", "1h", "1d"));
        } else if (args.length == 5 && args[0].equalsIgnoreCase("event") && args[2].equalsIgnoreCase("enable")) {
            completions.addAll(Arrays.asList("x1", "x2", "x3", "x4"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("region")) {
            completions.addAll(Arrays.asList("wand", "set", "delete"));
        }
            return completions.stream()
                    .filter(sub -> sub.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                    .collect(Collectors.toList());
        }
    }
