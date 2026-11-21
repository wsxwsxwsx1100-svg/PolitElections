package ru.polittest.elections;

import org.bukkit.Material;

/** Законы меты боёв, которые может принять председатель. */
public enum Law {
    SHIELDS("Без щитов", Material.SHIELD),
    ENDER_PEARLS("Без эндер-жемчуга", Material.ENDER_PEARL),
    GOLDEN_APPLES("Без золотых яблок", Material.GOLDEN_APPLE),
    TOTEMS("Без тотемов бессмертия", Material.TOTEM_OF_UNDYING),
    END_CRYSTALS("Без кристаллов Энда", Material.END_CRYSTAL),
    ELYTRA("Без элитр", Material.ELYTRA);

    public final String title;
    public final Material icon;

    Law(String title, Material icon) {
        this.title = title;
        this.icon = icon;
    }
}
