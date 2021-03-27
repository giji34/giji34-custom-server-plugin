package com.github.giji34.t;

import com.github.giji34.t.command.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class Main extends JavaPlugin implements Listener {
    private final ToggleGameModeCommand toggleGameModeCommand = new ToggleGameModeCommand();
    private final TeleportCommand teleportCommand = new TeleportCommand(this);
    private final EditCommand editCommand = new EditCommand(this);
    private final PortalCommand portalCommand = new PortalCommand(this);
    private Permission permission;
    private MobSpawnProhibiter mobSpawnProhibiter;
    private Borders borders;

    public Main() {
    }

    @Override
    public void onLoad() {
        try {
            File jar = getFile();
            File pluginDirectory = new File(jar.getParent(), "giji34");
            this.permission = new Permission(new File(pluginDirectory, "permission.yml"));
            this.teleportCommand.init(pluginDirectory);
            this.editCommand.init(pluginDirectory);
            this.portalCommand.init(pluginDirectory);
            this.mobSpawnProhibiter = new MobSpawnProhibiter(new File(pluginDirectory, "mob_spawn_allowed_regions.yml"), this);
            this.borders = new Borders(new File(pluginDirectory, "borders.yml"));
        } catch (Exception e) {
            getLogger().warning("error: " + e);
        }
    }

    private void reload() {
        File jar = getFile();
        File pluginDirectory = new File(jar.getParent(), "giji34");
        this.permission = new Permission(new File(pluginDirectory, "permission.yml"));
        this.portalCommand.reload();
        this.mobSpawnProhibiter = new MobSpawnProhibiter(new File(pluginDirectory, "mob_spawn_allowed_regions.yml"), this);
        this.borders = new Borders(new File(pluginDirectory, "borders.yml"));
        Server server = getServer();
        CommandSender console = server.getConsoleSender();
        server.dispatchCommand(console, "whitelist reload");
    }

    @Override
    public void onEnable() {
        PluginCommand tpb = getCommand("tpb");
        if (tpb != null) {
            tpb.setTabCompleter(new TeleportLandmarkTabCompleter(teleportCommand, 0));
        }
        PluginCommand gfill = getCommand("gfill");
        if (gfill != null) {
            gfill.setTabCompleter(new BlockNameTabCompleter());
        }
        PluginCommand greplace = getCommand("greplace");
        if (greplace != null) {
            greplace.setTabCompleter(new BlockNameTabCompleter());
        }
        PluginCommand gtree = getCommand("gtree");
        if (gtree != null) {
            gtree.setTabCompleter(new TreeTypeTabCompleter());
        }
        PluginCommand guide = getCommand("guide");
        if (guide != null) {
            guide.setTabCompleter(new TeleportLandmarkTabCompleter(teleportCommand, 1));
        }
        PluginCommand connect = getCommand("connect");
        if (connect != null) {
            connect.setTabCompleter(new StringListTabCompleter(new String[]{"2434_main", "2434_world06", "hololive_01", "en_hololive", "hololive_00"}));
        }
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            return false;
        }
        Player player = (Player) sender;
        if (invalidGameMode(player)) {
            return false;
        }
        if (!this.permission.hasPermission(player, label)) {
            player.sendMessage(ChatColor.RED + label + "コマンドを実行する権限がありません");
            return true;
        }
        switch (label) {
            case "tpl":
                return teleportCommand.teleport(player, args);
            case "tpb":
                return teleportCommand.teleportToLandmark(player, args);
            case "gm":
                toggleGameModeCommand.toggle(player);
                return true;
            case "gfill":
                return editCommand.fill(player, args);
            case "greplace":
                return editCommand.replace(player, args);
            case "gundo":
                return editCommand.undo(player);
            case "gregenerate":
                return editCommand.regenerate(player, args);
            case "gtree":
                return editCommand.tree(player, args);
            case "guide":
                return teleportCommand.guide(player, this.borders, args);
            case "follow":
                return teleportCommand.follow(player, args);
            case "create_inter_server_portal":
                return portalCommand.createInterServerPortal(player, args, editCommand);
            case "create_intra_server_portal":
                return portalCommand.createIntraServerPortal(player, args, editCommand);
            case "delete_portal":
                return portalCommand.delete(player, args);
            case "fell_trees":
                return editCommand.fellTrees(player);
            case "/chunk":
                return editCommand.chunk(player);
            case "uuid":
                World world = player.getWorld();
                getLogger().info(world.getName() + ":" + world.getUID().toString());
                return true;
            case "connect":
                return this.handleConnectCommand(player, args);
            case "kusa":
                return editCommand.kusa(player);
            case "giji34":
                return this.handleAdminCommand(player, args);
            default:
                return false;
        }
    }

    private boolean invalidGameMode(Player player) {
        GameMode current = player.getGameMode();
        return current != GameMode.CREATIVE && current != GameMode.SPECTATOR;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        if (this.permission.hasRole(player, "member")) {
            if (invalidGameMode(player)) {
                return;
            }
            if (!e.hasItem()) {
                return;
            }
            ItemStack tool = e.getItem();
            if (tool == null) {
                return;
            }
            Material toolType = tool.getType();
            if (toolType == Material.WOODEN_AXE) {
                Block block = e.getClickedBlock();
                if (block == null) {
                    return;
                }
                EquipmentSlot hand = e.getHand();
                if (hand != EquipmentSlot.HAND) {
                    return;
                }
                Loc loc = Loc.fromVectorFloored(block.getLocation().toVector());
                Action action = e.getAction();
                if (action == Action.LEFT_CLICK_BLOCK) {
                    editCommand.setSelectionStartBlock(player, loc);
                } else if (action == Action.RIGHT_CLICK_BLOCK) {
                    editCommand.setSelectionEndBlock(player, loc);
                } else {
                    return;
                }
                e.setCancelled(true);
            } else if (toolType == Material.SEAGRASS) {
                Block block = e.getClickedBlock();
                if (block == null) {
                    return;
                }
                EquipmentSlot hand = e.getHand();
                if (hand != EquipmentSlot.HAND) {
                    return;
                }
                Action action = e.getAction();
                if (action != Action.LEFT_CLICK_BLOCK) {
                    return;
                }
                Loc loc = Loc.fromVectorFloored(block.getLocation().toVector());
                if (EditCommand.TallSeaGrass(player, loc)) {
                    e.setCancelled(true);
                }
            }
        } else {
            if (this.shouldRejectInteraction(e)) {
                e.setCancelled(true);
            }
        }
    }

    private boolean shouldRejectInteraction(PlayerInteractEvent e) {
        Action action = e.getAction();
        Block block = e.getClickedBlock();
        Player player = e.getPlayer();
        switch (action) {
            case PHYSICAL: {
                if (block == null) {
                    return true;
                }
                Material material = block.getType();
                if (material.isInteractable()) {
                    return false;
                }
                switch (material) {
                    case FARMLAND:
                        return true;
                    default:
                        return false;
                }
            }
            case RIGHT_CLICK_BLOCK: {
                if (block == null) {
                    return true;
                }
                if (player.isSneaking()) {
                    return true;
                }
                Material material = block.getType();
                switch (material) {
                    case CAKE:
                        return true;
                    case RAIL:
                    case POWERED_RAIL:
                    case ACTIVATOR_RAIL:
                    case DETECTOR_RAIL:
                        if (!e.hasItem()) {
                            return true;
                        }
                        ItemStack itemStack = e.getItem();
                        if (itemStack != null && itemStack.getType() == Material.MINECART) {
                            return false;
                        }
                        return true;
                }
                if (MaterialHelper.isBoat(e.getMaterial()) && e.getHand() == EquipmentSlot.HAND) {
                    return false;
                }
                if (material.isInteractable()) {
                    return false;
                }
                return true;
            }
            default:
                return true;
        }
    }

    static void Repair(@NotNull ItemStack item) {
        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta instanceof Damageable) {
            Damageable damageable = (Damageable) itemMeta;
            damageable.setDamage(0);
            item.setItemMeta(itemMeta);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        Player player = e.getPlayer();
        if (invalidGameMode(player)) {
            return;
        }
        PlayerInventory inventory = player.getInventory();
        ItemStack tool = inventory.getItemInMainHand();
        if (tool.getType() != Material.WOODEN_AXE) {
            return;
        }
        e.setCancelled(true);
    }

    @EventHandler
    public void onCreatureSpawnSpawn(CreatureSpawnEvent e) {
        CreatureSpawnEvent.SpawnReason reason = e.getSpawnReason();
        LivingEntity entity = e.getEntity();
        EntityType entityType = entity.getType();
        switch (reason) {
            case SPAWNER_EGG:
                switch (entityType) {
                    case WITHER:
                    case ENDER_DRAGON:
                        e.setCancelled(true);
                        break;
                }
                return;
            case VILLAGE_INVASION:
                e.setCancelled(true);
                getLogger().info("村の襲撃: " + entityType + " のスポーンをキャンセルしました");
                return;
            case NATURAL:
                if (this.mobSpawnProhibiter.isMobSpawnAllowed(e.getLocation())) {
                    switch (entityType) {
                        case WANDERING_TRADER:
                        case TRADER_LLAMA:
                            // 作業の邪魔なのでスポーンを阻止する
                            getLogger().info("行商人: " + entityType + " のスポーンをキャンセルしました");
                            e.setCancelled(true);
                            break;
                    }
                } else {
                    e.setCancelled(true);
                }
                return;
            case BUILD_WITHER:
                e.setCancelled(true);
                return;
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Portal portal = portalCommand.filterPortalByCooldown(player, portalCommand.findPortal(player));
        if (portal != null) {
            portalCommand.markPortalUsed(player, portal);
            portal.apply(player, this);
            return;
        }

        if (this.permission.hasRole(player, "member")) {
            return;
        }
        this.borders.correct(player);
    }

    static final String[] kCanPlaceOnRails = {
            "minecraft:powered_rail",
            "minecraft:detector_rail",
            "minecraft:activator_rail",
            "minecraft:rail",
    };

    private static List<String> GetMissingRails(ItemStack itemStack) {
        List<String> canPlaceOn = ItemStackHelper.GetCanPlaceOn(itemStack);
        ArrayList<String> rails = new ArrayList<>();
        for (String rail : kCanPlaceOnRails) {
            if (!canPlaceOn.contains(rail)) {
                rails.add(rail);
            }
        }
        return rails;
    }

    private static String TagCanPlaceOn(List<String> canPlaceOnRails) {
        String rails = String.join(",", canPlaceOnRails.stream().map(t -> "\"" + t + "\"").collect(Collectors.toList()));
        return "CanPlaceOn:[" + rails + "]";
    }

    private static String ItemStringMinecart(List<String> canPlaceOnRails) {
        String s = "minecraft:minecart";
        if (!canPlaceOnRails.isEmpty()) {
            s += "{" + TagCanPlaceOn(canPlaceOnRails) + "}";
        }
        return s;
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent e) {
        if (e.getEntityType() != EntityType.DROPPED_ITEM) {
            return;
        }
        Item item = (Item) e.getEntity();
        ItemStack itemStack = item.getItemStack();
        if (itemStack.getType() != Material.MINECART) {
            return;
        }
        List<String> rails = GetMissingRails(itemStack);
        if (rails.isEmpty()) {
            return;
        }
        UUID id = e.getEntity().getUniqueId();
        Server server = getServer();
        server.getScheduler().runTask(this, () -> {
            ConsoleCommandSender console = server.getConsoleSender();
            String command = "data merge entity " + id + " {Item:{tag:{" + TagCanPlaceOn(rails) + "}}}";
            server.dispatchCommand(console, command);
        });
    }

    @EventHandler
    public void onPlayerJoined(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        addPotionEffects(player);
        if (this.permission.hasRole(player, "member")) {
            portalCommand.setAnyPortalCooldown(player);

            Location loc = portalCommand.getPortalReturnLocation(player);
            if (loc == null) {
                return;
            }
            player.teleport(loc, PlayerTeleportEvent.TeleportCause.PLUGIN);
        } else {
            Server server = player.getServer();
            for (World world : server.getWorlds()) {
                if (world.getEnvironment() == World.Environment.NORMAL) {
                    Location loc = world.getSpawnLocation();
                    player.teleport(loc, PlayerTeleportEvent.TeleportCause.PLUGIN);
                    break;
                }
            }
            server.getScheduler().runTaskLater(this, () -> {
                PlayerInventory inventory = player.getInventory();
                boolean hasDiamondSword = false;
                boolean hasBoat = false;
                for (int i = 0; i < inventory.getSize(); i++) {
                    ItemStack itemStack = inventory.getItem(i);
                    if (itemStack == null) {
                        continue;
                    }
                    Material material = itemStack.getType();
                    if (material == Material.DIAMOND_SWORD) {
                        Repair(itemStack);
                        hasDiamondSword = true;
                    } else if (MaterialHelper.isBoat(material)) {
                        hasBoat = true;
                    }
                }
                if (!hasDiamondSword) {
                    inventory.addItem(new ItemStack(Material.DIAMOND_SWORD));
                }
                if (!hasBoat) {
                    inventory.addItem(new ItemStack(Material.OAK_BOAT));
                }
                ConsoleCommandSender console = server.getConsoleSender();
                ItemStack offHand = inventory.getItemInOffHand();
                if (inventory.contains(Material.MINECART) || (offHand.getType() == Material.MINECART)) {
                    ItemStack[] storageContents = inventory.getStorageContents();
                    for (int i = 0; i < storageContents.length; i++) {
                        ItemStack itemStack = storageContents[i];
                        if (itemStack == null) {
                            continue;
                        }
                        if (itemStack.getType() != Material.MINECART) {
                            continue;
                        }
                        List<String> rails = GetMissingRails(itemStack);
                        if (!rails.isEmpty()) {
                            String category = i < 9 ? "hotbar" : "inventory";
                            int offset = i < 9 ? 0 : 9;
                            String command = "replaceitem entity " + player.getUniqueId() + " " + category + "." + (i - offset) + " " + ItemStringMinecart(rails);
                            server.dispatchCommand(console, command);
                        }
                    }

                    if (offHand.getType() == Material.MINECART) {
                        List<String> rails = GetMissingRails(offHand);
                        if (!rails.isEmpty()) {
                            String command = "replaceitem entity " + player.getUniqueId() + " weapon.offhand " + ItemStringMinecart(rails);
                            server.dispatchCommand(console, command);
                        }
                    }
                } else {
                    server.dispatchCommand(console, "give " + player.getName() + " " + ItemStringMinecart(Arrays.asList(kCanPlaceOnRails.clone())));
                }
            }, 40);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        final Player player = event.getPlayer();
        player.getServer().getScheduler().runTaskLater(this, () -> {
            addPotionEffects(player);
        }, 40);
    }

    private void addPotionEffects(Player player) {
        ArrayList<PotionEffect> effects = new ArrayList<PotionEffect>();
        int duration = 7 * 24 * 60 * 60 * 20;
        int amplifier = 0;
        boolean ambient = false;
        boolean particles = false;
        PotionEffect nightVision = new PotionEffect(PotionEffectType.NIGHT_VISION, duration, amplifier, ambient, particles);
        PotionEffect saturation = new PotionEffect(PotionEffectType.SATURATION, duration, amplifier, ambient, particles);
        PotionEffect instantHealth = new PotionEffect(PotionEffectType.HEAL, duration, amplifier, ambient, particles);
        PotionEffect waterBreathing = new PotionEffect(PotionEffectType.WATER_BREATHING, duration, amplifier, ambient, particles);
        effects.add(nightVision);
        effects.add(saturation);
        effects.add(instantHealth);
        effects.add(waterBreathing);
        if (!player.addPotionEffects(effects)) {
            getLogger().warning("failed adding portion effects for player: " + player.getName());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Portal portal = portalCommand.getCoolingdownPortal(player);
        if (portal == null) {
            portalCommand.setPortalReturnLocation(player, null);
        } else if (portal instanceof InterServerPortal) {
            InterServerPortal interServerPortal = (InterServerPortal) portal;
            Location returnLoc = interServerPortal.returnLoc;
            if (returnLoc != null) {
                Location loc = player.getLocation();
                loc.setX(returnLoc.getX());
                loc.setY(returnLoc.getY());
                loc.setZ(returnLoc.getZ());
                loc.setYaw(returnLoc.getYaw());
                portalCommand.setPortalReturnLocation(player, loc);
            }
        }
        this.borders.forget(player);
    }

    private boolean handleConnectCommand(Player player, String[] args) {
        if (args.length != 1) {
            player.sendMessage(ChatColor.RED + "接続先を指定してください. (例) /connect 2434_main");
            return false;
        }
        String destination = args[0];
        if (destination.equals("2434_main")) {
            destination = "main";
        } else if (destination.equals("2434_world06")) {
            destination = "world06";
        }
        try {
            InterServerPortal.Connect(player, destination, this);
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + args[0] + " に接続できませんでした");
            getLogger().warning(player.getName() + " failed connecting server " + destination);
            return false;
        }
        player.sendMessage(args[0] + " に接続しています...");
        return true;
    }

    private boolean handleAdminCommand(Player player, String[] args) {
        if (!this.permission.hasRole(player, "admin")) {
            return false;
        }
        if (!player.isOp()) {
            return false;
        }
        if (args.length < 1) {
            return false;
        }
        String subCommand = args[0];
        switch (subCommand) {
            case "reload": {
                this.reload();
                player.sendMessage("reload しました");
                return true;
            }
            default:
                return false;
        }
    }

    @EventHandler
    public void onEntityPotionEffect(EntityPotionEffectEvent e) {
        EntityPotionEffectEvent.Cause cause = e.getCause();
        if (cause != EntityPotionEffectEvent.Cause.BEACON) {
            return;
        }
        Entity entity = e.getEntity();
        if (e.getEntityType() != EntityType.PLAYER) {
            return;
        }
        if (!(entity instanceof Player)) {
            return;
        }
        Player player = (Player) entity;
        if (!this.permission.hasRole(player, "member")) {
            return;
        }
        GameMode gameMode = player.getGameMode();
        if (gameMode != GameMode.CREATIVE) {
            return;
        }
        e.setCancelled(true);
    }
}
