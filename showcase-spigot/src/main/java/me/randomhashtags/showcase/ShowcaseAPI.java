package me.randomhashtags.showcase;

import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import me.randomhashtags.showcase.universal.UInventory;
import me.randomhashtags.showcase.universal.UVersionable;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public enum ShowcaseAPI implements Listener, CommandExecutor, UVersionable {
    INSTANCE;

    private boolean isEnabled;
    private ItemStack item;
    private ItemMeta itemMeta;
    private List<String> lore;
    
    private ItemStack addItemConfirm, addItemCancel, removeItemConfirm, removeItemCancel, expansion;
    private int addedRows = 0;
    private UInventory additems, removeitems;
    private String othertitle, selftitle, TCOLOR;
    private List<Integer> itemslots;
    private List<Player> inSelf, inOther;
    private HashMap<Player, Integer> deleteSlot;

    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        final Player player = sender instanceof Player ? (Player) sender : null;
        final int l = args.length;
        if(player != null) {
            if(l == 0) {
                open(player, player, 1);
            } else {
                final String a = args[0];
                if(a.startsWith("add")) {
                    confirmAddition(player, player.getItemInHand());
                } else if(player.hasPermission("Showcase.other")) {
                    final OfflinePlayer target = Bukkit.getOfflinePlayer(a);
                    open(player, target, 1);
                }
            }
        }
        if(l >= 2 && sender.hasPermission("Showcase.reset")) {
            final String a = args[0];
            if(a.equals("reset")) {
                resetShowcases(Bukkit.getOfflinePlayer(args[1]));
            }
        }
        return true;
    }

    public void load() {
        if(isEnabled) {
            return;
        }
        isEnabled = true;
        PLUGIN_MANAGER.registerEvents(this, SHOWCASE);
        final long started = System.currentTimeMillis();

        item = new ItemStack(Material.APPLE);
        itemMeta = item.getItemMeta();
        lore = new ArrayList<>();

        expansion = createItemStack(SHOWCASE_CONFIG, "items.expansion");
        addedRows = SHOWCASE_CONFIG.getInt("items.expansion.added rows");

        itemslots = new ArrayList<>();
        inSelf = new ArrayList<>();
        inOther = new ArrayList<>();
        deleteSlot = new HashMap<>();

        additems = new UInventory(null, SHOWCASE_CONFIG.getInt("add item.size"), colorize(SHOWCASE_CONFIG.getString("add item.title")));
        removeitems = new UInventory(null, SHOWCASE_CONFIG.getInt("remove item.size"), colorize(SHOWCASE_CONFIG.getString("remove item.title")));

        othertitle = colorize(SHOWCASE_CONFIG.getString("settings.other title"));
        selftitle = colorize(SHOWCASE_CONFIG.getString("settings.self title"));
        TCOLOR = SHOWCASE_CONFIG.getString("settings.time color");

        addItemConfirm = createItemStack(SHOWCASE_CONFIG, "add item.confirm");
        addItemCancel = createItemStack(SHOWCASE_CONFIG, "add item.cancel");
        removeItemConfirm = createItemStack(SHOWCASE_CONFIG, "remove item.confirm");
        removeItemCancel = createItemStack(SHOWCASE_CONFIG, "remove item.cancel");

        final Inventory ai = additems.getInventory(), ri = removeitems.getInventory();
        for(int i = 1; i <= 2; i++) {
            for(int o = 0; o < (i == 1 ? additems.getSize() : removeitems.getSize()); o++) {
                String s = SHOWCASE_CONFIG.getString((i == 1 ? "add item." : "remove item.") + o + ".item");
                if(s != null) {
                    switch (s) {
                        case "{CONFIRM}":
                            if(i == 1) ai.setItem(o, addItemConfirm.clone());
                            else       ri.setItem(o, removeItemConfirm.clone());
                            break;
                        case "{CANCEL}":
                            (i == 1 ? ai : ri).setItem(o, i == 1 ? addItemCancel : removeItemCancel);
                            break;
                        case "{ITEM}":
                            itemslots.add(o);
                            break;
                        default:
                            break;
                    }
                }
            }
        }
        sendConsoleDidLoadFeature("ShowcaseAPI", started);
    }
    public void unload() {
        if(isEnabled) {
            isEnabled = false;
            HandlerList.unregisterAll(this);
            final String msg = colorize("&e&l(!)&r &eYou've been forced to exit a showcase due to reloading the server.");
            for(Player player : new ArrayList<>(inSelf)) {
                player.sendMessage(msg);
                player.closeInventory();
            }
            for(Player player : new ArrayList<>(inOther)) {
                player.sendMessage(msg);
                player.closeInventory();
            }
        }
    }

    public void resetShowcases(OfflinePlayer player) {
        if(player != null) {
            final SPlayer pdata = SPlayer.get(player.getUniqueId());
            pdata.resetShowcases();
        }
    }
    public void confirmAddition(@NotNull Player player, @NotNull ItemStack item) {
        if(player.hasPermission("Showcase.add")) {
            confirm(player, item, additems);
        }
    }
    public void confirmDeletion(@NotNull Player player, @NotNull ItemStack item) {
        if(player.hasPermission("Showcase.remove")) {
            confirm(player, item, removeitems);
        }
    }
    private void confirm(Player player, ItemStack item, UInventory type) {
        if(item != null && !item.getType().equals(Material.AIR)) {
            player.openInventory(Bukkit.createInventory(player, type.getSize(), type.getTitle()));
            final Inventory top = player.getOpenInventory().getTopInventory();
            top.setContents(type.getInventory().getContents());
            for(int i : itemslots) {
                top.setItem(i, item);
            }
        }
        player.updateInventory();
    }
    private void add(UUID player, ItemStack item, int page) {
        if(item == null || item.getType().equals(Material.AIR)) {
        } else {
            final OfflinePlayer op = Bukkit.getOfflinePlayer(player);
            if(op.isOnline()) {
                removeItem(op.getPlayer(), item, item.getAmount());
            }
            final String format = toReadableDate(new Date(), "MMMM dd, yyyy");
            itemMeta = item.getItemMeta(); lore.clear();
            if(itemMeta.hasLore()) lore.addAll(itemMeta.getLore());
            lore.add(colorize(TCOLOR + format));
            itemMeta.setLore(lore); lore.clear();
            item.setItemMeta(itemMeta);

            final SPlayer pdata = SPlayer.get(player);
            pdata.addToShowcase(page, item);
        }
    }
    private void delete(UUID player, int page, ItemStack is) {
        final SPlayer pdata = SPlayer.get(player);
        pdata.removeFromShowcase(page, is);
    }
    public void open(@NotNull Player opener, @Nullable OfflinePlayer target, int page) {
        if(target == null || target == opener) target = opener;
        final boolean self = target == opener;
        final SPlayer pdata = SPlayer.get(target.getUniqueId());
        final HashMap<Integer, ItemStack[]> showcases = pdata.getShowcases();
        int maxpage = 0;
        for(int i = 1; i <= 100; i++) {
            if(showcases.containsKey(i)) {
                maxpage = i;
            }
        }
        if(!opener.hasPermission("Showcase" + (self ? "" : ".other"))) {
            sendStringListMessage(opener, getStringList(SHOWCASE_CONFIG, "messages.no access"), null);
        } else {
            int size = pdata.getShowcaseSize(page);
            size = size == 0 ? 9 : size;
            (self ? inSelf : inOther).add(opener);
            final Inventory inv = Bukkit.createInventory(opener, size, (self ? selftitle : othertitle).replace("{PLAYER}", (self ? opener : target).getName()).replace("{PAGE}", Integer.toString(page)).replace("{MAX}", Integer.toString(maxpage)));
            opener.openInventory(inv);
            final Inventory top = opener.getOpenInventory().getTopInventory();
            final HashMap<Integer, ItemStack> showcase = pdata.getShowcaseItems(page);
            for(int i : showcase.keySet()) {
                if(i < size) {
                    top.setItem(i, showcase.get(i));
                }
            }
            opener.updateInventory();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void inventoryClickEvent(InventoryClickEvent event) {
        final ItemStack current = event.getCurrentItem();
        if(current != null && !current.getType().equals(Material.AIR)) {
            final Player player = (Player) event.getWhoClicked();
            final Inventory top = player.getOpenInventory().getTopInventory();
            final String title = event.getView().getTitle();
            final boolean isAdding = title.equals(additems.getTitle()), edit = isAdding || title.equals(removeitems.getTitle());

            if(edit) {
                final ItemStack item = top.getItem(itemslots.get(0));
                if(isAdding && current.equals(addItemConfirm)) {
                    add(player.getUniqueId(), item, 1);
                } else if(current.equals(removeItemConfirm)) {
                    delete(player.getUniqueId(), 1, top.getItem(deleteSlot.get(player)));
                    deleteSlot.remove(player);
                }
            } else if(inSelf.contains(player)) {
                if(event.getRawSlot() >= top.getSize()) {
                    confirmAddition(player, current);
                } else {
                    confirmDeletion(player, current);
                    deleteSlot.put(player, itemslots.get(1));
                }
            } else {
                return;
            }

            event.setCancelled(true);
            player.updateInventory();
            if(edit && (current.equals(addItemConfirm) || current.equals(addItemCancel) || current.equals(removeItemConfirm) || current.equals(removeItemCancel))) {
                open(player, player, 1);
                inSelf.add(player);
            }
        }
    }
    @EventHandler
    private void inventoryCloseEvent(InventoryCloseEvent event) {
        final Player player = (Player) event.getPlayer();
        inSelf.remove(player);
        inOther.remove(player);
        deleteSlot.remove(player);
    }
    @EventHandler
    private void playerInteractEvent(PlayerInteractEvent event) {
        final ItemStack i = event.getItem();
        if(i != null && i.isSimilar(expansion)) {
            final Player player = event.getPlayer();
            final SPlayer pdata = SPlayer.get(player.getUniqueId());
            final HashMap<Integer, Integer> sizes = pdata.getShowcaseSizes();
            event.setCancelled(true);
            player.updateInventory();
            for(int o = 1; o <= 10; o++) {
                if(sizes.containsKey(o)) {
                    final int size = sizes.get(o);
                    if(size != 54) {
                        sizes.put(o, size + (addedRows * 9));
                        removeItem(player, i, 1);
                        return;
                    }
                }
            }
        }
    }
    @EventHandler
    private void playerJoinEvent(PlayerJoinEvent event) {
        SPlayer.get(event.getPlayer().getUniqueId());
    }
    @EventHandler
    private void playerQuitEvent(PlayerQuitEvent event) {
        SPlayer.get(event.getPlayer().getUniqueId()).unload();
    }
}
