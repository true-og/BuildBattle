package plugily.projects.buildbattle.handlers.misc;

import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;

import com.bergerkiller.bukkit.mw.WorldConfig;

import plugily.projects.buildbattle.Main;

/**
 * Disk-backed record of where each player stood before they entered an arena.
 * Held as world name plus coordinates rather than a Bukkit Location so an entry
 * survives a restart and keeps pointing at the right dimension even while that
 * world is unloaded.
 */
public class PreJoinLocationStore {

    private static final long DAY_MILLIS = 86400000L;
    private static final long FLUSH_INTERVAL_TICKS = 100L;

    private record Entry(String world, double x, double y, double z, float yaw, float pitch, long savedAt) {

        static Entry of(Location location) {

            return new Entry(location.getWorld().getName(), location.getX(), location.getY(), location.getZ(),
                    location.getYaw(), location.getPitch(), System.currentTimeMillis());

        }

    }

    private final Main plugin;
    private final File file;
    private final Map<UUID, Entry> entries = new ConcurrentHashMap<>();
    private volatile boolean dirty;
    private BukkitTask flushTask;

    public PreJoinLocationStore(Main plugin) {

        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "prejoin-locations.yaml");
        load();
        flushTask = Bukkit.getScheduler().runTaskTimer(plugin, this::save, FLUSH_INTERVAL_TICKS, FLUSH_INTERVAL_TICKS);

    }

    public synchronized void load() {

        entries.clear();
        if (!file.exists()) {

            plugin.getDebugger().debug("[PreJoinStore] No prejoin-locations.yaml found; starting empty.");
            return;

        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection("locations");
        if (section == null) {

            return;

        }

        long expiryDays = Math.max(0, plugin.getConfig().getInt("MyWorlds.PreJoin-Location-Expiry-Days", 30));
        long oldestAllowed = expiryDays == 0 ? 0 : System.currentTimeMillis() - (expiryDays * DAY_MILLIS);
        int expired = 0;

        for (String key : section.getKeys(false)) {

            ConfigurationSection entrySection = section.getConfigurationSection(key);
            if (entrySection == null) {

                continue;

            }

            String world = entrySection.getString("world");
            if (world == null) {

                plugin.getLogger().warning("Skipping malformed return location entry '" + key + "'.");
                continue;

            }

            UUID id;
            try {

                id = UUID.fromString(key);

            } catch (IllegalArgumentException exception) {

                plugin.getLogger().warning("Skipping return location entry with invalid UUID '" + key + "'.");
                continue;

            }

            long savedAt = entrySection.getLong("saved", System.currentTimeMillis());
            if (savedAt < oldestAllowed) {

                expired++;
                continue;

            }

            entries.put(id,
                    new Entry(world, entrySection.getDouble("x"), entrySection.getDouble("y"),
                            entrySection.getDouble("z"), (float) entrySection.getDouble("yaw"),
                            (float) entrySection.getDouble("pitch"), savedAt));

        }

        if (expired > 0) {

            dirty = true;

        }

        plugin.getDebugger().debug("[PreJoinStore] Loaded {0} stored return location(s), dropped {1} expired.",
                entries.size(), expired);

    }

    // Only writes when something actually changed, so the repeating flush is
    // free on an idle server.
    public synchronized void save() {

        if (!dirty) {

            return;

        }

        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, Entry> mapEntry : entries.entrySet()) {

            String path = "locations." + mapEntry.getKey();
            Entry entry = mapEntry.getValue();
            yaml.set(path + ".world", entry.world());
            yaml.set(path + ".x", entry.x());
            yaml.set(path + ".y", entry.y());
            yaml.set(path + ".z", entry.z());
            yaml.set(path + ".yaw", (double) entry.yaw());
            yaml.set(path + ".pitch", (double) entry.pitch());
            yaml.set(path + ".saved", entry.savedAt());

        }

        try {

            if (!plugin.getDataFolder().exists()) {

                plugin.getDataFolder().mkdirs();

            }

            yaml.save(file);
            dirty = false;

        } catch (Exception exception) {

            plugin.getLogger().warning("Failed to save return locations: " + exception.getMessage());

        }

    }

    public void put(UUID playerId, Location location) {

        if (playerId == null || location == null || location.getWorld() == null) {

            return;

        }

        entries.put(playerId, Entry.of(location));
        dirty = true;

    }

    public void remove(UUID playerId) {

        if (playerId != null && entries.remove(playerId) != null) {

            dirty = true;

        }

    }

    public String getWorldName(UUID playerId) {

        Entry entry = playerId != null ? entries.get(playerId) : null;
        return entry != null ? entry.world() : null;

    }

    // Resolves the stored spot against a loaded world. Null when the world is
    // not loaded right now.
    public Location get(UUID playerId) {

        Entry entry = playerId != null ? entries.get(playerId) : null;
        if (entry == null) {

            return null;

        }

        World world = Bukkit.getWorld(entry.world());
        if (world == null) {

            return null;

        }

        return new Location(world, entry.x(), entry.y(), entry.z(), entry.yaw(), entry.pitch());

    }

    // Same as get(), but pulls the world back in through MyWorlds when it is
    // unloaded -- a player who came from the End belongs in the End, even if
    // the End emptied out while they played.
    public Location getOrLoadWorld(UUID playerId) {

        Location resolved = get(playerId);
        if (resolved != null) {

            return resolved;

        }

        String worldName = getWorldName(playerId);
        if (worldName == null) {

            return null;

        }

        WorldConfig worldConfig = WorldConfig.getIfExists(worldName);
        if (worldConfig == null) {

            plugin.getLogger().warning("Stored return world '" + worldName + "' is unknown to MyWorlds.");
            return null;

        }

        plugin.getDebugger().debug("[PreJoinStore] Loading world {0} to return a player to their pre-join location.",
                worldName);
        if (worldConfig.loadWorld() == null) {

            return null;

        }

        return get(playerId);

    }

    public void shutdown() {

        if (flushTask != null) {

            try {

                flushTask.cancel();

            } catch (IllegalStateException ignored) {

            }

            flushTask = null;

        }

        save();

    }

}
