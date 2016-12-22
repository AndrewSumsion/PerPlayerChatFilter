package io.github.AndrewSumsion.ppcf;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.java.JavaPlugin;
import org.apache.commons.lang3.StringUtils;

public final class PPCF extends JavaPlugin implements Listener {
	public ArrayList<String> badWords = ((ArrayList<String>)getConfig().getStringList("badwords"));
	@Override
	public void onEnable() {
		// Register Events
		getServer().getPluginManager().registerEvents(this, this);

		// Register Permissions
		Permission[] permissions = {
				new Permission("ppcf.filter"),
				new Permission("ppcf.toggle"),
				new Permission("ppcf.reload"),
				new Permission("ppcf.edit")};
		for(Permission perm : permissions){
			try {
				getServer().getPluginManager().addPermission(perm);
				getLogger().info(perm.getName()+" permission was registered");
			} catch(Exception e) {

			}
		}
		// Take care of config
		File configFile = new File(getDataFolder(), "config.yml");
		if (!configFile.exists()) {
			saveDefaultConfig();
		}
		reloadConfig();
	}
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equalsIgnoreCase("filter") || cmd.getName().equalsIgnoreCase("ppcf")) { 
			Player player = null;
			if(!(sender instanceof Player) && args.length == 0) {
				sender.sendMessage(""+ChatColor.DARK_RED+ChatColor.BOLD+"You must be a player!");
				return true;
			}
			if(!(sender instanceof Player) && !(args[0].equals("reload"))) {
				sender.sendMessage(""+ChatColor.DARK_RED+ChatColor.BOLD+"You must be a player!");
			}
			else {
			player = ((Player) sender);
			}
			// Toggle Filter
			if(args.length == 0) {
				toggleFilter(player);
				// Reload Config
			} else if(args[0].equalsIgnoreCase("reload")) {
					reloadCommand(player);
				// Add Bad Word
			} else if(args[0].equalsIgnoreCase("add")) {
				addWord(args[1], player);
				// Remove Bad Word
			} else if(args[0].equalsIgnoreCase("remove")) {
				removeWord(args[1], player);
			} else if(args[0].equalsIgnoreCase("status")) {
				filterStatus(player);
			}
			else {
				usage(player);
			}
			return true;
		}
		return false; 
	}
	@SuppressWarnings("static-access")
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onChat(AsyncPlayerChatEvent event) {
		if(event.isCancelled()) {
			return;
		}
		boolean containsWords = false;
		for(String word : badWords) {
			if(StringUtils.containsIgnoreCase(event.getMessage(), word)) {containsWords = true;}
		}
		if(containsWords) {
			String badMessage = event.getMessage();
			String stringList = "";
			for(String word : badWords) {
				stringList = stringList+word+"|";
			}
			stringList=stringList.substring(0, stringList.length()-1);
			Pattern regex = Pattern.compile(stringList, Pattern.CASE_INSENSITIVE);
			Matcher matcher = regex.matcher(badMessage);
			badMessage = matcher.replaceAll("****");
			for (Player r : Bukkit.getOnlinePlayers()) {
				if(r.hasPermission("ppcf.filter")) {
					event.getRecipients().remove(r);
					r.sendMessage(event.getMessage().format(event.getFormat(), event.getPlayer().getDisplayName(), badMessage));
				}
			}
		}
	}
	public void toggleFilter(Player player) {
		if(player.hasPermission("ppcf.toggle")) {
			PermissionAttachment attachment = player.addAttachment(this);
			if(player.hasPermission("ppcf.filter")) {
				attachment.setPermission("ppcf.filter", false);
				filterStatus(player);
			} else {
				attachment.setPermission("ppcf.filter", true);
				filterStatus(player);
			}
		} else {
			noPerm(player);
		}
	}
	@EventHandler
	public void onLogin(PlayerJoinEvent event) {
		filterStatus(event.getPlayer());
		event.getPlayer().sendMessage(""+ChatColor.GOLD+"Use "+ChatColor.DARK_RED+"/filter "+ChatColor.GOLD+"to toggle.");
	}
	public void reloadCommand(Player player) {
		if(player.hasPermission("ppcf.reload")) {
		try {
			reloadConfig();
			player.sendMessage(""+ChatColor.GREEN+"PerPlayerChatFilter successfully reloaded!");
		} catch(Exception e) {
			player.sendMessage(""+ChatColor.RED+"Reload failed with exception: "+e);
		}
		} else {noPerm(player);}
	}
	public void addWord(String word, Player player) {
		if(player.hasPermission("ppcf.edit")) {
			badWords.add(word);
			getConfig().set("badwords", (List<String>)badWords);
			player.sendMessage(""+ChatColor.GREEN+"\""+word+"\" was added to the list of bad words.");
		} else {noPerm(player);}
	}
	public void removeWord(String word, Player player) {
		if(player.hasPermission("ppcf.edit")) {
			try {
				badWords.remove(word);
			} catch(Exception e) {
				player.sendMessage(""+ChatColor.RED+"Error while removing \""+word+"\"");
				return;
			}
		} else {noPerm(player);}
		getConfig().set("badwords", (List<String>)badWords);
		player.sendMessage(""+ChatColor.GREEN+"\""+word+"\" was removed from the list of bad words.");
	}
	public void noPerm(Player player) {
		player.sendMessage(""+ChatColor.RED+"Insufficient Permissions");
	}
	public void filterStatus(Player player) {
		if(player.hasPermission("ppcf.filter")) {
			player.sendMessage(""+ChatColor.GOLD+"Your chat filter is "+ChatColor.DARK_GREEN+ChatColor.BOLD+"ON");
		}
		if(!(player.hasPermission("ppcf.filter"))) {
			player.sendMessage(""+ChatColor.GOLD+"Your chat filter is "+ChatColor.DARK_RED+ChatColor.BOLD+"OFF");
		}
	}
	public void usage(Player player) {
		String[] info = {
				""+ChatColor.RED+ChatColor.UNDERLINE+"Usage:",
				""+ChatColor.GRAY+"/filter - Toggles Filter",
				""+ChatColor.GRAY+"/filter status - Displays filter status",
				""+ChatColor.GRAY+"/filter add <word> - Adds a word to be filtered",
				""+ChatColor.GRAY+"/filter remove <word> - Removes a filtered word",
				""+ChatColor.GRAY+"/filter reload - Reloads PerPlayerChatFilter config",
		};
		player.sendMessage(info);
	}
	@Override
	public void onDisable() {
		getLogger().log(Level.INFO, "Bye");
		saveConfig();
	}
}