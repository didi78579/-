package upgrade_final2.upgrade_final2;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class ToolUpgrade implements Listener {

    private JavaPlugin plugin;

    private final Map<UUID, Long> interactCooldown = new HashMap<>();

    public ToolUpgrade(JavaPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void initializeSpecialAbilities() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && item.hasItemMeta()) {
                    applyToolSpecificSpecialAbility(item);
                }
            }
        }
    }

    public void applyToolUpgrade(Player player, ItemStack tool, int level) {
        if (tool == null || !UpgradableItems.isTool(tool.getType())) {
            player.sendMessage(ChatColor.RED + "선택한 아이템이 도구가 아닙니다.");
            return;
        }

        int successChance = calculateSuccessChance(level);
        int failChance = calculateFailChance(level);
        int downgradeChance = calculateDowngradeChance(level);
        int destroyChance = calculateDestroyChance(level);

        Map<Enchantment, Integer> existingEnchants = tool.getEnchantments();

        setItemAttributes(tool, level);
        setItemLore(tool, level, successChance, failChance, downgradeChance, destroyChance);

        if (level == 10) {
            addSpecialAbility(tool);
            setUnbreakable(tool);
            announceLegendaryTool(player, tool);
        }

        applyToolSpecificSpecialAbility(tool);
    }

    private void setItemAttributes(ItemStack item, int level) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            // 내구성과 효율 레벨 계산
            int durabilityLevel = (level + 1) / 2; // 레벨에 따라 내구성이 1, 1, 2, 2 식으로 증가
            int efficiencyLevel = level / 2;       // 레벨에 따라 효율이 0, 1, 1, 2 식으로 증가

            // 내구성 추가
            meta.addEnchant(Enchantment.DURABILITY, Math.max(durabilityLevel, meta.getEnchantLevel(Enchantment.DURABILITY)), true);
            // 효율 추가
            if(efficiencyLevel!=0){meta.addEnchant(Enchantment.DIG_SPEED, Math.max(efficiencyLevel, meta.getEnchantLevel(Enchantment.DIG_SPEED)), true);}

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
                if (UpgradableItems.isPickaxe(item.getType())) {
                    lore.add(ChatColor.AQUA + "특수 능력: 그래플링(쿨타임 120초)");
                } else if (UpgradableItems.isAxe(item.getType())) {
                    lore.add(ChatColor.AQUA + "특수 능력: 패륜(번개, 방패 무력화(30초), 이펙트 제거/쿨타임 60초)");
                } else if (UpgradableItems.isHoe(item.getType())) {
                    lore.add(ChatColor.AQUA + "특수 능력: 흡혈(상대의 전체체력의 40%회복|회복량의 50%피해/쿨타임 90초)");
                } else if (UpgradableItems.isShovel(item.getType())) {
                    lore.add(ChatColor.AQUA + "특수 능력: 기절(지속시간 1.5초/쿨타임 120초)");
                } else if (UpgradableItems.isFISHING_ROD(item.getType())) {
                    lore.add(ChatColor.AQUA + "특수 능력: 그랩(쿨타임 60초)");
                }

            }

            meta.setLore(lore);
            item.setItemMeta(meta);
        }
    }

    private void addSpecialAbility(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "special_ability"), PersistentDataType.BYTE, (byte) 1);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
    }

    private void announceLegendaryTool(Player player, ItemStack item) {
        String itemName = item.getItemMeta() != null && item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : item.getType().name();
        String legendaryName = "전설의 " + itemName;

        String message = ChatColor.GOLD + "전설의 " + itemName + "이 탄생하였습니다!";
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(message);
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + legendaryName);
            item.setItemMeta(meta);
        }
    }

    private void applyToolSpecificSpecialAbility(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            Material type = item.getType();
            NamespacedKey cooldownKey = new NamespacedKey(plugin, "cooldown");
            long cooldownTime = 30000; // 기본 쿨다운 시간

            if (UpgradableItems.isPickaxe(type)) {
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "special_grappling_ability"), PersistentDataType.BYTE, (byte) 1);
                cooldownTime = 30000; // 쿨타임 30초
            } else if (UpgradableItems.isHoe(type)) {
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "special_hoe_ability"), PersistentDataType.BYTE, (byte) 1);
                cooldownTime = 90000; // 쿨타임 180초
            } else if (UpgradableItems.isAxe(type)) {
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "special_axe_ability"), PersistentDataType.BYTE, (byte) 1);
                cooldownTime = 60000; // 쿨타임 60초
            } else if (UpgradableItems.isShovel(type)) {
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "special_shovel_ability"), PersistentDataType.BYTE, (byte) 1);
                cooldownTime = 120000; // 쿨타임 120초
            } else if (UpgradableItems.isFISHING_ROD(type)) {
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "special_fishing_rod_ability"), PersistentDataType.BYTE, (byte) 1);
                cooldownTime = 60000; // 낚싯대 쿨타임 60초
            }

            meta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "grappling_charges"), PersistentDataType.INTEGER, 4
            );

            meta.getPersistentDataContainer().set(cooldownKey, PersistentDataType.LONG, System.currentTimeMillis() - cooldownTime);
            item.setItemMeta(meta);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        Player player = (Player) event.getDamager();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.getPersistentDataContainer().has(new NamespacedKey(plugin, "special_ability"), PersistentDataType.BYTE)) return;

        Material type = item.getType();

        long cooldownTime = 0;

        if (UpgradableItems.isHoe(type)) {
            cooldownTime = 90000; // 괭이 쿨타임 180초
        } else if (UpgradableItems.isAxe(type)) {
            cooldownTime = 60000; // 도끼 쿨타임 60초
        } else if (UpgradableItems.isShovel(type)) {
            cooldownTime = 120000; // 삽 쿨타임 120초
        }

        long lastUsed = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "cooldown"), PersistentDataType.LONG);
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUsed < cooldownTime) return;

        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "cooldown"), PersistentDataType.LONG, currentTime);
        item.setItemMeta(meta);

        player.setCooldown(item.getType(), (int) (cooldownTime / 50));

        Entity target = event.getEntity();

        if (UpgradableItems.isHoe(type)) {
            double healthRestored = player.getMaxHealth() * 0.2;
            player.setHealth(Math.min(player.getHealth() + healthRestored, player.getMaxHealth()));
            if (target instanceof LivingEntity) {
                LivingEntity livingTarget = (LivingEntity) target;
                double damage = healthRestored * 0.5;

                double newHealth = livingTarget.getHealth() - damage;
                newHealth = Math.max(0.0, newHealth);
                livingTarget.setHealth(newHealth);
                // 모든 플레이어가 소리를 들을 수 있도록 설정
                target.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_DRINK, 1.0f, 1.0f);
            }
        } else if (UpgradableItems.isAxe(type)) {
            // 번개 효과 추가
            target.getWorld().strikeLightning(target.getLocation());

            if (target instanceof Player) {
                Player targetPlayer = (Player) target;

                disableAllShields(targetPlayer);

                // 모든 이펙트 클리어
                targetPlayer.getActivePotionEffects().forEach(effect -> targetPlayer.removePotionEffect(effect.getType()));
            }

            // 모든 대상에 효과 클리어
            if (target instanceof LivingEntity) {
                LivingEntity livingTarget = (LivingEntity) target;
                livingTarget.getActivePotionEffects().forEach(effect -> livingTarget.removePotionEffect(effect.getType()));
            }
        } else if (UpgradableItems.isShovel(type)) {
            if (target instanceof LivingEntity) {
                ((LivingEntity) target).addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 30, 4));
                ((LivingEntity) target).addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 30, 4));
                // 모든 플레이어가 소리를 들을 수 있도록 설정
                target.getWorld().playSound(target.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);

                if (target instanceof Player) {
                    Player targetPlayer = (Player) target;
                    new BukkitRunnable() {
                        private final float initialYaw = targetPlayer.getLocation().getYaw();
                        private final float initialPitch = targetPlayer.getLocation().getPitch();
                        private int ticks = 0;

                        @Override
                        public void run() {
                            if (ticks++ > 30) {
                                this.cancel();
                                return;
                            }
                            Location loc = targetPlayer.getLocation();
                            loc.setYaw(initialYaw);
                            loc.setPitch(initialPitch);
                            targetPlayer.teleport(loc);
                        }
                    }.runTaskTimer(plugin, 0, 1);
                }
            }
        }
    }

    // 전역적으로 10강 곡괭이의 쿨다운 상태를 공유
    private final Map<String, Long> sharedCooldownMap = new HashMap<>();
    private final String SHARED_KEY = "10_level_pickaxe_cooldown"; // 10강 곡괭이 전역 키
    private final int MAX_CHARGES = 4; // 최대 충전량
    private final long COOLDOWN_TIME = 120000; // 120초 쿨타임 (120,000ms)

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // UUID를 String으로 변환하여 사용
        sharedCooldownMap.put(playerId.toString(), 0L); // 그래플링 쿨타임 초기화

        // 아이템 초기화 로직 (위에서 설명한 그래플링 충전 초기화 로직)
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item != null && UpgradableItems.isPickaxe(item.getType())) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                NamespacedKey chargesKey = new NamespacedKey(plugin, "grappling_charges");
                meta.getPersistentDataContainer().set(chargesKey, PersistentDataType.INTEGER, MAX_CHARGES);
                item.setItemMeta(meta);
                updateActionBar(player, MAX_CHARGES);;
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();

        // 중복 처리 방지 (200ms 이내 반복 클릭 차단)
        if (interactCooldown.containsKey(playerId) && currentTime - interactCooldown.get(playerId) < 200) {
            return;
        }
        interactCooldown.put(playerId, currentTime); // 인터랙션 갱신

        ItemStack item = player.getInventory().getItemInMainHand();

        // 곡괭이 확인
        if (item == null || !UpgradableItems.isPickaxe(item.getType())) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        // 로어 확인 (특수 능력 포함 여부)
        List<String> lore = meta.getLore();
        if (lore == null || lore.stream().noneMatch(line -> line.contains("특수 능력: 그래플링(쿨타임 120초)"))) {
            return;
        }

        // PersistentDataContainer에서 현재 충전량 확인
        NamespacedKey chargesKey = new NamespacedKey(plugin, "grappling_charges");
        int charges = meta.getPersistentDataContainer().getOrDefault(chargesKey, PersistentDataType.INTEGER, MAX_CHARGES);

        // 전역 쿨타임 확인
        long lastSharedCooldown = sharedCooldownMap.getOrDefault(SHARED_KEY, 0L);
        if (charges == 0 && (currentTime - lastSharedCooldown < COOLDOWN_TIME)) {
            long timeLeft = (COOLDOWN_TIME - (currentTime - lastSharedCooldown)) / 1000;
            return;
        }

        // 능력 발동 처리 (우클릭 동작 확인)
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true); // 이벤트 중복 방지
            if (charges > 0) {
                performGrappling(player); // 그래플링 능력 실행
                charges--;

                // 충전량 업데이트
                meta.getPersistentDataContainer().set(chargesKey, PersistentDataType.INTEGER, charges);
                item.setItemMeta(meta); // 업데이트된 ItemMeta 반영

                updateActionBar(player, charges); // 액션바 업데이트

                // 충전량이 0일 때 전역 쿨다운 시작
                if (charges == 0) {
                    // 남은 전역 쿨타임 확인
                    startCooldown(player, meta, chargesKey, item);
                    // 쿨타임이 다 지났다면 충전량 복구 로직 실행
                    meta.getPersistentDataContainer().set(chargesKey, PersistentDataType.INTEGER, charges);
                    item.setItemMeta(meta);
                    updateActionBar(player, charges);
                }
            }
        }
    }

    private void startCooldown(Player player, ItemMeta meta, NamespacedKey chargesKey, ItemStack item) {
        long currentTime = System.currentTimeMillis();
        sharedCooldownMap.put(SHARED_KEY, currentTime); // 전역 쿨다운 기록
        player.setCooldown(item.getType(), (int) (COOLDOWN_TIME / 50)); // 클라이언트 쿨타임 동기화

        // 복구 스케줄 예약
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try {
                // 충전량 복구 이전에 null 체크
                if (meta != null) {
                    meta.getPersistentDataContainer().set(chargesKey, PersistentDataType.INTEGER, MAX_CHARGES);
                    item.setItemMeta(meta);
                    updateActionBar(player, MAX_CHARGES);
                }
            } catch (Exception ex) {
                player.sendMessage(ChatColor.RED + "그래플링 충전 중 오류가 발생했습니다.");
                ex.printStackTrace();
            }
        }, COOLDOWN_TIME / 50);
    }

    private void updateActionBar(Player player, int charges) {
        StringBuilder actionBar = new StringBuilder();

        // 남은 충전량 ●로 표시
        for (int i = 0; i < charges; i++) {
            actionBar.append(ChatColor.GREEN).append("●");
        }

        // 소진된 충전량 ○로 표시
        for (int i = charges; i < MAX_CHARGES; i++) {
            actionBar.append(ChatColor.GRAY).append("○");
        }

        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(actionBar.toString()));
    }

    private void performGrappling(Player player) {
        Location eyeLocation = player.getEyeLocation();
        Vector direction = eyeLocation.getDirection().normalize(); // 플레이어가 바라보는 방향
        double maxDistance = 30.0; // 그래플링 최대 거리

        // 유효한 타겟 위치 확인
        final Location targetLocation = findTargetLocation(eyeLocation, direction, maxDistance);
        if (targetLocation == null) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    TextComponent.fromLegacyText(ChatColor.RED + "그래플링 실패! 경로 상에 장애물이 없습니다.")
            );
            return;
        }

        // 그래플링 라인 생성 (파티클 효과)
        spawnGrapplingLine(player, eyeLocation, targetLocation);

        // 타겟 방향으로 이동
        Vector velocity = targetLocation.toVector().subtract(player.getLocation().toVector()).normalize().multiply(2.0);
        player.setVelocity(velocity);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.2f);
    }

    private Location findTargetLocation(Location start, Vector direction, double maxDistance) {
        for (double i = 1; i <= maxDistance; i++) {
            Location checkLocation = start.clone().add(direction.clone().multiply(i));
            if (checkLocation.getBlock().getType().isSolid()) {
                return checkLocation;
            }
        }
        return null; // 유효한 타겟 위치 없음
    }

    private void spawnGrapplingLine(Player player, Location start, Location end) {
        Vector direction = end.toVector().subtract(start.toVector()).normalize();
        double distance = start.distance(end);

        // 파티클 라인 생성
        for (double i = 0; i <= distance; i += 0.5) {
            Location particleLocation = start.clone().add(direction.clone().multiply(i));
            player.spawnParticle(
                    org.bukkit.Particle.REDSTONE,
                    particleLocation,
                    1,
                    new org.bukkit.Particle.DustOptions(org.bukkit.Color.WHITE, 1)
            );
        }
    }




    @EventHandler
    public void onFishingRodUse(PlayerFishEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        // 아이템 체크
        if (item == null || !UpgradableItems.isFISHING_ROD(item.getType())) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.getPersistentDataContainer().has(new NamespacedKey(plugin, "special_fishing_rod_ability"), PersistentDataType.BYTE))
            return;

        // 낚싯찌를 던질 때 (FISHING 상태)
        if (event.getState() == PlayerFishEvent.State.FISHING) {
            if (event.getHook() != null) {
                Vector velocity = event.getHook().getVelocity();

                // 속도 크기 제한 (최대 10.0)
                velocity = velocity.normalize().multiply(Math.min(velocity.length() * 3.0, 10.0));

                event.getHook().setVelocity(velocity); // 제한된 속도를 설정
            }
            return;
        }

        // 적 낚싯대를 사용할 때 (CAUGHT_ENTITY 상태)
        if (event.getState() == PlayerFishEvent.State.CAUGHT_ENTITY) {
            Entity target = event.getCaught();

            // LivingEntity 여부 확인
            if (!(target instanceof LivingEntity)) return;

            LivingEntity livingTarget = (LivingEntity) target;

            // 쿨타임 체크
            long lastUsed = meta.getPersistentDataContainer().getOrDefault(
                    new NamespacedKey(plugin, "cooldown"), PersistentDataType.LONG, 0L);
            long currentTime = System.currentTimeMillis();
            long cooldownTime = 60000; // 60초 쿨타임 설정

            if (currentTime - lastUsed < cooldownTime) {
                return;
            }

            // 쿨타임 업데이트
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "cooldown"), PersistentDataType.LONG, currentTime);
            item.setItemMeta(meta);

            player.setCooldown(item.getType(), (int) (cooldownTime / 50)); // 50틱 = 1초

            // 대상 끌어오는 로직
            try {
                // 대상 위로 띄우기
                org.bukkit.util.Vector upwardForce = new org.bukkit.util.Vector(0, 1.2, 0); // 위쪽으로 1.2 힘 가함
                livingTarget.setVelocity(upwardForce);

                // 10틱 후 대상 끌어오기
                Location playerLocation = player.getLocation();
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (!livingTarget.isValid() || livingTarget.isDead()) return; // 유효성 확인

                    Location targetLocation = livingTarget.getLocation();
                    double pullX = playerLocation.getX() - targetLocation.getX();
                    double pullY = playerLocation.getY() - targetLocation.getY();
                    double pullZ = playerLocation.getZ() - targetLocation.getZ();

                    // 속도를 제한
                    org.bukkit.util.Vector pullForce = new org.bukkit.util.Vector(pullX, pullY, pullZ).normalize().multiply(2.0);
                    if (pullForce.length() > 10.0) {
                        pullForce = pullForce.normalize().multiply(10.0);
                    }

                    livingTarget.setVelocity(pullForce);

                    // 효과음 재생
                    livingTarget.getWorld().playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 1.5f, 1.0f);

                }, 10); // 10틱 이후 실행
            } catch (Exception e) {
                plugin.getLogger().warning("Error while pulling entity with fishing rod: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void disableAllShields(Player player) {
        ItemStack mainHandItem = player.getInventory().getItemInMainHand();
        ItemStack offHandItem = player.getInventory().getItemInOffHand();

        if (mainHandItem != null && mainHandItem.getType() == Material.SHIELD) {
            applyShieldCooldown(player, EquipmentSlot.HAND);
        }

        if (offHandItem != null && offHandItem.getType() == Material.SHIELD) {
            applyShieldCooldown(player, EquipmentSlot.OFF_HAND);
        }
    }

    private void applyShieldCooldown(Player player, EquipmentSlot slot) {
        if (slot == EquipmentSlot.HAND) {
            player.getInventory().getItemInMainHand().setDurability((short) 59);
        } else if (slot == EquipmentSlot.OFF_HAND) {
            player.getInventory().getItemInOffHand().setDurability((short) 59);
        }
        player.setCooldown(Material.SHIELD, 600); // 1200틱 = 60초
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