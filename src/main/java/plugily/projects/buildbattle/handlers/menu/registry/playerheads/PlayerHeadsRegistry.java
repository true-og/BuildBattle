/*
 *
 * BuildBattle - Ultimate building competition minigame
 * Copyright (C) 2022 Plugily Projects - maintained by Tigerpanzer_02 and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package plugily.projects.buildbattle.handlers.menu.registry.playerheads;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import plugily.projects.buildbattle.Main;
import plugily.projects.buildbattle.handlers.menu.OptionsRegistry;
import plugily.projects.buildbattle.handlers.misc.HeadDatabaseManager;
import plugily.projects.minigamesbox.classic.handlers.language.MessageBuilder;
import plugily.projects.minigamesbox.classic.utils.configuration.ConfigUtils;
import plugily.projects.minigamesbox.classic.utils.helper.ItemBuilder;
import plugily.projects.minigamesbox.classic.utils.helper.ItemUtils;
import plugily.projects.minigamesbox.classic.utils.misc.complement.ComplementAccessor;
import plugily.projects.minigamesbox.classic.utils.version.xseries.XMaterial;
import plugily.projects.minigamesbox.inventory.utils.fastinv.InventoryScheme;
import plugily.projects.minigamesbox.inventory.utils.fastinv.PaginatedFastInv;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * @author Plajer
 *         <p>
 *         Created at 23.12.2018
 */
public class PlayerHeadsRegistry {

    private static final Pattern HEAD_ENTRY_PATTERN = Pattern.compile(
            "\\{\\s*\"name\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"\\s*,\\s*\"value\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"\\s*}");

    private final Main plugin;
    private final Map<HeadsCategory, PaginatedFastInv> categories = new HashMap<>();

    public PlayerHeadsRegistry(OptionsRegistry registry) {

        this.plugin = registry.getPlugin();
        registerCategories();

    }

    private void registerCategories() {

        FileConfiguration config = ConfigUtils.getConfig(plugin, "heads/mainmenu");
        for (String str : config.getKeys(false)) {

            if (!config.getBoolean(str + ".enabled", true)) {

                continue;

            }

            if (str.equalsIgnoreCase("Do-Not-Edit")) {

                continue;

            }

            if (config.getBoolean(str + ".database", false)) {

                String categoryName = config.getString(str + ".config", "fail");
                if (categoryName.equalsIgnoreCase("search")) {

                    HeadsCategory category = new HeadsCategory(str);
                    category.setItemStack(new ItemBuilder(ItemUtils.getSkull(config.getString(str + ".texture")))
                            .name(new MessageBuilder(config.getString(str + ".displayname"))
                                    .value(categoryName.toUpperCase()).build())
                            .lore(config.getStringList(str + ".lore").stream()
                                    .map(lore -> new MessageBuilder(lore).value(categoryName.toUpperCase()).build())
                                    .collect(Collectors.toList()))
                            .glowEffect().build());
                    category.setPermission(config.getString(str + ".permission"));
                    category.setSearch(true);
                    categories.put(category, null);

                } else {

                    CompletableFuture.supplyAsync(() -> plugin.getHeadDatabaseManager().getDatabase(categoryName))
                            .thenAccept(download ->
                            {

                                if (download != HeadDatabaseManager.DownloadStatus.FAIL) {

                                    useDatabaseHeads(config, str);

                                }

                            });

                }

                continue;

            }

            HeadsCategory category = new HeadsCategory(str);

            category.setItemStack(
                    new ItemBuilder(ItemUtils.getSkull(config.getString(str + ".texture")))
                            .name(new MessageBuilder(config.getString(str + ".displayname")).build())
                            .lore(config.getStringList(str + ".lore").stream()
                                    .map(lore -> new MessageBuilder(lore).build()).collect(Collectors.toList()))
                            .build());
            category.setPermission(config.getString(str + ".permission"));

            Set<ItemStack> playerHeads = new HashSet<>();
            FileConfiguration categoryConfig = ConfigUtils.getConfig(plugin,
                    "heads/menus/" + config.getString(str + ".config"));
            for (String path : categoryConfig.getKeys(false)) {

                if (!categoryConfig.getBoolean(path + ".enabled", true)) {

                    continue;

                }

                ItemStack stack = ItemUtils.getSkull(categoryConfig.getString(path + ".texture"));
                ItemMeta im = stack.getItemMeta();

                ComplementAccessor.getComplement().setDisplayName(im,
                        new MessageBuilder(categoryConfig.getString(path + ".displayname")).build());
                ComplementAccessor.getComplement().setLore(im, categoryConfig.getStringList(path + ".lore").stream()
                        .map(lore -> new MessageBuilder(lore).build()).collect(Collectors.toList()));
                stack.setItemMeta(im);
                playerHeads.add(stack);

            }

            createPaginatedInventory(str, config, playerHeads, category);

        }

    }

    private void createPaginatedInventory(String str, FileConfiguration config, Collection<ItemStack> playerHeads,
            HeadsCategory category)
    {

        PaginatedFastInv gui = new PaginatedFastInv(54, new MessageBuilder(config.getString(str + ".menuname"))
                .value(config.getString(str + ".config")).build());
        new InventoryScheme().mask("111111111").mask("111111111").mask("111111111").mask("111111111").mask("111111111")
                .bindPagination('1').apply(gui);

        gui.previousPageItem(45, p -> new ItemBuilder(XMaterial.ARROW.parseItem())
                .name("&7<- &6" + p + "&7/&6" + gui.lastPage()).colorizeItem().build());
        gui.addPageChangeHandler(openedPage -> {

            gui.setItem(49, new ItemBuilder(XMaterial.BARRIER.parseItem()).name("&7X &6" + openedPage + " &7X")
                    .colorizeItem().build(), e -> e.getWhoClicked().closeInventory());

        });
        gui.nextPageItem(53, p -> new ItemBuilder(XMaterial.ARROW.parseItem())
                .name("&6 " + p + "&7/&6" + gui.lastPage() + " &7->").colorizeItem().build());

        plugin.getOptionsRegistry().addGoBackItem(gui, 46);

        if (playerHeads.size() > 200) {

            List<ItemStack> heads = new ArrayList<>(playerHeads);
            Collections.shuffle(heads);
            int start = plugin.getRandom().nextInt(heads.size() - 225);
            playerHeads = heads.subList(start, start + 225);

        }

        for (ItemStack playerHead : playerHeads) {

            gui.addContent(playerHead,
                    clickEvent -> clickEvent.getWhoClicked().getInventory().addItem(playerHead.clone()));

        }

        plugin.getOptionsRegistry().addGoBackItem(gui, gui.getInventory().getSize() - 1);
        category.setGui(gui);
        categories.put(category, gui);

    }

    public void useDatabaseHeads(FileConfiguration config, String str) {

        HeadsCategory category = new HeadsCategory(str);
        String categoryName = config.getString(str + ".config", "fail");

        category.setItemStack(new ItemBuilder(ItemUtils.getSkull(config.getString(str + ".texture")))
                .name(new MessageBuilder(config.getString(str + ".displayname")).value(categoryName.toUpperCase())
                        .build())
                .lore(config.getStringList(str + ".lore").stream()
                        .map(lore -> new MessageBuilder(lore).value(categoryName.toUpperCase()).build())
                        .collect(Collectors.toList()))
                .build());
        category.setPermission(config.getString(str + ".permission"));

        CompletableFuture.supplyAsync(() -> loadHeadsFromYML(categoryName))
                .thenAccept(playerHeads -> createPaginatedInventory(str, config, playerHeads.values(), category));

    }

    private final Map<String, Map<String, ItemStack>> headsDatabase = new HashMap<>();

    public Map<String, ItemStack> loadHeadsFromYML(String name) {

        // Should do this in async thread to do not cause dead for the main thread
        long start = System.currentTimeMillis();
        File file = new File(plugin.getDataFolder(), "heads/database/" + name + ".yml");
        Map<String, String> heads = new HashMap<>();
        if (file.exists()) {

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)))
            {

                String content = reader.lines().collect(Collectors.joining("\n"));
                if (content.trim().startsWith("[")) {

                    loadHeadsFromJson(content, heads);

                } else {

                    loadHeadsFromLegacyFlatFile(content, heads);

                }

            } catch (Exception e) {

                plugin.getDebugger().debug(Level.WARNING, "Cannot load file heads/database/" + name + ".yml!");

            }

        } else {

            plugin.getDebugger().debug(Level.WARNING, "File heads/database/" + name + ".yml does not exist!");

        }

        plugin.getDebugger().debug("[System] [Plugin] Head file loading " + name + " finished took ms"
                + (System.currentTimeMillis() - start));

        long secondStart = System.currentTimeMillis();
        Map<String, ItemStack> playerHeads = new HashMap<>();
        for (Map.Entry<String, String> entry : heads.entrySet()) {

            if (entry.getKey().toLowerCase().contains("(dup)")) {

                continue;

            }

            ItemStack stack = ItemUtils.getSkull(entry.getValue());
            ItemMeta im = stack.getItemMeta();

            ComplementAccessor.getComplement().setDisplayName(im, new MessageBuilder(entry.getKey()).build());
            ComplementAccessor.getComplement().setLore(im,
                    Collections.singletonList(new MessageBuilder("MENU_OPTION_CONTENT_HEADS_DATABASE_LORE").asKey()
                            .value(entry.getKey()).build()));
            stack.setItemMeta(im);
            playerHeads.put(entry.getKey(), stack);

        }

        headsDatabase.put(name, playerHeads);
        plugin.getDebugger().debug("[System] [Plugin] Head textures loading " + name + " finished took ms"
                + (System.currentTimeMillis() - secondStart));
        return playerHeads;

    }

    private void loadHeadsFromLegacyFlatFile(String content, Map<String, String> heads) {

        for (String line : content.split("\\R")) {

            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {

                continue;

            }

            int colonIndex = line.indexOf(": ");
            if (colonIndex == -1) {

                continue;

            }

            String key = line.substring(0, colonIndex);
            String value = line.substring(colonIndex + 2);
            if (value.startsWith("\"") && value.endsWith("\"")) {

                value = value.substring(1, value.length() - 1);

            }

            if (value.startsWith("'") && value.endsWith("'")) {

                value = value.substring(1, value.length() - 1);

            }

            heads.put(key, value);

        }

    }

    private void loadHeadsFromJson(String content, Map<String, String> heads) {

        Matcher matcher = HEAD_ENTRY_PATTERN.matcher(content);
        int duplicateIndex = 0;
        while (matcher.find()) {

            String key = unescapeJson(matcher.group(1));
            String value = unescapeJson(matcher.group(2));
            if (heads.containsKey(key)) {

                key = key + " (dup) " + duplicateIndex++;

            }

            heads.put(key, value);

        }

    }

    private String unescapeJson(String value) {

        StringBuilder output = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {

            char current = value.charAt(i);
            if (current != '\\' || i + 1 >= value.length()) {

                output.append(current);
                continue;

            }

            char escaped = value.charAt(++i);
            switch (escaped) {

                case '"':
                case '\\':
                case '/':
                    output.append(escaped);
                    break;
                case 'b':
                    output.append('\b');
                    break;
                case 'f':
                    output.append('\f');
                    break;
                case 'n':
                    output.append('\n');
                    break;
                case 'r':
                    output.append('\r');
                    break;
                case 't':
                    output.append('\t');
                    break;
                case 'u':
                    if (i + 4 < value.length()) {

                        String unicode = value.substring(i + 1, i + 5);
                        output.append((char) Integer.parseInt(unicode, 16));
                        i += 4;
                        break;

                    }

                    output.append('u');
                    break;
                default:
                    output.append(escaped);
                    break;

            }

        }

        return output.toString();

    }

    public Map<HeadsCategory, PaginatedFastInv> getCategories() {

        return categories;

    }

    public Map<String, Map<String, ItemStack>> getHeadsDatabase() {

        return headsDatabase;

    }

}
