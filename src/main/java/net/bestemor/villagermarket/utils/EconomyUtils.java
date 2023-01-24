package net.bestemor.villagermarket.utils;

import net.bestemor.core.config.ConfigManager;
import net.milkbowl.vault.economy.Economy;

public class EconomyUtils {

    public static boolean enableServerAccount() {
        return ConfigManager.getBoolean("deposit_deleted_money_to_server");
    }

    public static String getServerAccountName() { return ConfigManager.getString("server_account_name"); }

    public static boolean depositServer(Economy economy, double amount) {
        String serverAccountName = getServerAccountName();

        if (!validateServerAccount(economy)) {
            return false;
        }
        return economy.depositPlayer(serverAccountName, amount)
                .transactionSuccess();
    }

    public static boolean withdrawServer(Economy economy, double amount) {
        String serverAccountName = getServerAccountName();

        if (!validateServerAccount(economy)) {
            return false;
        }
        return economy.withdrawPlayer(serverAccountName, amount)
                .transactionSuccess();
    }

    private static boolean validateServerAccount(Economy economy) {
        String serverAccountName = getServerAccountName();

        if (!economy.hasAccount(serverAccountName)) {
            return economy.createPlayerAccount(serverAccountName);
        }
        return true;
    }
}
