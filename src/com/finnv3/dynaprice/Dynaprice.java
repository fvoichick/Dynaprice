package com.finnv3.dynaprice;
import org.bukkit.plugin.java.JavaPlugin;

public final class Dynaprice extends JavaPlugin {

	private Shop shop;
	
	public void onEnable() {
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
