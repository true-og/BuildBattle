package plugily.projects.buildbattle.handlers.misc;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.PermissionAttachment;

import plugily.projects.buildbattle.Main;

/**
 * Keeps BuildBattle compatible with GameModeInventories-OG while making the
 * arena worlds the only place on the server where a regular player may hold
 * creative mode -- and only as the builder.
 *
 * Two tiers of runtime permission attachment:
 *
 * Every arena participant gets gamemodeinventories.use negated for the length
 * of their stay. MiniGamesBox owns inventory save/restore inside arenas, and a
 * GameModeInventories swap on the survival/adventure/creative flips the game
 * performs would clobber armor and offhand mid-restore (they are applied before
 * the gamemode is set) and pollute the player's stored survival inventory with
 * arena state.
 *
 * Builders additionally get gamemodeinventories.anywhere (sanctions creative
 * and stops the forced-survival watchdogs) and gamemodeinventories.bypass
 * (lifts creative item restrictions inside the plots with GMI's default bypass
 * flags). These are stripped the moment the builder leaves creative by any
 * route: round rotation, arena leave, game end, world change, quit.
 */
public class BuilderCreativeManager implements Listener {

    private static final String GMI_ANYWHERE_PERMISSION = "gamemodeinventories.anywhere";
    private static final String GMI_USE_PERMISSION = "gamemodeinventories.use";
    private static final String GMI_BYPASS_PERMISSION = "gamemodeinventories.bypass";

    private final Main plugin;
    private final Map<UUID, PermissionAttachment> exemptions = new ConcurrentHashMap<>();
    private final Set<UUID> builders = ConcurrentHashMap.newKeySet();

    public BuilderCreativeManager(Main plugin) {

        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

    }

    /**
     * Suspends GameModeInventories inventory swapping for an arena participant.
     * Called when the player joins an arena; lasts until they leave it.
     */
    public void enterArena(Player player) {

        ensureAttachment(player);

    }

    /**
     * Puts an arena builder into creative mode. Refused when the player is not
     * inside an arena world, so BuildBattle can never become a creative exemption
     * anywhere else on the server.
     */
    public void grantBuilderCreative(Player player) {

        if (!isArenaWorld(player.getWorld())) {

            plugin.getDebugger().debug("[BuilderCreative] Refusing creative for {0}: world {1} is not an arena world.",
                    player.getName(), player.getWorld().getName());
            return;

        }

        PermissionAttachment attachment = ensureAttachment(player);
        attachment.setPermission(GMI_ANYWHERE_PERMISSION, true);
        attachment.setPermission(GMI_BYPASS_PERMISSION, true);
        builders.add(player.getUniqueId());

        player.setGameMode(GameMode.CREATIVE);
        if (player.getGameMode() != GameMode.CREATIVE) {

            plugin.getDebugger().debug(
                    "[BuilderCreative] Another plugin cancelled the creative switch for {0}; dropping builder perms.",
                    player.getName());
            endBuilderCreative(player);

        }

    }

    /**
     * Removes the whole exemption. Safe to call for players that never had one.
     * Callers that restore the player's inventory must do so before revoking, so
     * GameModeInventories cannot swap inventories mid-restore.
     */
    public void revoke(Player player) {

        builders.remove(player.getUniqueId());
        PermissionAttachment attachment = exemptions.remove(player.getUniqueId());
        if (attachment == null) {

            return;

        }

        try {

            attachment.remove();

        } catch (IllegalArgumentException ignored) {

            // The player instance is already gone; the attachment died with it.

        }

    }

    public void revokeAll() {

        for (UUID playerId : exemptions.keySet()) {

            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {

                revoke(player);

            } else {

                builders.remove(playerId);
                exemptions.remove(playerId);

            }

        }

    }

    // The builder left creative by any route (round rotation to adventure, the
    // MiniGamesBox end-of-game restore to survival, an admin /gamemode). The
    // demotion is deferred a tick so the inventory restore that triggered the
    // change finishes with the no-swap permission still attached; that
    // permission itself stays until the player leaves the arena.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {

        Player player = event.getPlayer();
        if (event.getNewGameMode() == GameMode.CREATIVE || !builders.contains(player.getUniqueId())) {

            return;

        }

        Bukkit.getScheduler().runTask(plugin, () -> {

            if (player.getGameMode() != GameMode.CREATIVE) {

                endBuilderCreative(player);

            }

        });

    }

    // A participant left the arena worlds. Builders still in creative are
    // flipped to survival before the attachment is removed, so
    // GameModeInventories neither swaps inventories on the flip nor sees a
    // sanctioned creative outside the arena.
    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {

        Player player = event.getPlayer();
        if (!exemptions.containsKey(player.getUniqueId()) || isArenaWorld(player.getWorld())) {

            return;

        }

        if (builders.contains(player.getUniqueId()) && player.getGameMode() == GameMode.CREATIVE) {

            player.setGameMode(GameMode.SURVIVAL);

        }

        revoke(player);

    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {

        revoke(event.getPlayer());

    }

    private PermissionAttachment ensureAttachment(Player player) {

        return exemptions.computeIfAbsent(player.getUniqueId(), id -> {

            PermissionAttachment attachment = player.addAttachment(plugin);
            attachment.setPermission(GMI_USE_PERMISSION, false);
            return attachment;

        });

    }

    private void endBuilderCreative(Player player) {

        if (!builders.remove(player.getUniqueId())) {

            return;

        }

        PermissionAttachment attachment = exemptions.get(player.getUniqueId());
        if (attachment == null) {

            return;

        }

        attachment.unsetPermission(GMI_ANYWHERE_PERMISSION);
        attachment.unsetPermission(GMI_BYPASS_PERMISSION);

    }

    private boolean isArenaWorld(World world) {

        if (world == null) {

            return false;

        }

        for (World arenaWorld : plugin.getArenaRegistry().getArenaWorlds()) {

            if (arenaWorld != null && arenaWorld.getName().equals(world.getName())) {

                return true;

            }

        }

        for (World arenaWorld : plugin.getArenaRegistry().getArenaIngameWorlds()) {

            if (arenaWorld != null && arenaWorld.getName().equals(world.getName())) {

                return true;

            }

        }

        // Plot worlds are BuildBattle-specific and not part of the MiniGamesBox
        // world lists, so fall back to the arenas.yml-derived set.
        return plugin.getMyWorldsManager().isConfiguredArenaWorld(world.getName());

    }

}
