package ru.polittest.elections;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

/** Хранит состояние выборов, председателя, законов и напряжённости. */
public class ElectionManager {

    public static final int MAX_LAWS = 2;

    public enum Tension {
        UP("Повышена"),
        SAME("Без изменений"),
        DOWN("Понижена");

        public final String title;

        Tension(String title) {
            this.title = title;
        }
    }

    public enum ToggleResult { ENABLED, DISABLED, LIMIT }

    private final PolitElections plugin;
    private final File file;

    private boolean active;
    private final Map<UUID, String> candidates = new LinkedHashMap<>(); // игрок -> ник
    private final Map<UUID, UUID> votes = new HashMap<>();              // город -> кандидат
    private UUID chairman;
    private String chairmanName;
    private final EnumSet<Law> laws = EnumSet.noneOf(Law.class);
    private Tension tension = Tension.SAME;

    public ElectionManager(PolitElections plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
    }

    /** Вес голоса города. Позже сюда можно добавить здания и монеты. */
    public double townWeight(Town town) {
        return town.getNumResidents();
    }

    // ---------- выборы ----------

    public boolean isActive() {
        return active;
    }

    public void start() {
        active = true;
        candidates.clear();
        votes.clear();
        save();
    }

    /** Завершить выборы без победителя. */
    public void cancel() {
        active = false;
        candidates.clear();
        votes.clear();
        save();
    }

    /** Завершить выборы с победителем: законы и напряжённость сбрасываются. */
    public void finish(UUID winner) {
        chairman = winner;
        chairmanName = candidates.get(winner);
        laws.clear();
        tension = Tension.SAME;
        cancel();
    }

    public boolean addCandidate(UUID player, String name) {
        if (candidates.containsKey(player)) return false;
        candidates.put(player, name);
        save();
        return true;
    }

    public Map<UUID, String> getCandidates() {
        return candidates;
    }

    public UUID findCandidate(String name) {
        for (Map.Entry<UUID, String> e : candidates.entrySet()) {
            if (e.getValue().equalsIgnoreCase(name)) return e.getKey();
        }
        return null;
    }

    public void vote(UUID town, UUID candidate) {
        votes.put(town, candidate);
        save();
    }

    public Map<UUID, Double> tally() {
        Map<UUID, Double> result = new LinkedHashMap<>();
        for (UUID c : candidates.keySet()) result.put(c, 0.0);
        for (Map.Entry<UUID, UUID> v : votes.entrySet()) {
            Town town = TownyAPI.getInstance().getTown(v.getKey());
            if (town == null || !result.containsKey(v.getValue())) continue;
            result.merge(v.getValue(), townWeight(town), Double::sum);
        }
        return result;
    }

    // ---------- председатель ----------

    public UUID getChairman() {
        return chairman;
    }

    public String getChairmanName() {
        return chairmanName == null ? "—" : chairmanName;
    }

    public boolean isChairman(UUID id) {
        return chairman != null && chairman.equals(id);
    }

    public Tension getTension() {
        return tension;
    }

    public void setTension(Tension t) {
        tension = t; // пока ни на что не влияет
        save();
    }

    public boolean isLawActive(Law law) {
        return laws.contains(law);
    }

    public Set<Law> getLaws() {
        return laws;
    }

    public ToggleResult toggleLaw(Law law) {
        if (laws.remove(law)) {
            save();
            return ToggleResult.DISABLED;
        }
        if (laws.size() >= MAX_LAWS) return ToggleResult.LIMIT;
        laws.add(law);
        save();
        return ToggleResult.ENABLED;
    }

    // ---------- сохранение ----------

    public void load() {
        if (!file.exists()) return;
        YamlConfiguration d = YamlConfiguration.loadConfiguration(file);
        active = d.getBoolean("active");
        String ch = d.getString("chairman");
        chairman = ch == null ? null : UUID.fromString(ch);
        chairmanName = d.getString("chairman-name");
        try {
            tension = Tension.valueOf(d.getString("tension", "SAME"));
        } catch (IllegalArgumentException ignored) {
            tension = Tension.SAME;
        }
        for (String s : d.getStringList("laws")) {
            try {
                laws.add(Law.valueOf(s));
            } catch (IllegalArgumentException ignored) {
            }
        }
        ConfigurationSection cs = d.getConfigurationSection("candidates");
        if (cs != null) {
            for (String k : cs.getKeys(false)) candidates.put(UUID.fromString(k), cs.getString(k));
        }
        ConfigurationSection vs = d.getConfigurationSection("votes");
        if (vs != null) {
            for (String k : vs.getKeys(false)) votes.put(UUID.fromString(k), UUID.fromString(vs.getString(k)));
        }
    }

    public void save() {
        YamlConfiguration d = new YamlConfiguration();
        d.set("active", active);
        d.set("chairman", chairman == null ? null : chairman.toString());
        d.set("chairman-name", chairmanName);
        d.set("tension", tension.name());
        List<String> names = new ArrayList<>();
        for (Law l : laws) names.add(l.name());
        d.set("laws", names);
        for (Map.Entry<UUID, String> e : candidates.entrySet()) d.set("candidates." + e.getKey(), e.getValue());
        for (Map.Entry<UUID, UUID> e : votes.entrySet()) d.set("votes." + e.getKey(), e.getValue().toString());
        try {
            plugin.getDataFolder().mkdirs();
            d.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("Не удалось сохранить data.yml: " + ex.getMessage());
        }
    }
}
