package upgrade_final2.upgrade_final2;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;  // Bukkit 전체 임포트
import org.bukkit.block.Block;  // 추가된 임포트
import org.bukkit.block.data.BlockData;  // 추가된 임포트
import org.bukkit.block.data.Levelled;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRiptideEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.entity.AbstractArrow;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class TridentUpgrade implements Listener {

    private final JavaPlugin plugin;
    private final ConcurrentHashMap<UUID, Long> projectileImmunityPlayers = new ConcurrentHashMap<>();
    private final NamespacedKey shooterKey;
    private final NamespacedKey specialAbilityKey;
    private final Set<UUID> cooldownPlayers = ConcurrentHashMap.newKeySet();

    private final Map<UUID, Long> abilityCooldowns = new HashMap<>();
    private final long COOLDOWN_TIME = 30 * 1000;

    public TridentUpgrade(JavaPlugin plugin) {
        this.plugin = plugin;
        this.shooterKey = new NamespacedKey(plugin, "shooter_uuid");
        this.specialAbilityKey = new NamespacedKey(plugin, "special_ability");
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Trident trident) {
            ProjectileSource shooter = trident.getShooter();
            if (shooter instanceof Player player) {

                ItemStack tridentItem = player.getInventory().getItemInMainHand();
                if (isTrident(tridentItem.getType())) {

                    if (tridentItem.hasItemMeta()) {

                        ItemMeta meta = tridentItem.getItemMeta();
                        if (meta != null && meta.hasLore()) {
                            List<String> lore = meta.getLore();
                            if (lore != null) {
                                int level = (int) getLevelFromLore(lore);

                                // 트라이던트에 Shooter UUID 저장
                                PersistentDataContainer container = trident.getPersistentDataContainer();
                                container.set(shooterKey, PersistentDataType.STRING, player.getUniqueId().toString());

                                if (level >= 1) {
                                    if (event.getEntity() instanceof Player hitPlayer && !hitPlayer.getUniqueId().equals(player.getUniqueId())) {
                                        hitPlayer.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 1));
                                    }
                                }

                                if (level == 10) {
                                    activateProjectileImmunity(player);
                                    setUnbreakable(tridentItem, level); // 내구도 무한 호출
                                    Boolean hasSpecialAbility = meta.getPersistentDataContainer().has(specialAbilityKey, PersistentDataType.BYTE);
                                    if (hasSpecialAbility != null && hasSpecialAbility) {
                                        trident.setPickupStatus(AbstractArrow.PickupStatus.CREATIVE_ONLY);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 필드에 글로벌 플래그 추가
    private final Set<UUID> launchedTridents = ConcurrentHashMap.newKeySet();

    // ProjectileLaunchEvent 핸들러 수정
    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (event.getEntity() instanceof Trident trident) {
            ProjectileSource shooter = trident.getShooter();
            if (shooter instanceof Player player) {
                // 중복 방지: 이미 삼지창을 처리한 경우 무시
                if (launchedTridents.contains(player.getUniqueId())) {
                    return;
                }

                ItemStack tridentItem = player.getInventory().getItemInMainHand();
                if (isTrident(tridentItem.getType()) && tridentItem.hasItemMeta()) {
                    ItemMeta meta = tridentItem.getItemMeta();
                    List<String> lore = meta.getLore();

                    int level = (int) getLevelFromLore(lore);

                    if (level > 0) {
                        // 추가 삼지창 발사
                        launchAdditionalTridents(player, level, trident.getLocation(), trident.getVelocity());

                        // 중복 방지를 위해 플레이어 UUID를 추가
                        launchedTridents.add(player.getUniqueId());

                        // 일정 시간이 지나면 중복 플래그 제거
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                launchedTridents.remove(player.getUniqueId());
                            }
                        }.runTaskLater(plugin, 20L); // 1초 후 플래그 제거
                    }
                }
            }
        }
    }

    // PlayerRiptideEvent 핸들러 수정
    @EventHandler
    public void onPlayerRiptide(PlayerRiptideEvent event) {
        Player player = event.getPlayer();

        // 삼지창을 들고 있는지 확인
        ItemStack tridentItem = player.getInventory().getItemInMainHand();
        if (isTrident(tridentItem.getType()) && tridentItem.hasItemMeta()) {
            ItemMeta meta = tridentItem.getItemMeta();
            List<String> lore = meta.getLore();

            int level = (int) getLevelFromLore(lore);

            if (level > 0) {
                // 급류 효과 시 추가 삼지창 발사
                launchAdditionalTridents(player, level, player.getLocation(), player.getLocation().getDirection().multiply(2));
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {

        if (event.getEntity() instanceof Player player) {
            UUID playerId = player.getUniqueId();
            if (event.getCause() == EntityDamageEvent.DamageCause.PROJECTILE) {
                Long immunityEndTime = projectileImmunityPlayers.get(playerId);
                if (immunityEndTime != null && immunityEndTime > System.currentTimeMillis()) {

                    event.setCancelled(true);
                } else {
                    projectileImmunityPlayers.remove(playerId);

                }
            }
        }
    }

    @EventHandler
    public void onPotionSplash(PotionSplashEvent event) {
        for (LivingEntity entity : event.getAffectedEntities()) {
            if (entity instanceof Player player) {
                UUID playerId = player.getUniqueId();
                Long immunityEndTime = projectileImmunityPlayers.get(playerId);
                if (immunityEndTime != null && immunityEndTime > System.currentTimeMillis()) {

                    event.setIntensity(player, 0.0);  // Set the intensity of the potion effect to 0 for the affected player
                }
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction().toString().contains("RIGHT_CLICK")) {
            Player player = event.getPlayer();
            ItemStack itemInHand = player.getInventory().getItemInMainHand();
            if (isTrident(itemInHand.getType()) && isMaxLevelTrident(itemInHand) && !cooldownPlayers.contains(player.getUniqueId())) {
                createTemporaryWaterBlock(player);
            }
        }
    }

    private void activateProjectileImmunity(Player player) {
        UUID playerId = player.getUniqueId();

        projectileImmunityPlayers.put(playerId, System.currentTimeMillis() + 5000);

        new BukkitRunnable() {
            @Override
            public void run() {
                projectileImmunityPlayers.remove(playerId);
            }
        }.runTaskLater(plugin, 100L);
    }

    private void launchAdditionalTridents(Player player, int level, Location origin, Vector direction) {
        World world = player.getWorld();
        Random random = new Random();
        double spreadRadius = 3;  // 발사체 퍼지는 범위 감소

        for (int i = 0; i < level; i++) {
            double offsetX = (random.nextDouble() - 0.5) * spreadRadius;
            double offsetY = (random.nextDouble() - 0.5) * spreadRadius;
            double offsetZ = (random.nextDouble() - 0.5) * spreadRadius;

            Location spawnLocation = origin.clone().add(offsetX, offsetY, offsetZ);

            Trident additionalTrident = (Trident) world.spawnEntity(spawnLocation, EntityType.TRIDENT);
            additionalTrident.setShooter(player);
            additionalTrident.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);

            // 추가적인 방향 설정
            Vector newDirection = direction.clone();
            newDirection.add(new Vector(offsetX, offsetY, offsetZ).normalize().multiply(0.1));
            additionalTrident.setVelocity(newDirection);

            // 이름 제거
            additionalTrident.setCustomName(null);
            additionalTrident.setCustomNameVisible(false);
        }
    }

    private double getLevelFromLore(List<String> lore) {
        if (lore == null) {
            return 0;
        }

        for (String line : lore) {
            if (line.startsWith(ChatColor.BLUE + "추가 투사체 수:")) {
                try {
                    return Double.parseDouble(
                            ChatColor.stripColor(line)
                                    .replace("추가 투사체 수:", "")
                                    .replace("개", "")
                                    .trim()
                    );
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Lore format is incorrect: " + line);
                }
            }
        }
        return 0;
    }

    // 삼지창 강화 적용 함수
    public static void applyTridentUpgrade(JavaPlugin plugin, Player player, ItemStack trident, int level) {
        if (trident == null || !isTrident(trident.getType())) {
            return;
        }

        int successChance = calculateSuccessChance(level);
        int failChance = calculateFailChance(level);
        int downgradeChance = calculateDowngradeChance(level);
        int destroyChance = calculateDestroyChance(level);

        setItemLore(trident, level, successChance, failChance, downgradeChance, destroyChance);

        if (level == 10) {
            addSpecialAbility(trident, plugin);
            announceLegendaryTrident(plugin, player, trident);
            setUnbreakable(trident, level); // 내구도 무한 호출
        }
    }

    private static boolean isTrident(Material material) {
        return material == Material.TRIDENT;
    }

    private boolean isMaxLevelTrident(ItemStack item) {
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasLore()) {
                List<String> lore = meta.getLore();
                if (lore != null) {
                    return lore.stream().anyMatch(line -> line.contains("추가 투사체 수: 10개"));
                }
            }
        }
        return false;
    }

    private void createTemporaryWaterBlock(Player player) {
        UUID uuid = player.getUniqueId();

        // 쿨타임 적용 여부 확인
        if (abilityCooldowns.containsKey(uuid)) {
            long timeLeft = abilityCooldowns.get(uuid) - System.currentTimeMillis();
            if (timeLeft > 0) {
                int secondsLeft = (int) (timeLeft / 1000);

                player.spigot().sendMessage(
                        ChatMessageType.ACTION_BAR,
                        TextComponent.fromLegacyText(net.md_5.bungee.api.ChatColor.AQUA + "허공급류 사용 가능: " + secondsLeft + "초 후")
                );
                return;
            }
        }

        // 허공 급류 효과 (워터 블록 생성)
        AbilityWaterBlock ability = new AbilityWaterBlock(player, plugin);
        ability.startWater();

        // 액션바로 능력 활성화 표시
        player.spigot().sendMessage(
                ChatMessageType.ACTION_BAR,
                TextComponent.fromLegacyText(net.md_5.bungee.api.ChatColor.AQUA + "허공 급류 발동!")
        );

        // 쿨타임 적용
        abilityCooldowns.put(uuid, System.currentTimeMillis() + COOLDOWN_TIME);

        // 쿨타임 종료 후 액션바 표시
        new BukkitRunnable() {
            @Override
            public void run() {
                player.spigot().sendMessage(
                        ChatMessageType.ACTION_BAR,
                        TextComponent.fromLegacyText(net.md_5.bungee.api.ChatColor.GREEN + "허공급류 능력 사용 가능!")
                );

                // 쿨타임 해제
                abilityCooldowns.remove(uuid);
            }
        }.runTaskLater(plugin, COOLDOWN_TIME / 50); // 30초 후 실행
    }

    private static void setItemLore(ItemStack item, int level, int successChance, int failChance, int downgradeChance, int destroyChance) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GREEN + "성공 확률: " + successChance + "%");
            lore.add(ChatColor.YELLOW + "실패 확률: " + failChance + "%");
            lore.add(ChatColor.RED + "하락 확률: " + downgradeChance + "%");
            lore.add(ChatColor.DARK_RED + "파괴 확률: " + destroyChance + "%");
            lore.add("");
            lore.add(ChatColor.BLUE + "추가 투사체 수: " + level + "개");
            lore.add(ChatColor.DARK_GRAY + "(적중시 구속 3초간 부여)");

            if (level == 10) {
                lore.add("");
                lore.add(ChatColor.AQUA + "특수 능력: 허공급류");


                // 특수 효과를 추가로 표시합니다.
                item.setDurability((short) -1);
                item.addUnsafeEnchantment(Enchantment.DURABILITY, 10);  // 내구도 인챈트 최대 레벨로 설정
            }

            meta.setLore(lore);
            item.setItemMeta(meta);
        }
    }

    private static void addSpecialAbility(ItemStack item, JavaPlugin plugin) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "special_ability"), PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);

        }
    }

    private static void setUnbreakable(ItemStack item, int level) {
        if (level == 10) { // 10강일 때만 적용
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setUnbreakable(true); // 내구도 무한 설정
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_UNBREAKABLE); // 툴팁에서 내구도 무한 숨기기
                item.setItemMeta(meta);
            }
        }
    }

    private static void announceLegendaryTrident(JavaPlugin plugin, Player player, ItemStack item) {
        String itemName = item.getItemMeta() != null && item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : item.getType().name();
        String legendaryName = "전설의 " + itemName;

        // 서버 내 모든 플레이어에게 메시지 브로드캐스트
        String message = ChatColor.GOLD + "전설의 " + itemName + "이 탄생하였습니다!";
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(message);
        }

        // 전설의 아이템이 탄생한 것을 축하하는 소리를 모든 플레이어에게 재생
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        }

        // 아이템 이름을 전설의 xxx으로 변경
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + legendaryName);
            item.setItemMeta(meta);
        }
    }

    private static int calculateSuccessChance(int level) {
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

    private static int calculateFailChance(int level) {
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

    private static int calculateDowngradeChance(int level) {
        return switch (level) {
            case 0, 1, 2, 3, 4, 9 -> 0;
            case 5 -> 8;
            case 6 -> 10;
            case 7 -> 15;
            case 8 -> 20;
            default -> 0;
        };
    }

    private static int calculateDestroyChance(int level) {
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