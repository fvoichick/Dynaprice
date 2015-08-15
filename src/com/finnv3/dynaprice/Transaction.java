package com.finnv3.dynaprice;

import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;

public final class Transaction {

	private final Dynaprice plugin;
	private final String itemName;
	private final boolean buy;
	private int times;

	/**
	 * Constructs a Transaction instance.
	 * 
	 * @param plugin
	 *            the DynamicPricesPlugin instance running
	 * @param itemName
	 *            the name of the item in this transaction. Unofficial names
	 *            allowed.
	 * @param buy
	 *            true if buying, false if selling
	 */
	public Transaction(Dynaprice plugin, String itemName, boolean buy, int numberOfItems) {
		this.plugin = plugin;
		this.itemName = plugin.getShop().getOfficialName(itemName);
		if (this.itemName == null) {
			throw new IllegalArgumentException("Unrecognized item name " + itemName);
		}
		this.buy = buy;

		times = 1;
		while (true) {
			if (buy) {
				int items = getReturns().getAmount();
				if (items >= numberOfItems) {
					break;
				}
			} else {
				int items = getCost().getAmount();
				if (items > numberOfItems) {
					if (times > 1) {
						times--;
					}
					break;
				}
			}
			times++;
		}
	}

	/**
	 * Gets the cost of a transaction, in the form of an ItemStack. If it is a
	 * buy transaction, this will be the amount of currency required. If it is a
	 * sell transaction, this will be the items sold.
	 * 
	 * @return the items a player must pay for this transaction
	 */
	@SuppressWarnings("deprecation")
	public ItemStack getCost() {
		if (buy) {
			MaterialData currency = plugin.getShop().getCurrency();
			int value = value();
			int amount = 0;
			for (int i = 0; i < times; i++) {
				if (value > 0) {
					amount += value;
				} else {
					amount++;
				}
				value = changeValue(value);
			}
			return new ItemStack(currency.getItemType(), amount, currency.getData());
		} else {
			MaterialData sellItem = plugin.getShop().getMaterialData(itemName);
			int value = value();
			int amount = 0;
			for (int i = 0; i < times; i++) {
				if (value > 0) {
					amount++;
				} else {
					amount -= value;
				}
				value = changeValue(value);
			}
			return new ItemStack(sellItem.getItemType(), amount, sellItem.getData());
		}
	}

	/**
	 * Gets the returns from the transaction, in the form of an ItemStack. If it
	 * is a buy transaction, this will be the amount of items bought. If it is a
	 * sell transaction, this will be the amount of currency received.
	 * 
	 * @return the items a player receives from this transaction
	 */
	@SuppressWarnings("deprecation")
	public ItemStack getReturns() {
		if (buy) {
			MaterialData buyItem = plugin.getShop().getMaterialData(itemName);
			int value = value();
			int amount = 0;
			for (int i = 0; i < times; i++) {
				if (value > 0) {
					amount++;
				} else {
					amount -= value;
				}
				value = changeValue(value);
			}
			return new ItemStack(buyItem.getItemType(), amount, buyItem.getData());
		} else {
			MaterialData currency = plugin.getShop().getCurrency();
			int value = value();
			int amount = 0;
			for (int i = 0; i < times; i++) {
				if (value > 0) {
					amount += value;
				} else {
					amount++;
				}
				value = changeValue(value);
			}
			return new ItemStack(currency.getItemType(), amount, currency.getData());
		}
	}

	private static ItemStack[] divideStack(ItemStack items) {
		int maxSize = items.getType().getMaxStackSize();
		ItemStack[] result = new ItemStack[(int) Math.ceil((double) items.getAmount() / maxSize)];
		int toStack = items.getAmount();
		for (int i = 0; i < result.length; i++) {
			System.out.println(toStack);
			result[i] = new ItemStack(items.getType(), Math.min(toStack, maxSize), items.getDurability());
			toStack -= maxSize;
		}
		return result;
	}

	/**
	 * Gets the value of the item involved. If this is a sell transaction, the
	 * value will be lower than if this is a buy transaction.
	 * 
	 * @return the value of the item
	 */
	private int value() {
		int value = plugin.getConfig().getInt("items." + itemName + ".value", 1);
		if (!buy && !plugin.getConfig().getBoolean("items." + itemName + ".static")) {
			if (value > 0) {
				value -= plugin.getConfig().getInt("buy-sell-difference", 1);
				if (value <= 0) {
					value -= 2;
				}
			} else {
				value -= plugin.getConfig().getInt("buy-sell-difference", 1);
			}
		}
		return value;
	}

	private int changeValue(int oldValue) {
		int priceChange = plugin.getConfig().getInt("price-change");
		if (buy) {
			if (oldValue >= 1) {
				return oldValue + priceChange;
			}
			oldValue += priceChange;
			if (oldValue > -2) {
				return oldValue + 2;
			} else {
				return oldValue;
			}
		} else {
			if (oldValue <= -2) {
				return oldValue - priceChange;
			}
			oldValue -= priceChange;
			if (oldValue < 1) {
				return oldValue - 2;
			} else {
				return oldValue;
			}
		}
	}

	public void execute(Player player) {
		ItemStack cost = getCost();
		if (player.getInventory().containsAtLeast(cost, cost.getAmount())) {
			ItemStack returns = getReturns();
			String message;

			if (!plugin.getConfig().getBoolean("items." + itemName + ".static")) {
				int newValue = plugin.getConfig().getInt("items." + itemName + ".value", 1);
				for (int i = 0; i < times; i++) {
					newValue = changeValue(newValue);
				}
				plugin.getConfig().set("items." + itemName + ".value", newValue);
				plugin.saveConfig();
			}

			if (buy) {
				message = "You bought " + returns.getAmount() + ' '
						+ plugin.getShop().nameFromMaterial(returns.getData()) + " for " + cost.getAmount() + ' '
						+ plugin.getShop().nameFromMaterial(cost.getData());
			} else {
				message = "You sold " + cost.getAmount() + ' ' + plugin.getShop().nameFromMaterial(cost.getData())
						+ " for " + returns.getAmount() + ' ' + plugin.getShop().nameFromMaterial(returns.getData());
			}
			player.sendMessage(message);
			player.getInventory().removeItem(cost);
			give(player, divideStack(returns));
		} else {
			if (cost.getType().getMaxDurability() > 0 && cost.getDurability() > 0) {
				player.sendMessage("Damaged items cannot be sold. Try repairing your items.");
			}
			player.sendMessage(ChatColor.RED + "This transaction requires " + cost.getAmount() + " "
					+ plugin.getShop().nameFromMaterial(cost.getData()));
		}
	}

	private static void give(Player player, ItemStack[] items) {
		Map<Integer, ItemStack> leftItems = player.getInventory().addItem(items);
		for (ItemStack leftItem : leftItems.values()) {
			player.getWorld().dropItemNaturally(player.getLocation(), leftItem);
		}
	}

}
