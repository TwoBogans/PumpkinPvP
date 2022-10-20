package org.au2b2t.pumpkinpvp;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Explosive;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.stream.Collectors;

public final class PumpkinPVP extends JavaPlugin implements Listener {

    private boolean bedwars;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        bedwars = getConfig().getBoolean("bedwars", false);
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void on(BlockPlaceEvent e) {
        if (e.getBlock().getType() == Material.PUMPKIN) {
            this.createExplosion(e.getBlock(), e.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void on(PlayerInteractEvent e) {
        if (e.getClickedBlock() != null) {
            final Block block = e.getClickedBlock();

            if (block.getType() == Material.PUMPKIN) {
                this.createExplosion(block, e.getPlayer());
            }

            if (bedwars && block.getType() == Material.WOOL && e.getItem().getType() == Material.END_CRYSTAL) {
                final World world = block.getWorld();
                final Location loc = block.getLocation().add(0.5, 1, 0.5);

                if (world.getNearbyEntitiesByType(EnderCrystal.class, loc, 0.5D).size() > 1) {
                    return;
                }

                world.spawn(loc, EnderCrystal.class, enderCrystal -> enderCrystal.setShowingBottom(false));

                consumeItem(e.getPlayer(), 1, Material.END_CRYSTAL);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void on(PumpkinDamageEvent e) {
        if (e.getEntity() instanceof Player) {
            final Player player = (Player) e.getEntity();
            if (e.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
                e.setCancelled(isSurrounded(player));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void on(EntityExplodeEvent e) {
        e.blockList().removeIf(block -> bedwars && (block.getType() == Material.WOOL));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void on(BlockExplodeEvent e) {
        e.blockList().removeIf(block -> bedwars && block.getType() == Material.WOOL);
    }

    private boolean isSurrounded(final Player player) {
        final Location loc = player.getLocation().add(0,1,0); // gets the block they're standing on plus 1
        final Block block1 = loc.add(0,1,0).getBlock(); // gets the block at players face
        final Block block2 = loc.getBlock();

        for (BlockFace face : new BlockFace[]{ BlockFace.EAST, BlockFace.WEST, BlockFace.NORTH, BlockFace.SOUTH }) {
            if (block1.getRelative(face).getType() == Material.PUMPKIN ||
                block2.getRelative(face).getType() == Material.PUMPKIN
            ) {
                getLogger().info(String.format("false={face=%s block1=%s block2=%s}",
                        face,
                        block1.getRelative(face).getType(),
                        block2.getRelative(face).getType()));
                return false; // can do damage
            }
            if (block1.getRelative(face).getType() != Material.AIR &&
                block2.getRelative(face).getType() != Material.AIR
            ) {
                getLogger().info(String.format("true={face=%s block1=%s block2=%s}",
                        face,
                        block1.getRelative(face).getType(),
                        block2.getRelative(face).getType()));
                return true; // surrounded
            }
        }

        return false;
    }

    private void createExplosion(final Block block, final Player placer) {
        final Block below = block.getLocation().add(0.0D, -1.0D, 0.0D).getBlock();
        if (below.getType() == Material.BEDROCK || below.getType() == Material.OBSIDIAN || (bedwars && below.getType() == Material.WOOL)) {
            getServer().getScheduler().runTask(this, () -> {
                block.setMetadata("dmp.pumpkinPlacer", new FixedMetadataValue(this, placer.getUniqueId().toString()));

                block.getWorld().createExplosion(block.getLocation(), 8.0F, true, true);

                for (Player nearbyPlayer : block.getWorld().getNearbyPlayers(block.getLocation(), 8)) {
                    PumpkinDamageEvent damageEvent = new PumpkinDamageEvent(block, nearbyPlayer, placer);
                    getServer().getPluginManager().callEvent(damageEvent);
                }

                block.setType(Material.AIR);
            });
        }
    }

    public void consumeItem(Player player, int count, Material mat) {
        Map<Integer, ? extends ItemStack> ammo = player.getInventory().all(mat);

        int found = 0;
        for (ItemStack stack : ammo.values()) {
            found += stack.getAmount();
        }
        if (count > found) {
            return;
        }

        for (Integer index : ammo.keySet()) {
            ItemStack stack = ammo.get(index);

            int removed = Math.min(count, stack.getAmount());
            count -= removed;

            if (stack.getAmount() == removed) {
                player.getInventory().setItem(index, null);
            }
            else {
                stack.setAmount(stack.getAmount() - removed);
            }

            if (count <= 0) {
                break;
            }
        }

        player.updateInventory();
    }

}
