package net.bestemor.villagermarket.shop;

import net.bestemor.core.config.ConfigManager;
import net.bestemor.core.config.CurrencyBuilder;
import net.bestemor.villagermarket.VMPlugin;
import net.bestemor.villagermarket.utils.EconomyUtils;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

public class AdminShop extends VillagerShop {

    public AdminShop(VMPlugin plugin, File file) {
        super(plugin, file);
        shopfrontHolder.load();

        isLoaded = true;
    }

    /** Buys item/command from the admin shop */
    @Override
    protected void buyItem(int slot, Player player) {
        ShopItem shopItem = shopfrontHolder.getItemList().get(slot);
        Economy economy = plugin.getEconomy();

        BigDecimal price = shopItem.getSellPrice();

        if (!shopItem.verifyPurchase(player, ItemMode.SELL)) {
            return;
        }
        CurrencyBuilder message = ConfigManager.getCurrencyBuilder("messages.bought_item_as_customer")
                .replace("%amount%", String.valueOf(shopItem.getAmount()))
                .replace("%item%", shopItem.getItemName())
                .replace("%shop%", getShopName())
                .addPrefix();

        if (shopItem.isItemTrade()) {
            message.replace("%price%", shopItem.getItemTradeAmount() + "x " + shopItem.getItemTradeName());
        } else {
            message.replaceCurrency("%price%", price);
        }
        player.sendMessage(message.build());

        if (shopItem.isItemTrade()) {
            removeItems(player.getInventory(), shopItem.getItemTrade(), shopItem.getItemTradeAmount());
        } else {
            EconomyResponse ecoResponse = economy.withdrawPlayer(player, price.doubleValue());

            if (!ecoResponse.transactionSuccess()) {
                return;
            }

            // Deposit sale price to server
            if (EconomyUtils.enableServerAccount()) {
                EconomyUtils.depositServer(economy, price.doubleValue());
            }

            BigDecimal left = BigDecimal.valueOf(economy.getBalance(player));
            player.sendMessage(ConfigManager.getCurrencyBuilder("messages.money_left").replaceCurrency("%amount%", left).addPrefix().build());
            shopStats.addEarned(price.doubleValue());
        }

        shopStats.addSold(shopItem.getAmount());
        giveShopItem(player, shopItem);
        shopItem.incrementPlayerTrades(player);
        shopItem.incrementServerTrades();

        player.playSound(player.getLocation(), ConfigManager.getSound("sounds.buy_item"), 1, 1);

        VMPlugin.log.add(new Date() + ": " + player.getName() + " bought " + shopItem.getAmount() + "x " + shopItem.getType() + " from Admin Shop " + "(" + price.toPlainString() + ")");

    }

    /** Sells item to the admin shop */
    @Override
    protected void sellItem(int slot, Player player) {
        ShopItem shopItem = shopfrontHolder.getItemList().get(slot);
        Economy economy = plugin.getEconomy();

        int amount = shopItem.getAmount();
        BigDecimal price = shopItem.getBuyPrice();

        if (!shopItem.verifyPurchase(player, ItemMode.BUY)) {
            return;
        }

        player.sendMessage(ConfigManager.getCurrencyBuilder("messages.sold_item_as_customer")
                .replace("%amount%", String.valueOf(shopItem.getAmount()))
                .replaceCurrency("%price%", price)
                .replace("%item%", shopItem.getItemName())
                .replace("%shop%", getShopName()).build());

        BigDecimal tax = BigDecimal.valueOf(ConfigManager.getDouble("tax"));
        BigDecimal taxAmount = tax.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP).multiply(price);

        // Deposit taxes to server
        if (tax.doubleValue() > 0) {
            player.sendMessage(ConfigManager.getCurrencyBuilder("messages.tax").replaceCurrency("%tax%", taxAmount).addPrefix().build());

            if (EconomyUtils.enableServerAccount()) {
                EconomyUtils.depositServer(economy, taxAmount.doubleValue());
                price = price.subtract(taxAmount);
            }
        }

        economy.depositPlayer(player, price.doubleValue());
        removeItems(player.getInventory(), shopItem.getRawItem(), shopItem.getAmount());
        shopItem.incrementPlayerTrades(player);
        shopItem.incrementServerTrades();
        shopStats.addBought(amount);
        shopStats.addSpent(price.doubleValue());

        player.playSound(player.getLocation(), ConfigManager.getSound("sounds.sell_item"), 0.5f, 1);

        String currency = config.getString("currency");
        String valueCurrency = (config.getBoolean("currency_before") ? currency + price : price + currency);
        VMPlugin.log.add(new Date() + ": " + player.getName() + " sold " + amount + "x " + shopItem.getType() + " to: " + entityUUID + " (" + valueCurrency + ")");
    }

    @Override
    public String getModeCycle(String mode, boolean isItemTrade) {
        return ConfigManager.getString("menus.edit_item.mode_cycle.admin_shop." + (!isItemTrade ? mode : "item_trade"));
    }


    @Override
    public int getAvailable(ShopItem shopItem) {
        return -1;
    }

    /** Runs when a Player wants to buy a command */
    public void buyCommand(Player player, ShopItem shopItem) {
        Economy economy = plugin.getEconomy();

        BigDecimal price = shopItem.getSellPrice();
        if (economy.getBalance(player) < price.doubleValue()) {
            player.sendMessage(ConfigManager.getMessage("messages.not_enough_money"));
            return;
        }
        if (shopItem.getPlayerLimit(player) >= shopItem.getLimit() && shopItem.getLimit() != 0) {
            player.sendMessage(ConfigManager.getMessage("messages.reached_command_limit"));
            return;
        }
        economy.withdrawPlayer(player, price.doubleValue());

        if (shopItem.getCommands() != null && !shopItem.getCommands().isEmpty()) {
            ConsoleCommandSender sender = Bukkit.getConsoleSender();
            for (String command : shopItem.getCommands()) {
                Bukkit.dispatchCommand(sender, command.replaceAll("%player%", player.getName()));
            }
        }

        shopItem.incrementPlayerTrades(player);
    }
}
