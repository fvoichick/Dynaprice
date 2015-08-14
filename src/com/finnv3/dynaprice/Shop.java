package com.finnv3.dynaprice;

import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;

public final class Shop implements CommandExecutor {

	private Dynaprice plugin;

	public Shop(Dynaprice plugin) {
		this.plugin = plugin;
	}

	/**
	 * Gets the official name from an item's unofficial name. The official name
	 * is the one that's the name of the section in the config. The default
	 * values here are generally the in-game name of the item. This method
	 * prioritizes official name, then alias, then bukkit material name.
	 * 
	 * @param name
	 *            the name of the item to search for
	 * @return the official (config) name of the item
	 */
	public String getOfficialName(String name) {
		String bestName = null;
		boolean isAlias = false;
		for (String itemName : plugin.getConfig().getConfigurationSection("items").getKeys(false)) {
			if (plugin.getConfig().isSet("items." + itemName + ".bukkit-name")) {
				if (stripName(name).equals(stripName(itemName))) {
					bestName = itemName;
					break;
				}
				if (!isAlias) {
					if (plugin.getConfig().isSet("items." + itemName + ".aliases")) {
						for (String alias : plugin.getConfig().getStringList("items." + itemName + ".aliases")) {
							if (stripName(name).equals(stripName(alias))) {
								bestName = itemName;
								isAlias = true;
							}
						}
					}
					if (bestName == null) {
						String bukkitName = plugin.getConfig().getString("items." + itemName + ".bukkit-name");
						if (stripName(name).equals(stripName(bukkitName))) {
							bestName = itemName;
						}
					}
				}
			}
		}
		return bestName;
	}

	@SuppressWarnings("deprecation")
	public String nameFromMaterial(MaterialData material) {
		for (String itemName : plugin.getConfig().getConfigurationSection("items").getKeys(false)) {
			if (plugin.getConfig().getString("items." + itemName + ".bukkit-name", "")
					.equals(material.getItemType().toString())
					&& plugin.getConfig().getInt("items." + itemName + ".data") == material.getData()) {
				return itemName;
			}
		}
		return null;
	}

	/**
	 * Get the MaterialData object that is represented by a given name. This
	 * method first calls the getOfficialName method, so it is okay to use an
	 * unofficial name here.
	 * 
	 * @param itemName
	 *            the name of the item to find
	 * @return a MaterialData object with the Material and data specified in the
	 *         config
	 */
	@SuppressWarnings("deprecation")
	public MaterialData getMaterialData(String itemName) {
		String officialName = getOfficialName(itemName);
		if (officialName == null) {
			return null;
		}
		Material material;
		try {
			material = Material.valueOf(plugin.getConfig().getString("items." + officialName + ".bukkit-name"));
		} catch (IllegalArgumentException e) {
			plugin.getLogger().log(Level.SEVERE, "Unable to get the Material from item named: " + officialName, e);
			return null;
		}
		byte data = (byte) plugin.getConfig().getInt("items." + officialName + ".data");
		return new MaterialData(material, data);
	}

	@SuppressWarnings("deprecation")
	public MaterialData getCurrency() {
		Material currency = Material.valueOf(plugin.getConfig().getString("currency.bukkit-name"));
		byte data = (byte) plugin.getConfig().getInt("currency.data");
		return new MaterialData(currency, data);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		boolean valueCommand = command.getName().equalsIgnoreCase("value");
		boolean buyCommand = command.getName().equalsIgnoreCase("buy");
		if (!valueCommand && !(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED + "You must be a player to buy or sell");
		}
		String itemName;
		int numberOfItems = 1;
		
		if (args.length == 1) {
			try {
				numberOfItems = Integer.parseInt(args[0]);
				args = new String[0]; // if the only argument is a number, it is the number of items
			} catch (NumberFormatException e) {
				if (args[0].equalsIgnoreCase("all")) {
					if (sender instanceof Player) {
						Player player = (Player) sender;
						numberOfItems = totalAmount(player.getInventory(), player.getItemInHand());
						args = new String[0];
					}
				}
			}
		}
		
		if (args.length == 0) {
			if (sender instanceof Player) {
				Player player = (Player) sender;
				ItemStack itemInHand = player.getItemInHand();
				if (itemInHand != null) {
					if (itemInHand.getType().getMaxDurability() > 0 && itemInHand.getDurability() > 0 && (valueCommand || buyCommand)) {
						itemName = nameFromMaterial(new MaterialData(itemInHand.getType()));
					} else {
						itemName = nameFromMaterial(itemInHand.getData());
					}
					if (itemName == null) {
						if (itemInHand.getType().getMaxDurability() > 0) {
							player.sendMessage(ChatColor.RED + "Damaged items cannot be sold. Try repairing your items.");
							return true;
						}
						player.sendMessage(ChatColor.RED + "That item is not available for trade");
						return true;
					}
				} else {
					player.sendMessage(ChatColor.RED + "You are not holding an item");
					return false;
				}
			} else {
				sender.sendMessage(ChatColor.RED + "You must be a player to use that command");
				return false;
			}
		} else if (args.length == 1) {
			itemName = getOfficialName(args[0]);
			if (itemName == null) {
				sender.sendMessage(ChatColor.RED + "Invalid item name: \"" + args[0] + "\"");
				return true;
			}
		} else {
			boolean usingTimes;
			try {
				numberOfItems = Integer.parseInt(args[args.length - 1]);
				usingTimes = true;
			} catch (NumberFormatException e) {
				usingTimes = false;
			}
			int wordsInItemName = args.length;
			if (usingTimes) {
				wordsInItemName--;
			}
			StringBuilder words = new StringBuilder();
			for (int i = 0; i < wordsInItemName; i++) {
				if (i > 0) {
					words.append(' ');
				}
				words.append(args[i]);
			}
			itemName = getOfficialName(words.toString());
			if (itemName == null) {
				sender.sendMessage(ChatColor.RED + "Invalid item name: \"" + words + "\"");
				return true;
			}
		}

		if (valueCommand || !(sender instanceof Player)) {
			Transaction buy = new Transaction(plugin, itemName, true, numberOfItems);
			Transaction sell = new Transaction(plugin, itemName, false, numberOfItems);
			sender.sendMessage("You may buy " + buy.getReturns().getAmount() + ' '
					+ nameFromMaterial(buy.getReturns().getData()) + " for " + buy.getCost().getAmount() + ' '
					+ nameFromMaterial(buy.getCost().getData()) + ".");
			sender.sendMessage("You may sell " + sell.getCost().getAmount() + ' '
					+ nameFromMaterial(sell.getCost().getData()) + " for " + sell.getReturns().getAmount() + ' '
					+ nameFromMaterial(sell.getReturns().getData()) + ".");
		} else {
			Transaction transaction = new Transaction(plugin, itemName, buyCommand, numberOfItems);
			transaction.execute((Player) sender);
		}
		return true;
	}
	
	private static int totalAmount(Inventory inventory, ItemStack item) {
		int amount = 0;
		for (ItemStack i : inventory.getContents()) {
			if (item.isSimilar(i)) {
				amount += i.getAmount();
			}
		}
		return amount;
	}

	/**
	 * Strips an item name of special characters and changes it to lowercase
	 * 
	 * @param string
	 *            the item name to strip
	 * @return a stripped-down version of the item name
	 */
	private static String stripName(String string) {
		return string.replace('_', ' ').replace(':', ' ').replace(" ", "").toLowerCase();
	}
}
