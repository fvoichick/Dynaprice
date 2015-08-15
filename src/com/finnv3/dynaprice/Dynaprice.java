package com.finnv3.dynaprice;

import java.io.IOException;
import java.util.logging.Level;

import org.apache.commons.lang3.text.WordUtils;
import org.bukkit.Material;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import org.mcstats.MetricsLite;

import net.gravitydevelopment.updater.Updater;

public final class Dynaprice extends JavaPlugin {

	private Shop shop;

	public void onEnable() {
		saveDefaultConfig();
		if (!validateConfig()) {
			getLogger().info("Disabling Dynaprice v" + getDescription().getVersion());
			setEnabled(false);
			return;
		}
		if (getConfig().getBoolean("auto-update", true)) {
			new Updater(this, id, getFile(), Updater.UpdateType.DEFAULT, false);
		}
		try {
			MetricsLite metrics = new MetricsLite(this);
			metrics.start();
		} catch (IOException e) {
			getLogger().log(Level.WARNING, "Failed to submit stats to mcstats.org", e);
		}
		shop = new Shop(this);
		getCommand("buy").setExecutor(shop);
		getCommand("sell").setExecutor(shop);
		getCommand("value").setExecutor(shop);
	}

	public Shop getShop() {
		return shop;
	}

	public void onDisable() {
		saveConfig();
	}

	private boolean validateConfig() {
		Configuration config = getConfig();
		if (!config.isSet("auto-update") || !config.isBoolean("auto-update")) {
			config.set("auto-update", true);
		}

		if (!config.isSet("buy-sell-difference") || !config.isInt("buy-sell-difference")) {
			config.set("buy-sell-difference", 1);
		}
		if (!config.isSet("price-change") || !config.isInt("price-change")) {
			config.set("price-change", 1);
		}
		int buySellDifference = config.getInt("buy-sell-difference");
		int priceChange = config.getInt("price-change");
		if (priceChange > buySellDifference) {
			getLogger().warning(
					"Your price-change value (" + priceChange + ") is greater than your buy-sell-difference value ("
							+ buySellDifference + "), increasing buy-sell-difference to " + priceChange);
			config.set("buy-sell-difference", priceChange);
		}

		if (!config.isSet("currency.bukkit-name") || !config.isString("currency.bukkit-name")) {
			config.set("currency.bukkit-name", "EMERALD");
		}
		if (Material.valueOf(config.getString("currency.bukkit-name")) == null) {
			getLogger().severe("Invalid currency.bukkit-name in config");
			return false;
		}
		if (!config.isSet("currency.name") || !config.isString("currency.name")) {
			config.set("currency.name", WordUtils.capitalizeFully(config.getString("currency.bukkit-name")));
		}

		ConfigurationSection itemsConfig = config.getConfigurationSection("items");
		if (itemsConfig == null || itemsConfig.getKeys(false).isEmpty()) {
			getLogger().info("No items are in the config, adding defaults");
			config.set("items", config.getDefaults().get("items"));
		} else {
			for (String itemName : itemsConfig.getKeys(false)) {
				ConfigurationSection itemSection = itemsConfig.getConfigurationSection(itemName);
				if (!itemSection.isSet("bukkit-name") || !itemSection.isString("bukkit-name")
						|| Material.valueOf(itemSection.getString("bukkit-name")) == null) {
					getLogger().severe(
							"Invalid " + itemName + " bukkit-name in config: " + itemSection.get("bukkit-name", null));
					return false;
				} else {
					if (!itemSection.isSet("value") || !itemSection.isInt("value")) {
						int defValue = config.getDefaults().getInt("items." + itemName + ".value", 1);
						getLogger().warning("No value set for " + itemName + ", defaulting to " + defValue);
						itemSection.set("value", defValue);
					}
				}
			}
		}
		saveConfig();
		return true;
	}

	private static final int id = 94275;
}
