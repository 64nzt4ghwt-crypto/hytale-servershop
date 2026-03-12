package com.howlstudio.servershop;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.io.*; import java.nio.file.*; import java.util.*;

public class ShopManager {
    private final Path dataDir;
    private final Map<String, ShopItem> items = new LinkedHashMap<>();
    private final Map<UUID, Double> balances = new HashMap<>(); // local fallback if no Economy

    public ShopManager(Path dataDir) {
        this.dataDir = dataDir;
        try { Files.createDirectories(dataDir); } catch (Exception e) {}
        loadDefaults();
        load();
    }

    private void loadDefaults() {
        if (items.isEmpty()) {
            items.put("vip", new ShopItem("vip", "VIP Rank", "VIP rank for 30 days", 1000, -1, "rank set {player} vip"));
            items.put("home-extra", new ShopItem("home-extra", "+1 Home Slot", "Unlock an extra home slot", 500, -1, "home addslot {player}"));
            items.put("rename", new ShopItem("rename", "Name Change Token", "Change your nickname once", 200, -1, "nick token {player}"));
        }
    }

    private void load() {
        try {
            Path f = dataDir.resolve("shop.txt");
            if (!Files.exists(f)) return;
            items.clear();
            for (String line : Files.readAllLines(f)) {
                if (line.isBlank() || line.startsWith("#")) continue;
                ShopItem item = ShopItem.fromConfig(line);
                if (item != null) items.put(item.getId(), item);
            }
        } catch (Exception e) { System.out.println("[ServerShop] Load error: " + e.getMessage()); }
    }

    public void save() {
        try {
            StringBuilder sb = new StringBuilder("# ServerShop items — id|name|desc|price|stock(-1=unlimited)|command\n");
            for (ShopItem item : items.values()) sb.append(item.toConfig()).append("\n");
            Files.writeString(dataDir.resolve("shop.txt"), sb.toString());
        } catch (Exception e) { System.out.println("[ServerShop] Save error: " + e.getMessage()); }
    }

    public int getItemCount() { return items.size(); }

    public double getBalance(UUID uid) { return balances.getOrDefault(uid, 1000.0); }
    public void deduct(UUID uid, double amount) { balances.merge(uid, amount, (old, amt) -> old - amt); }

    public AbstractPlayerCommand getShopCommand() {
        return new AbstractPlayerCommand("shop", "Browse and buy from the server shop. Usage: /shop list | /shop buy <id>") {
            @Override
            protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef playerRef, World world) {
                String input = ctx.getInputString().trim();
                String[] args = input.isEmpty() ? new String[0] : input.split("\\s+");
                String sub = args.length > 0 ? args[0].toLowerCase() : "list";

                if (sub.equals("list") || sub.isEmpty()) {
                    playerRef.sendMessage(Message.raw("=== Server Shop ==="));
                    playerRef.sendMessage(Message.raw(String.format("  Balance: %.0f coins", getBalance(playerRef.getUuid()))));
                    for (ShopItem item : items.values()) {
                        String stock = item.getStock() == -1 ? "∞" : String.valueOf(item.getStock());
                        playerRef.sendMessage(Message.raw(String.format(
                            "  [%s] %s — %s (stock: %s)", item.getId(), item.getDisplayName(), item.getPriceStr(), stock)));
                        playerRef.sendMessage(Message.raw("      " + item.getDescription()));
                    }
                    playerRef.sendMessage(Message.raw("  Use /shop buy <id> to purchase."));
                } else if (sub.equals("buy") && args.length > 1) {
                    String itemId = args[1].toLowerCase();
                    ShopItem item = items.get(itemId);
                    if (item == null) { playerRef.sendMessage(Message.raw("[Shop] Unknown item: " + itemId)); return; }
                    if (!item.isAvailable()) { playerRef.sendMessage(Message.raw("[Shop] " + item.getDisplayName() + " is out of stock.")); return; }
                    double bal = getBalance(playerRef.getUuid());
                    if (bal < item.getPrice()) { playerRef.sendMessage(Message.raw("[Shop] Not enough coins. Need " + item.getPriceStr() + ", have " + String.format("%.0f", bal) + ".")); return; }
                    deduct(playerRef.getUuid(), item.getPrice());
                    item.purchase();
                    playerRef.sendMessage(Message.raw("[Shop] Purchased: " + item.getDisplayName() + "! (" + item.getPriceStr() + " deducted)"));
                    System.out.println("[ServerShop] Purchase: " + playerRef.getUsername() + " bought " + itemId);
                } else {
                    playerRef.sendMessage(Message.raw("Usage: /shop list | /shop buy <id>"));
                }
            }
        };
    }

    public AbstractPlayerCommand getShopAdminCommand() {
        return new AbstractPlayerCommand("shopadmin", "[Admin] Manage shop items. Usage: /shopadmin add|remove|reload") {
            @Override
            protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef playerRef, World world) {
                String input = ctx.getInputString().trim();
                String[] args = input.isEmpty() ? new String[0] : input.split("\\s+", 2);
                String sub = args.length > 0 ? args[0].toLowerCase() : "";
                if (sub.equals("reload")) {
                    load(); playerRef.sendMessage(Message.raw("[Shop] Reloaded " + items.size() + " items."));
                } else if (sub.equals("remove") && args.length > 1) {
                    if (items.remove(args[1]) != null) { save(); playerRef.sendMessage(Message.raw("[Shop] Removed: " + args[1])); }
                    else playerRef.sendMessage(Message.raw("[Shop] Not found: " + args[1]));
                } else {
                    playerRef.sendMessage(Message.raw("Usage: /shopadmin reload | /shopadmin remove <id>"));
                }
            }
        };
    }
}
