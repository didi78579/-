package upgrade_final2.upgrade_final2;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HelmetAbility implements Listener {
    private final JavaPlugin plugin;
    private final Map<UUID, BukkitTask> playerTasks = new HashMap<>();

    public HelmetAbility(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        schedulePotionEffects(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        removePotionEffects(event.getPlayer());
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        schedulePotionEffects(event.getPlayer());
    }

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        schedulePotionEffects(event.getPlayer());
    }

    private void removePotionEffects(Player player) {
        player.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);
        player.removePotionEffect(PotionEffectType.REGENERATION);

        UUID playerUUID = player.getUniqueId();
        if (playerTasks.containsKey(playerUUID)) {
            playerTasks.get(playerUUID).cancel();
            playerTasks.remove(playerUUID);
        }
    }

    private void schedulePotionEffects(Player player) {
        UUID playerUUID = player.getUniqueId();
        if (playerTasks.containsKey(playerUUID)) {
            playerTasks.get(playerUUID).cancel();
        }
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> applyPotionEffects(player), 0, 200); // 200 ticks = 10 seconds
        playerTasks.put(playerUUID, task);
    }

    private void applyPotionEffects(Player player) {
        ItemStack helmet = player.getInventory().getHelmet();

        if (helmet != null && helmet.hasItemMeta() &&
                helmet.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(plugin, "regeneration"), PersistentDataType.BYTE)
        ) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 220, 0, true, false));
        } else {
            removePotionEffects(player); // 헬멧을 벗으면 버프 제거
        }
    }
}