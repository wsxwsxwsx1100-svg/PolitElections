package ru.polittest.elections;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/** Меню председателя: напряжённость (пока заглушка) и законы меты боёв. */
public class ChairmanMenu implements CommandExecutor, Listener {

    private static final int[] TENSION_SLOTS = {10, 11, 12};
    private static final ElectionManager.Tension[] TENSIONS = ElectionManager.Tension.values();
    private static final int LAW_FIRST_SLOT = 19; // 6 законов: слоты 19-24

    private final ElectionManager m;

    public ChairmanMenu(ElectionManager m) {
        this.m = m;
    }

    private static class MenuHolder implements InventoryHolder {
        Inventory inv;

        @Override
        public Inventory getInventory() {
            return inv;
        }
    }

    private boolean canUse(Player p) {
        return m.isChairman(p.getUniqueId()) || p.hasPermission("politelections.admin");
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String label, String[] a) {
        if (!(s instanceof Player p)) {
            s.sendMessage(Msg.err("Только для игроков."));
            return true;
        }
        if (!canUse(p)) {
            p.sendMessage(Msg.err("Меню доступно только председателю сервера."));
            return true;
        }
        MenuHolder holder = new MenuHolder();
        Inventory inv = Bukkit.createInventory(holder, 27, Component.text("Меню председателя"));
        holder.inv = inv;
        render(inv);
        p.openInventory(inv);
        return true;
    }

    private void render(Inventory inv) {
        inv.clear();
        // Напряжённость
        Material[] mats = {Material.RED_CONCRETE, Material.YELLOW_CONCRETE, Material.LIME_CONCRETE};
        String[] names = {"Повысить напряжённость", "Оставить напряжённость", "Понизить напряжённость"};
        NamedTextColor[] colors = {NamedTextColor.RED, NamedTextColor.YELLOW, NamedTextColor.GREEN};
        for (int i = 0; i < TENSIONS.length; i++) {
            boolean current = m.getTension() == TENSIONS[i];
            inv.setItem(TENSION_SLOTS[i], item(mats[i], names[i], colors[i], current,
                    "Сейчас: " + m.getTension().title,
                    "Пока что кнопка ни на что не влияет."));
        }
        // Законы
        Law[] laws = Law.values();
        for (int i = 0; i < laws.length; i++) {
            boolean on = m.isLawActive(laws[i]);
            inv.setItem(LAW_FIRST_SLOT + i, item(laws[i].icon, laws[i].title,
                    on ? NamedTextColor.GREEN : NamedTextColor.GRAY, on,
                    "Статус: " + (on ? "ПРИНЯТ" : "не принят"),
                    "Принято законов: " + m.getLaws().size() + "/" + ElectionManager.MAX_LAWS,
                    "Клик, чтобы принять или отменить."));
        }
    }

    private ItemStack item(Material mat, String name, NamedTextColor color, boolean glow, String... lore) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(Component.text(name, color).decoration(TextDecoration.ITALIC, false));
        List<Component> l = new ArrayList<>();
        for (String s : lore) {
            l.add(Component.text(s, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(l);
        if (glow) meta.setEnchantmentGlintOverride(true);
        it.setItemMeta(meta);
        return it;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof MenuHolder)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (e.getClickedInventory() == null || e.getClickedInventory() != e.getInventory()) return;
        if (!canUse(p)) {
            p.closeInventory();
            return;
        }
        int slot = e.getSlot();

        for (int i = 0; i < TENSION_SLOTS.length; i++) {
            if (slot == TENSION_SLOTS[i]) {
                m.setTension(TENSIONS[i]);
                p.sendMessage(Msg.info("Напряжённость: " + TENSIONS[i].title + " (пока ни на что не влияет)."));
                render(e.getInventory());
                return;
            }
        }

        Law[] laws = Law.values();
        int idx = slot - LAW_FIRST_SLOT;
        if (idx >= 0 && idx < laws.length) {
            Law law = laws[idx];
            switch (m.toggleLaw(law)) {
                case ENABLED -> Bukkit.broadcast(Msg.gold("Председатель " + p.getName()
                        + " принял закон: " + law.title + "."));
                case DISABLED -> Bukkit.broadcast(Msg.gold("Председатель " + p.getName()
                        + " отменил закон: " + law.title + "."));
                case LIMIT -> p.sendMessage(Msg.err("Можно принять не больше "
                        + ElectionManager.MAX_LAWS + " законов. Сначала отмени один."));
            }
            render(e.getInventory());
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof MenuHolder) e.setCancelled(true);
    }
}
