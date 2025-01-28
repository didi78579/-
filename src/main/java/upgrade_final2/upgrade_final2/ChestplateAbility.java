package upgrade_final2.upgrade_final2;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Random;

public class ChestplateAbility implements Listener {

    private static final Random random = new Random();
    private static final double CHANCE = 0.07; // 10% 확률

    private final JavaPlugin plugin;

    public ChestplateAbility(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Entity entity = event.getEntity();
        Entity damager = event.getDamager();

        if (entity instanceof Player player && damager instanceof Player attacker) {
            ItemStack chestplate = player.getInventory().getChestplate();

            if (chestplate != null && chestplate.getItemMeta() != null) {
                ItemMeta meta = chestplate.getItemMeta();

                if (meta.getPersistentDataContainer().has(new NamespacedKey(plugin, "damage_negation"), PersistentDataType.BYTE)) {
                    if (random.nextDouble() < CHANCE) { // 10% 확률
                        event.setCancelled(true);
                        player.spigot().sendMessage(
                                ChatMessageType.ACTION_BAR,
                                TextComponent.fromLegacyText(ChatColor.AQUA + "공격이 무효화되었습니다."));;
                        attacker.spigot().sendMessage(
                                ChatMessageType.ACTION_BAR,
                                TextComponent.fromLegacyText(ChatColor.RED + "공격이 무효화되었습니다."));;
                    }
                }
            }
        }
    }
}