package me.gujix64.pickupchest;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class PickUpChest extends JavaPlugin implements Listener  {

    private FileConfiguration config;

    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);

        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }

        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        config = YamlConfiguration.loadConfiguration(configFile);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || !event.getPlayer().isSneaking()) {
            return;
        }

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock.getType() != Material.CHEST) {
            return;
        }

        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        if (config.contains(playerUUID + ".chest-contents")) {
            player.sendMessage("You have already picked up a chest!");
            return;
        }

        List<ItemStack> nonEmptySlots = getNonEmptySlots(clickedBlock);
        config.set(playerUUID + ".chest-contents", nonEmptySlots);
        saveConfig();

        clickedBlock.setType(Material.AIR);

        ItemStack chestItem = createChestItem(nonEmptySlots);
        player.getInventory().addItem(chestItem);
    }

    private List<ItemStack> getNonEmptySlots(Block block) {
        Chest chest = (Chest) block.getState();
        Inventory chestInventory = chest.getInventory();
        ItemStack[] contents = chestInventory.getContents();
        List<ItemStack> nonEmptySlots = new ArrayList<>();

        for (ItemStack itemStack : contents) {
            if (itemStack != null) {
                nonEmptySlots.add(itemStack);
            }
        }

        return nonEmptySlots;
    }

    private ItemStack createChestItem(List<ItemStack> nonEmptySlots) {
        ItemStack chestItem = new ItemStack(Material.CHEST);
        ItemMeta chestItemMeta = chestItem.getItemMeta();

        ArrayList<String> lore = new ArrayList<String>();
        for (ItemStack itemStack : nonEmptySlots) {
            lore.add(itemStack.getAmount() + "x " + itemStack.getType().name());
        }

        chestItemMeta.setLore(lore);
        chestItemMeta.setDisplayName("Picked Chest");
        chestItem.setItemMeta(chestItemMeta);

        return chestItem;
    }

    public void saveConfig() {
        try {
            config.save(new File(getDataFolder(), "config.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block placedBlock = event.getBlockPlaced();
        ItemStack itemStack = event.getItemInHand();
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        if (placedBlock.getType() == Material.CHEST && itemStack.hasItemMeta() && itemStack.getItemMeta().getDisplayName().equals("Picked Chest"))
        {
            if (config.contains(playerUUID + ".chest-contents")) {
                List<?> contents = config.getList(playerUUID + ".chest-contents");
                Inventory chestInventory = ((Chest) placedBlock.getState()).getInventory();
                for (Object obj : contents) {
                    if (obj instanceof ItemStack) {
                        chestInventory.addItem((ItemStack) obj);
                    }
                }
                config.set(playerUUID + ".chest-contents", null);
                try {
                    config.save(new File(getDataFolder(), "config.yml"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    @EventHandler
    public void onPlayerDrop(PlayerDropItemEvent event)
    {
        ItemStack item = event.getItemDrop().getItemStack();
        if(item.hasItemMeta() && item.getItemMeta().getDisplayName().equals("Picked Chest"))
        {
            event.setCancelled(true);

        }
    }
    Map<Player,Inventory> storage;

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem != null && clickedItem.hasItemMeta() && clickedItem.getItemMeta().getDisplayName().equals("Picked Chest")) {
            event.setCancelled(true);
            player.updateInventory();
        }

    }
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.hasItemMeta() && item.getItemMeta().getDisplayName().equals("Picked Chest")) {
                event.getDrops().remove(item);
                ItemStack newItem = new ItemStack(item);
                ItemMeta meta = newItem.getItemMeta();
                meta.setDisplayName(item.getItemMeta().getDisplayName());
                newItem.setItemMeta(meta);
                player.setMetadata("ChestToSpawn", new org.bukkit.metadata.FixedMetadataValue(this, newItem));
            }
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (player.hasMetadata("ChestToSpawn")) {
            ItemStack newItem = (ItemStack) player.getMetadata("ChestToSpawn").get(0).value();
            player.getInventory().addItem(newItem);
            player.removeMetadata("ChestToSpawn", this);
        }
    }

}
