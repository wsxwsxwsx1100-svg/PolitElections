package ru.polittest.elections;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class Msg {
    private Msg() {}

    public static Component info(String s) {
        return Component.text(s, NamedTextColor.AQUA);
    }

    public static Component ok(String s) {
        return Component.text(s, NamedTextColor.GREEN);
    }

    public static Component err(String s) {
        return Component.text(s, NamedTextColor.RED);
    }

    public static Component gold(String s) {
        return Component.text(s, NamedTextColor.GOLD);
    }
}
