package plugily.projects.buildbattle.minigamesbox.classic.utils.services;

import org.bukkit.plugin.java.JavaPlugin;

import plugily.projects.minigamesbox.classic.PluginMain;
import plugily.projects.minigamesbox.classic.utils.services.locale.LocaleService;

public class ServiceRegistry {

    private static PluginMain registeredService;
    private static boolean serviceEnabled;
    private static long serviceCooldown;
    private static LocaleService localeService;

    public static boolean registerService(PluginMain plugin) {

        if (registeredService != null && registeredService.equals(plugin)) {

            return false;

        }

        registeredService = plugin;
        serviceEnabled = false;
        return true;

    }

    public static JavaPlugin getRegisteredService() {

        return registeredService;

    }

    public static long getServiceCooldown() {

        return serviceCooldown;

    }

    public static void setServiceCooldown(long cooldown) {

        serviceCooldown = cooldown;

    }

    public static LocaleService getLocaleService(JavaPlugin plugin) {

        if (!serviceEnabled || registeredService == null || !registeredService.equals(plugin)) {

            return null;

        }

        return localeService;

    }

    public static boolean isServiceEnabled() {

        return serviceEnabled;

    }

}
