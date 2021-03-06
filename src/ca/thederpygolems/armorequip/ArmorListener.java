package com.reactivemc.armor;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.HashMap;
import java.util.UUID;


/**
 * @Author Borela
 * @Author ReactiveMC
 */
public class ArmorListener implements Listener
{
    private final boolean handleDispenser;

    public ArmorListener(boolean handleDispenser) {
        this.handleDispenser = handleDispenser;
        lastEquip = new HashMap<>();
    }
    private final HashMap<String, HashMap<ArmorType, Long>> lastEquip;

    @EventHandler
    public final void onInventoryClick(final InventoryClickEvent e){
        boolean shift = false;
        if(e.isCancelled()){ return; }
        if(e.getClick() == ClickType.SHIFT_LEFT || e.getClick() == ClickType.SHIFT_RIGHT){
            shift = true;
        }
        if(e.getSlotType() != InventoryType.SlotType.ARMOR && e.getSlotType() != InventoryType.SlotType.QUICKBAR && !e.getInventory().getName().equalsIgnoreCase("container.crafting")){ return; }
        HumanEntity whoClicked = e.getWhoClicked();
        if(!(whoClicked instanceof Player)){ return; }
        ItemStack currentItem = e.getCurrentItem();
        if(currentItem == null){ return; }
        ArmorType newArmorType = ArmorType.matchType(shift ? currentItem : e.getCursor());
        int rawSlot = e.getRawSlot();
        if(newArmorType != null && newArmorType.getSlot()+5 != rawSlot){
            return;
        }
        Player player = (Player) whoClicked;
        UUID uniqueId = whoClicked.getUniqueId();
        ItemStack[] oldArmor = player.getEquipment().getArmorContents();
        boolean equipping = true;
        if(rawSlot == 5 || rawSlot == 6 || rawSlot == 7 || rawSlot == 8){
            equipping = false;
        }
        if(shift){
            newArmorType = ArmorType.matchType(currentItem);
            if(newArmorType != null){
                if(newArmorType.equals(ArmorType.HELMET) && (equipping ? whoClicked.getInventory().getHelmet() == null : whoClicked.getInventory().getHelmet() != null) || newArmorType.equals(ArmorType.CHESTPLATE) && (equipping ? whoClicked.getInventory().getChestplate() == null : whoClicked.getInventory().getChestplate() != null) || newArmorType.equals(ArmorType.LEGGINGS) && (equipping ? whoClicked.getInventory().getLeggings() == null : whoClicked.getInventory().getLeggings() != null) || newArmorType.equals(ArmorType.BOOTS) && (equipping ? whoClicked.getInventory().getBoots() == null : whoClicked.getInventory().getBoots() != null)){
                    ArmorEquipEvent armorEquipEvent = new ArmorEquipEvent(player, ArmorEquipEvent.EquipMethod.SHIFT_CLICK, newArmorType, equipping ? null : currentItem, equipping ? currentItem : null, oldArmor);
                    if(canEquip(uniqueId.toString(), newArmorType)){
                        Bukkit.getServer().getPluginManager().callEvent(armorEquipEvent);
                        setLastEquip(uniqueId.toString(), newArmorType);
                        if(armorEquipEvent.isCancelled()){
                            e.setCancelled(true);
                        }
                    }
                }
            }
        }else{
            if(!equipping){
                // e.getCurrentItem() == Unequip
                // e.getCursor() == Equip
                newArmorType = ArmorType.matchType(currentItem.getType() != Material.AIR ? currentItem : e.getCursor());
                ArmorEquipEvent armorEquipEvent = new ArmorEquipEvent(player, ArmorEquipEvent.EquipMethod.DRAG, newArmorType, currentItem, e.getCursor(), oldArmor);
                if(canEquip(uniqueId.toString(), newArmorType)){
                    Bukkit.getServer().getPluginManager().callEvent(armorEquipEvent);
                    setLastEquip(uniqueId.toString(), newArmorType);
                    if(armorEquipEvent.isCancelled()){
                        e.setCancelled(true);
                    }
                }
            }
        }
    }

    // No need for a check for canEquip as you can't spam equip with right click
    @EventHandler
    public void playerInteractEvent(PlayerInteractEvent e){
        if(e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK){
            ArmorType newArmorType = ArmorType.matchType(e.getItem());
            if(newArmorType != null){
                if(newArmorType.equals(ArmorType.HELMET) && e.getPlayer().getInventory().getHelmet() == null ||
                        newArmorType.equals(ArmorType.CHESTPLATE) && e.getPlayer().getInventory().getChestplate() == null ||
                        newArmorType.equals(ArmorType.LEGGINGS) && e.getPlayer().getInventory().getLeggings() == null ||
                        newArmorType.equals(ArmorType.BOOTS) && e.getPlayer().getInventory().getBoots() == null){
                    ItemStack[] oldArmor = e.getPlayer().getEquipment().getArmorContents();
                    ArmorEquipEvent armorEquipEvent = new ArmorEquipEvent(e.getPlayer(), ArmorEquipEvent.EquipMethod.HOTBAR, ArmorType.matchType(e.getItem()), null, e.getItem(), oldArmor);
                    Bukkit.getServer().getPluginManager().callEvent(armorEquipEvent);
                    if(armorEquipEvent.isCancelled()){
                        e.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventHandler
    public void dispenserFireEvent(BlockDispenseEvent e){
        if (!handleDispenser) {
            return;
        }
        ArmorType type = ArmorType.matchType(e.getItem());
        if(ArmorType.matchType(e.getItem()) != null){
            Location loc = e.getBlock().getLocation();
            for(Player p : loc.getWorld().getPlayers()){
                Location location = p.getLocation();
                if(loc.getBlockY() - location.getBlockY() >= -1 && loc.getBlockY() - location.getBlockY() <= 1){
                    if(p.getInventory().getHelmet() == null && type.equals(ArmorType.HELMET) || p.getInventory().getChestplate() == null && type.equals(ArmorType.CHESTPLATE) || p.getInventory().getLeggings() == null && type.equals(ArmorType.LEGGINGS) || p.getInventory().getBoots() == null && type.equals(ArmorType.BOOTS)){
                        org.bukkit.block.Dispenser dispenser = (org.bukkit.block.Dispenser) e.getBlock().getState();
                        org.bukkit.material.Dispenser dis = (org.bukkit.material.Dispenser) dispenser.getData();
                        BlockFace directionFacing = dis.getFacing();
                        // Someone told me not to do big if checks because it's hard to read, look at me doing it -_-
                        if(directionFacing == BlockFace.EAST && location.getBlockX() != loc.getBlockX() && location.getX() <= loc.getX() + 2.3 && location.getX() >= loc.getX() || directionFacing == BlockFace.WEST && location.getX() >= loc.getX() - 1.3 && location.getX() <= loc.getX() || directionFacing == BlockFace.SOUTH && location.getBlockZ() != loc.getBlockZ() && location.getZ() <= loc.getZ() + 2.3 && location.getZ() >= loc.getZ() || directionFacing == BlockFace.NORTH && location.getZ() >= loc.getZ() - 1.3 && location.getZ() <= loc.getZ()){
                            ItemStack[] oldArmor = p.getEquipment().getArmorContents();
                            ArmorEquipEvent armorEquipEvent = new ArmorEquipEvent(p, ArmorEquipEvent.EquipMethod.DISPENSER, ArmorType.matchType(e.getItem()), null, e.getItem(), oldArmor);
                            Bukkit.getServer().getPluginManager().callEvent(armorEquipEvent);
                            if(armorEquipEvent.isCancelled()){
                                e.setCancelled(true);
                            }
                            return;
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void itemBreakEvent(PlayerItemBreakEvent e){
        ArmorType type = ArmorType.matchType(e.getBrokenItem());
        if(type != null){
            Player p = e.getPlayer();
            ItemStack[] oldArmor = e.getPlayer().getEquipment().getArmorContents();
            ArmorEquipEvent armorEquipEvent = new ArmorEquipEvent(p, ArmorEquipEvent.EquipMethod.BROKE, type, e.getBrokenItem(), null, oldArmor);
            Bukkit.getServer().getPluginManager().callEvent(armorEquipEvent);
            if(armorEquipEvent.isCancelled()){
                ItemStack i = e.getBrokenItem().clone();
                i.setAmount(1);
                i.setDurability((short) (i.getDurability() - 1));
                PlayerInventory inventory = p.getInventory();
                if(type.equals(ArmorType.HELMET)){
                    inventory.setHelmet(i);
                }else if(type.equals(ArmorType.CHESTPLATE)){
                    inventory.setChestplate(i);
                }else if(type.equals(ArmorType.LEGGINGS)){
                    inventory.setLeggings(i);
                }else if(type.equals(ArmorType.BOOTS)){
                    inventory.setBoots(i);
                }
            }
        }
    }

    public boolean canEquip(String id, ArmorType type) {
        return type == null || lastEquip.containsKey(id) && lastEquip.get(id).containsKey(type) && System.currentTimeMillis() - lastEquip.get(id).get(type) < 500;
    }

    public void setLastEquip(String id, ArmorType armorType){
        if(armorType != null){
            if(!lastEquip.containsKey(id)){
                lastEquip.put(id, new HashMap<ArmorType, Long>());
            }
            HashMap<ArmorType, Long> data = lastEquip.get(id);
            data.put(armorType, System.currentTimeMillis());
            lastEquip.put(id, data);
        }
    }
}
