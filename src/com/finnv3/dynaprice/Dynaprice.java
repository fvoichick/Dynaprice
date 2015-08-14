package com.finnv3.dynaprice;
import java.io.IOException;
import java.util.logging.Level;

import org.bukkit.plugin.java.JavaPlugin;
import org.mcstats.MetricsLite;

import net.gravitydevelopment.updater.Updater;

public final class Dynaprice extends JavaPlugin {

	private Shop shop;
	
	public void onEnable() {
		if (getConfig().getBoolean("auto-update", true)) {
			new Updater(this, id, getFile(), Updater.UpdateType.DEFAULT, false);
		}
		try {
			MetricsLite metrics = new MetricsLite(this);
			metrics.start();
		} catch(IOException e) {
			getLogger().log(Level.WARNING, "Failed to submit stats to mcstats.org", e);
		}
		saveDefaultConfig();
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
	
	private static final int id = -1;
}
