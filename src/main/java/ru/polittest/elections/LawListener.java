package ru.polittest.elections;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;

/** Применяет принятые председателем законы ко всем игрокам. */
public class LawListener implements Listener {

    private final ElectionManager m;

    public LawListener(ElectionManager m) {
        this.m = m;
    }

    private void deny(Player p, Law law) {
        p.sendActionBar(Component.text("Закон председателя: " + law.title, NamedTextColor.RED));
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = e.getItem();
        if (item == null) return;
        Law law = switch (item.getType()) {
            case SHIELD -> Law.SHIELDS;
            case ENDER_PEARL -> Law.ENDER_PEARLS;
            case END_CRYSTAL -> Law.END_CRYSTALS;
            default -> null;
        };
        if (law != null && m.isLawActive(law)) {
            e.setUseItemInHand(Event.Result.DENY);
            if (item.getType() == Material.END_CRYSTAL) e.setUseInteractedBlock(Event.Result.DENY);
            deny(e.getPlayer(), law);
        }
    }

    @EventHandler
    public void onLaunch(ProjectileLaunchEvent e) {
        if (e.getEntity() instanceof EnderPearl pearl && pearl.getShooter() instanceof Player p
                && m.isLawActive(Law.ENDER_PEARLS)) {
            e.setCancelled(true);
            deny(p, Law.ENDER_PEARLS);
        }
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent e) {
        Material t = e.getItem().getType();
        if ((t == Material.GOLDEN_APPLE || t == Material.ENCHANTED_GOLDEN_APPLE)
                && m.isLawActive(Law.GOLDEN_APPLES)) {
            e.setCancelled(true);
            deny(e.getPlayer(), Law.GOLDEN_APPLES);
        }
    }

    @EventHandler
    public void onResurrect(EntityResurrectEvent e) {
        if (e.getEntity() instanceof Player p && m.isLawActive(Law.TOTEMS)) {
            e.setCancelled(true);
            deny(p, Law.TOTEMS);
        }
    }

    @EventHandler
    public void onCrystalDamage(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof EnderCrystal && e.getEntity() instanceof Player
                && m.isLawActive(Law.END_CRYSTALS)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onGlide(EntityToggleGlideEvent e) {
        if (e.getEntity() instanceof Player p && e.isGliding() && m.isLawActive(Law.ELYTRA)) {
            e.setCancelled(true);
            deny(p, Law.ELYTRA);
        }
    }
}
