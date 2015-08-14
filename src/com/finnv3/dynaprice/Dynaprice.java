package com.finnv3.dynaprice;
import java.io.IOException;
import java.util.logging.Level;

import org.bukkit.plugin.java.JavaPlugin;
import org.mcstats.MetricsLite;

public final class Dynaprice extends JavaPlugin {

	private Shop shop;
	
	public void onEnable() {
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
	
}
