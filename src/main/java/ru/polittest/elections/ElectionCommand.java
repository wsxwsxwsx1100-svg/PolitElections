package ru.polittest.elections;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public class ElectionCommand implements CommandExecutor, TabCompleter {

    private final ElectionManager m;

    public ElectionCommand(ElectionManager m) {
        this.m = m;
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String label, String[] a) {
        if (a.length == 0) {
            s.sendMessage(Msg.info("/election status | run | vote <ник>"
                    + (s.hasPermission("politelections.admin") ? " | start | end" : "")));
            return true;
        }
        switch (a[0].toLowerCase()) {
            case "start" -> start(s);
            case "end" -> end(s);
            case "run" -> run(s);
            case "vote" -> vote(s, a);
            case "status" -> status(s);
            default -> s.sendMessage(Msg.err("Неизвестная подкоманда."));
        }
        return true;
    }

    private void start(CommandSender s) {
        if (!s.hasPermission("politelections.admin")) {
            s.sendMessage(Msg.err("Нет прав."));
            return;
        }
        if (m.isActive()) {
            s.sendMessage(Msg.err("Выборы уже идут."));
            return;
        }
        m.start();
        Bukkit.broadcast(Msg.gold("Начались выборы председателя сервера!"));
        Bukkit.broadcast(Msg.info("Мэры: /election run чтобы баллотироваться, /election vote <ник> чтобы отдать голос города."));
    }

    private void end(CommandSender s) {
        if (!s.hasPermission("politelections.admin")) {
            s.sendMessage(Msg.err("Нет прав."));
            return;
        }
        if (!m.isActive()) {
            s.sendMessage(Msg.err("Выборы сейчас не идут."));
            return;
        }
        Map<UUID, Double> tally = m.tally();
        double max = 0;
        for (double v : tally.values()) max = Math.max(max, v);
        if (max <= 0) {
            m.cancel();
            Bukkit.broadcast(Msg.gold("Выборы завершены без победителя: никто не получил голосов."));
            return;
        }
        List<UUID> leaders = new ArrayList<>();
        for (Map.Entry<UUID, Double> e : tally.entrySet()) {
            if (e.getValue() == max) leaders.add(e.getKey());
        }
        if (leaders.size() > 1) {
            s.sendMessage(Msg.err("Ничья между кандидатами. Выборы остаются открытыми: пусть города переголосуют."));
            return;
        }
        UUID winner = leaders.get(0);
        m.finish(winner);
        Bukkit.broadcast(Msg.gold("Новый председатель сервера: " + m.getChairmanName() + "! Меню: /chairman"));
    }

    private void run(CommandSender s) {
        Player p = playerOnly(s);
        if (p == null) return;
        if (!m.isActive()) {
            p.sendMessage(Msg.err("Выборы сейчас не идут."));
            return;
        }
        Resident r = TownyAPI.getInstance().getResident(p);
        if (r == null || !r.hasTown() || !r.isMayor()) {
            p.sendMessage(Msg.err("Баллотироваться могут только мэры городов."));
            return;
        }
        if (!m.addCandidate(p.getUniqueId(), p.getName())) {
            p.sendMessage(Msg.err("Ты уже в списке кандидатов."));
            return;
        }
        Bukkit.broadcast(Msg.gold(p.getName() + " выдвинул свою кандидатуру в председатели!"));
    }

    private void vote(CommandSender s, String[] a) {
        Player p = playerOnly(s);
        if (p == null) return;
        if (!m.isActive()) {
            p.sendMessage(Msg.err("Выборы сейчас не идут."));
            return;
        }
        if (a.length < 2) {
            p.sendMessage(Msg.err("Использование: /election vote <ник кандидата>"));
            return;
        }
        Resident r = TownyAPI.getInstance().getResident(p);
        if (r == null || !r.hasTown() || !r.isMayor()) {
            p.sendMessage(Msg.err("Голосовать за город может только его мэр."));
            return;
        }
        UUID cand = m.findCandidate(a[1]);
        if (cand == null) {
            p.sendMessage(Msg.err("Такого кандидата нет. Список: /election status"));
            return;
        }
        Town town = r.getTownOrNull();
        if (town == null) return;
        m.vote(town.getUUID(), cand);
        p.sendMessage(Msg.ok("Город " + town.getName() + " отдал " + (int) m.townWeight(town)
                + " голос(ов) за " + m.getCandidates().get(cand) + ". Голос можно изменить до конца выборов."));
    }

    private void status(CommandSender s) {
        if (!m.isActive()) {
            s.sendMessage(Msg.info("Выборы сейчас не идут. Председатель: " + m.getChairmanName()));
            return;
        }
        s.sendMessage(Msg.gold("Выборы идут. Кандидаты и голоса:"));
        Map<UUID, Double> tally = m.tally();
        if (tally.isEmpty()) {
            s.sendMessage(Msg.info("Пока нет кандидатов."));
            return;
        }
        for (Map.Entry<UUID, Double> e : tally.entrySet()) {
            s.sendMessage(Component.text(" • " + m.getCandidates().get(e.getKey()) + ": " + e.getValue().intValue()));
        }
    }

    private Player playerOnly(CommandSender s) {
        if (s instanceof Player p) return p;
        s.sendMessage(Msg.err("Только для игроков."));
        return null;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String label, String[] a) {
        List<String> out = new ArrayList<>();
        if (a.length == 1) {
            out.addAll(List.of("status", "run", "vote"));
            if (s.hasPermission("politelections.admin")) out.addAll(List.of("start", "end"));
        } else if (a.length == 2 && a[0].equalsIgnoreCase("vote")) {
            out.addAll(m.getCandidates().values());
        }
        String prefix = a[a.length - 1].toLowerCase();
        out.removeIf(x -> !x.toLowerCase().startsWith(prefix));
        return out;
    }
}
