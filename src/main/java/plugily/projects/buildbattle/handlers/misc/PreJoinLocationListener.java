package plugily.projects.buildbattle.handlers.misc;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import plugily.projects.buildbattle.Main;

// Players reach a BuildBattle world by more than /bbjoin -- /mw tp, portals and
// other plugins all land here. Without a recorded spot those players get dumped
// at main world spawn instead of where they came from.
public class PreJoinLocationListener implements Listener {

    private final Main plugin;

    public PreJoinLocationListener(Main plugin) {

        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {

        Location from = event.getFrom();
        Location to = event.getTo();
        if (from == null || to == null || from.getWorld() == null || to.getWorld() == null) {

            return;

        }

        if (from.getWorld().equals(to.getWorld())) {

            return;

        }

        boolean fromArena = isArenaWorld(from.getWorld());
        boolean toArena = isArenaWorld(to.getWorld());

        if (toArena && !fromArena) {

            // ArenaManager.joinAttempt records its own spot before teleporting, so
            // only fill the gap when nothing is stored yet.
            if (!plugin.hasPreJoinLocation(event.getPlayer().getUniqueId())) {

                plugin.savePreJoinLocation(event.getPlayer().getUniqueId(), from);

            }

            return;

        }

        // Left an arena world for a real one by any route, so the recorded spot has
        // served its purpose. Dropping it stops a stale spot yanking them later.
        if (fromArena && !toArena) {

            plugin.removePreJoinLocation(event.getPlayer().getUniqueId());

        }

    }

    // A player who dies in an arena and respawns after the arena released them
    // would otherwise respawn at main world spawn.
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {

        Player player = event.getPlayer();
        if (plugin.getArenaRegistry().getArena(player) != null) {

            return;

        }

        if (!plugin.hasPreJoinLocation(player.getUniqueId())) {

            return;

        }

        Location destination = plugin.getAndRemovePreJoinLocation(player.getUniqueId());
        if (destination != null && destination.getWorld() != null) {

            event.setRespawnLocation(destination);

        }

    }

    private boolean isArenaWorld(World world) {

        if (world == null) {

            return false;

        }

        if (plugin.getMyWorldsManager().isConfiguredArenaWorld(world.getName())) {

            return true;

        }

        for (World arenaWorld : plugin.getArenaRegistry().getArenaWorlds()) {

            if (arenaWorld != null && arenaWorld.getName().equals(world.getName())) {

                return true;

            }

        }

        return false;

    }

}
