package com.howlstudio.servershop;

import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

/**
 * ServerShopPlugin — Admin-run shop where players buy items with in-game currency.
 *
 * Features:
 *   - Admin defines shop items with price and quantity
 *   - Players browse with /shop list
 *   - Players buy with /shop buy <item>
 *   - Integrates with Economy plugin for balance checks
 *   - JSON-based item config
 */
public final class ServerShopPlugin extends JavaPlugin {
    private ShopManager manager;
    public ServerShopPlugin(JavaPluginInit init) { super(init); }

    @Override
    protected void setup() {
        System.out.println("[ServerShop] Loading...");
        manager = new ShopManager(getDataDirectory());
        CommandManager cmd = CommandManager.get();
        cmd.register(manager.getShopCommand());
        cmd.register(manager.getShopAdminCommand());
        System.out.println("[ServerShop] Ready. " + manager.getItemCount() + " items loaded.");
    }

    @Override
    protected void shutdown() {
        if (manager != null) manager.save();
        System.out.println("[ServerShop] Stopped.");
    }
}
