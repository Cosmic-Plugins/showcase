package me.randomhashtags.showcase;

import me.randomhashtags.showcase.universal.UVersionable;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

public final class SPlayer implements UVersionable {
    private static final String PLAYER_DATA_FOLDER = SHOWCASE.getDataFolder() + File.separator + "_Data" + File.separator + "playerData";
    public static final HashMap<UUID, SPlayer> CACHED_PLAYERS = new HashMap<>();

    private boolean isLoaded;
    private UUID uuid;
    private File file;
    private YamlConfiguration yml;
    private HashMap<Integer, Integer> showcaseSizes;
    private HashMap<Integer, ItemStack[]> showcases;

    public SPlayer(UUID uuid) {
        this.uuid = uuid;
        final File f = new File(PLAYER_DATA_FOLDER, uuid.toString() + ".yml");
        boolean backup = false;
        if(!CACHED_PLAYERS.containsKey(uuid)) {
            if(!f.exists()) {
                try {
                    final File folder = new File(SPlayer.PLAYER_DATA_FOLDER);
                    if(!folder.exists()) {
                        folder.mkdirs();
                    }
                    f.createNewFile();
                    backup = true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            file = new File(PLAYER_DATA_FOLDER, uuid.toString() + ".yml");
            yml = YamlConfiguration.loadConfiguration(file);
            CACHED_PLAYERS.put(uuid, this);
        }
        if(backup) {
            backup();
        }
    }

    public static SPlayer get(UUID player) {
        return CACHED_PLAYERS.getOrDefault(player, new SPlayer(player));
    }

    public void unload(boolean async) {
        if(async) {
            SCHEDULER.runTaskAsynchronously(SHOWCASE, () -> unload());
        } else {
            unload();
        }
    }
    public void unload() {
        if(isLoaded) {
            try {
                backup();
            } catch (Exception e) {
                e.printStackTrace();
            }
            isLoaded = false;
            CACHED_PLAYERS.remove(uuid);
        }
    }
    private void save() {
        try {
            yml.save(file);
            yml = YamlConfiguration.loadConfiguration(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public UUID getUUID() {
        return uuid;
    }
    public OfflinePlayer getOfflinePlayer() {
        return uuid != null ? Bukkit.getOfflinePlayer(uuid) : null;
    }

    public void backup() {
        if(showcases != null) {
            for(int p : showcases.keySet()) {
                yml.set("showcases." + p, null);
                yml.set("showcases." + p + ".size", getShowcaseSize(p));
                int s = 0;
                for(ItemStack i : showcases.get(p)) {
                    if(i != null && !i.getType().equals(Material.AIR)) {
                        yml.set("showcases." + p + "." + s, i.toString());
                    }
                    s++;
                }
            }
        }
        save();
    }

    public HashMap<Integer, ItemStack[]> getShowcases() {
        if(showcases == null && showcaseSizes == null) {
            showcases = new HashMap<>();
            showcaseSizes = new HashMap<>();
            if(yml.getConfigurationSection("showcases") != null) {
                for(String s : getConfigurationSectionKeys(yml, "showcases", false)) {
                    final int page = Integer.parseInt(s);
                    showcaseSizes.put(page, yml.getInt("showcases." + s + ".size"));
                    final ConfigurationSection i = yml.getConfigurationSection("showcases." + s);
                    final ItemStack[] items = new ItemStack[54];
                    if(i != null) {
                        for(String sl : i.getKeys(false)) {
                            final int slot = Integer.parseInt(sl);
                            ItemStack is;
                            try {
                                is = yml.getItemStack("showcases." + s + "." + sl);
                            } catch (Exception e) {
                                is = createItemStack(yml, "showcases." + s + "." + sl);
                            }
                            items[slot] = is;
                        }
                    }
                    showcases.put(page, items);
                }
            } else {
                final int defaultShowcase = SHOWCASE_CONFIG.getInt("settings.default showcases"), defaultSize = SHOWCASE_CONFIG.getInt("settings.default showcase size");
                if(defaultShowcase > 0) {
                    final ItemStack[] a = new ItemStack[54];
                    for(int i = 1; i <= defaultShowcase; i++) {
                        showcases.put(i, a);
                        showcaseSizes.put(i, defaultSize);
                    }
                }
            }
        }
        return showcases;
    }
    public HashMap<Integer, Integer> getShowcaseSizes() {
        getShowcases();
        return showcaseSizes;
    }
    public ItemStack[] getShowcase(int page) {
        return getShowcases().getOrDefault(page, null);
    }
    public int getShowcaseSize(int page) {
        return getShowcaseSizes().getOrDefault(page, 0);
    }
    public HashMap<Integer, ItemStack> getShowcaseItems(int page) {
        final HashMap<Integer, ItemStack> items = new HashMap<>();
        if(getShowcases().containsKey(page)) {
            int p = 0;
            for(ItemStack a : showcases.get(page)) {
                if(a != null && !a.getType().equals(Material.AIR)) {
                    items.put(p, a);
                }
                p++;
            }
        }
        return items;
    }
    public void addToShowcase(int page, ItemStack item) {
        if(getShowcases().containsKey(page)) {
            int p = 0;
            for(ItemStack is : showcases.get(page)) {
                if(is == null || is.getType().equals(Material.AIR)) {
                    showcases.get(page)[p] = item;
                    return;
                }
                p++;
            }
        }
    }
    public void removeFromShowcase(int page, ItemStack item) {
        if(getShowcases().containsKey(page)) {
            int p = 0;
            for(ItemStack is : showcases.get(page)) {
                if(is != null && is.equals(item)) {
                    showcases.get(page)[p] = null;
                    return;
                }
                p++;
            }
        }
    }
    public void resetShowcases() {
        showcases = new HashMap<>();
        showcaseSizes = new HashMap<>();
    }
    public void resetShowcase(int page) {
        showcases.put(page, new ItemStack[54]);
        showcaseSizes.put(page, 9);
    }

}
