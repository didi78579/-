package upgrade_final2.upgrade_final2;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class CustomRecipe implements Listener {

    private final JavaPlugin plugin;
    private final Set<String> processedPlayers = new HashSet<>();

    public CustomRecipe(JavaPlugin plugin) {
        this.plugin = plugin;
        registerRecipes();
        plugin.getServer().getPluginManager().registerEvents(this, plugin); // 이벤트 리스너 등록
    }

    private void registerRecipes() {
        // 레시피 등록
        registerMagicPaperRecipe();
        registerHorseSpawnEggRecipe();
        registerEnchantedAppleRecipe();
    }

    private void registerMagicPaperRecipe() {
        // 아이템을 메아리 조각으로 변경
        ItemStack customShard = new ItemStack(Material.ECHO_SHARD, 4);

        ItemMeta meta = customShard.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("마석")
                    .color(NamedTextColor.GOLD)
                    .decoration(TextDecoration.ITALIC, false)); // 이탤릭체 비활성화
            customShard.setItemMeta(meta);
        }

        // 새로운 레시피 키 정의ㅣ
        NamespacedKey key = new NamespacedKey(plugin, "echo_shard_bundle");

        removeRecipeIfExists(key); // 중복 제거

        ShapedRecipe recipe = new ShapedRecipe(key, customShard);
        recipe.shape("LLL", "LBL", "LLL");
        recipe.setIngredient('L', Material.LAPIS_LAZULI); // 라피스 블록 변경
        recipe.setIngredient('B', Material.BOOK);

        // Bukkit을 통해 레시피 추가
        Bukkit.addRecipe(recipe);
    }

    private void registerHorseSpawnEggRecipe() {
        ItemStack horseEgg = new ItemStack(Material.HORSE_SPAWN_EGG);

        NamespacedKey key = new NamespacedKey(plugin, "horse_spawn_egg");

        removeRecipeIfExists(key);

        ShapedRecipe recipe = new ShapedRecipe(key, horseEgg);
        recipe.shape("  L", "DED", "L L");
        recipe.setIngredient('L', Material.LEATHER);
        recipe.setIngredient('D', Material.DIAMOND);
        recipe.setIngredient('E', Material.SADDLE);

        Bukkit.addRecipe(recipe);
    }

    private void registerEnchantedAppleRecipe() {
        ItemStack enchantedApple = new ItemStack(Material.ENCHANTED_GOLDEN_APPLE);

        NamespacedKey key = new NamespacedKey(plugin, "enchanted_golden_apple");

        removeRecipeIfExists(key);

        ShapedRecipe recipe = new ShapedRecipe(key, enchantedApple);
        recipe.shape(" G ", "GAG", " G ");
        recipe.setIngredient('G', Material.GOLD_BLOCK);
        recipe.setIngredient('A', Material.APPLE);

        Bukkit.addRecipe(recipe);
    }

    private void removeRecipeIfExists(NamespacedKey key) {
        Iterator<Recipe> it = Bukkit.recipeIterator();
        while (it.hasNext()) {
            Recipe recipe = it.next();
            if (recipe instanceof ShapedRecipe) {
                ShapedRecipe shapedRecipe = (ShapedRecipe) recipe;
                if (shapedRecipe.getKey().equals(key)) {
                    it.remove();
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerUseSpawnEgg(PlayerInteractEvent event) {
        if ((event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR)
                && event.getHand() == EquipmentSlot.HAND) { // 메인 핸드에서만 작동
            ItemStack item = event.getItem();
            if (item != null && item.getType() == Material.HORSE_SPAWN_EGG) {
                Player player = event.getPlayer();
                String playerUUID = player.getUniqueId().toString();

                // 이미 처리된 이벤트인지 확인
                if (processedPlayers.contains(playerUUID)) {
                    return;
                }

                // 이벤트를 처리 중인 것으로 표시
                processedPlayers.add(playerUUID);

                // 작업을 별도의 스레드로 처리 비동기 실행
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    processedPlayers.remove(playerUUID);
                }, 1L); // 1틱 후에 삭제

                event.setCancelled(true); // 기본 스폰 이벤트 취소

                Bukkit.getScheduler().runTask(plugin, () -> {
                    Horse horse = (Horse) player.getWorld().spawnEntity(player.getLocation(), EntityType.HORSE);

                    horse.setTamed(true);
                    horse.setOwner(player);
                    horse.getInventory().setSaddle(new ItemStack(Material.SADDLE));
                    horse.getInventory().setArmor(new ItemStack(Material.DIAMOND_HORSE_ARMOR));

                    // 말의 속도를 1.5배로 증가
                    horse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(
                            horse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getBaseValue() * 2
                    );

                    // 소환 알 개수 1개 감소
                    if (item.getAmount() > 1) {
                        item.setAmount(item.getAmount() - 1);
                    } else {
                        player.getInventory().removeItem(item);
                    }
                });
            }
        }
    }
}