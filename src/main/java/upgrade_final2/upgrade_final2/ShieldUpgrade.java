package upgrade_final2.upgrade_final2;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ShieldUpgrade implements Listener {

    private static NamespacedKey cooldownKey;
    private static NamespacedKey abilityKey;
    private static final List<UUID> activeDash = new ArrayList<>();
    private static final long COOLDOWN_TIME = 150000L; // 150초 쿨타임 (밀리초 단위)

    private final JavaPlugin plugin; // plugin 변수 추가

    // 생성자로 JavaPlugin 전달받기
    public ShieldUpgrade(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // 초기화를 위한 메서드
    public static void initialize(JavaPlugin plugin) {
        cooldownKey = new NamespacedKey(plugin, "shield_cooldown");
        abilityKey = new NamespacedKey(plugin, "special_ability");
    }

    public void applyShieldUpgrade(Player player, ItemStack shield, int level) {
        if (shield == null || shield.getType() != Material.SHIELD) {
            player.sendMessage(ChatColor.RED + "선택한 아이템이 방패가 아닙니다.");
            return;
        }

        setShieldAttributes(shield, level);

        int successChance = calculateSuccessChance(level);
        int failChance = calculateFailChance(level);
        int downgradeChance = calculateDowngradeChance(level);
        int destroyChance = calculateDestroyChance(level);

        setItemLore(shield, level, successChance, failChance, downgradeChance, destroyChance);

        if (level == 10) {
            addSpecialAbility(shield);
            setUnbreakable(shield);
            announceLegendaryShield(shield, plugin); // plugin 변수 사용
        }

        applyShieldSpecificSpecialAbility(shield);
    }

    private void setShieldAttributes(ItemStack shield, int level) {
        ItemMeta meta = shield.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.DURABILITY, level, true);
            shield.setItemMeta(meta);
        }
    }

    private void setItemLore(ItemStack item, int level, int successChance, int failChance, int downgradeChance, int destroyChance) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();

            lore.add(ChatColor.GREEN + "성공 확률: " + successChance + "%");
            lore.add(ChatColor.YELLOW + "실패 확률: " + failChance + "%");
            lore.add(ChatColor.RED + "하락 확률: " + downgradeChance + "%");
            lore.add(ChatColor.DARK_RED + "파괴 확률: " + destroyChance + "%");
            lore.add("");

            if (level == 10) {
                lore.add(ChatColor.AQUA + "특수 능력: 방패 돌격");
            }

            meta.setLore(lore);
            item.setItemMeta(meta);
        }
    }

    private static void announceLegendaryShield(ItemStack item, JavaPlugin plugin) {
        ItemMeta meta = item.getItemMeta();
        String itemName = item.getItemMeta() != null && item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : item.getType().name();
        String legendaryName = "전설의 " + itemName;

            // 모든 플레이어에게 메시지 전송
        String message = ChatColor.GOLD + "전설의 " + itemName + "이(가) 탄생하였습니다!";
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(message);
        }

            // 플레이어를 위한 소리 재생
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f); // 웅장한 소리 재생
        }

        if (meta != null) {meta.setDisplayName(ChatColor.GOLD + legendaryName);
            item.setItemMeta(meta);
        }
    }

    private void applyShieldSpecificSpecialAbility(ItemStack shield) {
        ItemMeta meta = shield.getItemMeta();
        if (meta != null && shield.getType() == Material.SHIELD) {
            meta.getPersistentDataContainer().set(cooldownKey, PersistentDataType.LONG, System.currentTimeMillis() - COOLDOWN_TIME); // 초기 쿨다운 설정
            shield.setItemMeta(meta);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;

        Player player = (Player) event.getDamager();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || item.getType() != Material.SHIELD) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.getPersistentDataContainer().has(cooldownKey, PersistentDataType.LONG)) return;

        // 방패가 도끼에 맞더라도 무력화되지 않도록 하기
        if (player.isBlocking()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || item.getType() != Material.SHIELD) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        // 방패 이름 가져오기 (디스플레이 이름이 없으면 기본 이름 사용)
        String shieldName = meta.hasDisplayName() ? meta.getDisplayName() : ChatColor.WHITE + item.getType().name();

        // 좌클릭 (LEFT_CLICK_AIR 또는 LEFT_CLICK_BLOCK) 시 특수 능력 발동
        if (event.getAction() != Action.LEFT_CLICK_AIR && event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }

        if (!meta.getPersistentDataContainer().has(abilityKey, PersistentDataType.BYTE)) return;

        long lastUsed = meta.getPersistentDataContainer().getOrDefault(cooldownKey, PersistentDataType.LONG, 0L);
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastUsed < COOLDOWN_TIME) { // 쿨타임 적용
            // 남은 쿨타임 표시
            long remainingTime = COOLDOWN_TIME - (currentTime - lastUsed);
            int remainingSeconds = (int) (remainingTime / 1000);
            player.spigot().sendMessage(
                    ChatMessageType.ACTION_BAR,
                    TextComponent.fromLegacyText(ChatColor.RED + shieldName + " 사용 가능: " + remainingSeconds + "초 후")
            );
            return;
        }

        // 능력 사용 시간 기록
        meta.getPersistentDataContainer().set(cooldownKey, PersistentDataType.LONG, currentTime);

        // 대쉬 로직
        Vector dashDirection = player.getLocation().getDirection().multiply(5); // 5 블록 거리
        player.setVelocity(dashDirection);
        activeDash.add(player.getUniqueId());

        // 효과: 연기 파티클, 사운드
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 20, 0.5, 0.5, 0.5, 0.1);
        player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.5f);

        // 능력 발동 표시
        player.spigot().sendMessage(
                ChatMessageType.ACTION_BAR,
                TextComponent.fromLegacyText(ChatColor.GREEN + shieldName + " 능력이 발동되었습니다!")
        );

        item.setItemMeta(meta);

        // 대쉬 후 효과
        new BukkitRunnable() {
            @Override
            public void run() {
                activeDash.remove(player.getUniqueId());
                Location playerLocation = player.getLocation();
                List<Entity> nearbyEntities = player.getNearbyEntities(2, 1, 2);

                // 주변 적 공중 띄우기
                for (Entity entity : nearbyEntities) {
                    if (entity instanceof LivingEntity && entity != player) {
                        LivingEntity livingEntity = (LivingEntity) entity;
                        livingEntity.setVelocity(new Vector(0, 1, 0)); // 위로 띄우기
                    }
                }

                player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
                player.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, player.getLocation(), 1);
            }
        }.runTaskLater(JavaPlugin.getProvidingPlugin(getClass()), 6L);

        // 쿨타임 해제 메시지
        new BukkitRunnable() {
            @Override
            public void run() {
                // 쿨타임이 끝났을 때 개인에게 메시지 전송
                player.sendMessage(ChatColor.GREEN + shieldName + " 능력이 다시 사용 가능합니다!");

                // 액션바를 통해 재사용 가능 알림
                player.spigot().sendMessage(
                        ChatMessageType.ACTION_BAR,
                        TextComponent.fromLegacyText(ChatColor.GREEN + shieldName + " 사용 가능!")
                );
            }
        }.runTaskLater(JavaPlugin.getProvidingPlugin(getClass()), COOLDOWN_TIME / 50); // 150초 후 실행
    }

    private void addSpecialAbility(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(abilityKey, PersistentDataType.BYTE, (byte) 1);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
    }

    private void setUnbreakable(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            item.setItemMeta(meta);
        }
    }

    private int calculateSuccessChance(int level) {
        return switch (level) {
            case 0 -> 90;
            case 1 -> 80;
            case 2 -> 70;
            case 3 -> 60;
            case 4 -> 50;
            case 5, 6, 7, 8, 9 -> 50;
            default -> 0;
        };
    }

    private int calculateFailChance(int level) {
        return switch (level) {
            case 0 -> 10;
            case 1 -> 20;
            case 2 -> 30;
            case 3 -> 40;
            case 4 -> 50;
            case 5 -> 40;
            case 6 -> 35;
            case 7 -> 25;
            case 8 -> 10;
            case 9 -> 0;
            default -> 0;
        };
    }

    private int calculateDowngradeChance(int level) {
        return switch (level) {
            case 0, 1, 2, 3, 4, 9 -> 0;
            case 5 -> 8;
            case 6 -> 10;
            case 7 -> 15;
            case 8 -> 20;
            default -> 0;
        };
    }

    private int calculateDestroyChance(int level) {
        return switch (level) {
            case 0, 1, 2, 3, 4 -> 0;
            case 5 -> 2;
            case 6 -> 5;
            case 7 -> 10;
            case 8 -> 20;
            case 9 -> 50;
            default -> 0;
        };
    }
}