package ru.polittest.elections;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class PolitElections extends JavaPlugin {

    private ElectionManager manager;

    @Override
    public void onEnable() {
        if (getServer().getPluginManager().getPlugin("Towny") == null) {
            getLogger().severe("Towny не найден, плагин выключается.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        manager = new ElectionManager(this);
        manager.load();

        ElectionCommand election = new ElectionCommand(manager);
        getCommand("election").setExecutor(election);
        getCommand("election").setTabCompleter(election);

        ChairmanMenu menu = new ChairmanMenu(manager);
        getCommand("chairman").setExecutor(menu);

        getServer().getPluginManager().registerEvents(menu, this);
        getServer().getPluginManager().registerEvents(new LawListener(manager), this);

        // Запасной вариант против щитов: если закон принят, сбрасываем поднятый щит.
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (!manager.isLawActive(Law.SHIELDS)) return;
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.isBlocking()) p.clearActiveItem();
            }
        }, 2L, 2L);
    }

    @Override
    public void onDisable() {
        if (manager != null) manager.save();
    }
}
