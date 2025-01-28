package upgrade_final2.upgrade_final2;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.block.Action;

import java.util.*;

public class CustomEnchanting implements Listener {
    private final JavaPlugin plugin;
    private static final String ENCHANT_GUI_TITLE = "마법부여대";
    private static final NamespacedKey ENCHANTING_DOCUMENT_KEY = new NamespacedKey("upgrade_final2", "enchanted_book_bundle");
    private static final NamespacedKey ENCHANTED_FLAG_KEY = new NamespacedKey("upgrade_final2", "enchanted_flag");

    public CustomEnchanting(JavaPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onEnchantTableInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.ENCHANTING_TABLE) {
            event.setCancelled(true);

            Player player = event.getPlayer();
            Inventory customGui = Bukkit.createInventory(null, 9, ENCHANT_GUI_TITLE);

            // GUI 초기화
            for (int i = 0; i < customGui.getSize(); i++) {
                customGui.setItem(i, createGuiItem(Material.ENCHANTED_BOOK, ""));
            }

            customGui.setItem(4, new ItemStack(Material.ENCHANTING_TABLE)); // 가운데에 인첸트 테이블

            player.openInventory(customGui);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        Inventory clickedInventory = event.getClickedInventory();

        // 커스텀 GUI에서의 클릭 이벤트 처리
        if (event.getView().getTitle().equals(ENCHANT_GUI_TITLE)) {
            // 클릭된 아이템이 슬롯 내에 있는 경우에만 처리
            if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR) {
                event.setCancelled(true);

                ItemStack currentItem = event.getCurrentItem();
                Inventory customInventory = event.getView().getTopInventory();
                ItemStack guiItem = customInventory.getItem(4);

                if (clickedInventory == player.getInventory()) {
                    // 플레이어 인벤토리에서 커스텀 GUI로 아이템 이동
                    if (guiItem != null && guiItem.getType() == Material.ENCHANTING_TABLE) {
                        customInventory.setItem(4, currentItem); // 4번 슬롯에 아이템 배치
                        player.getInventory().clear(event.getSlot()); // 플레이어 인벤토리에서 아이템 제거
                    }
                } else if (clickedInventory == customInventory && event.getRawSlot() == 4) {
                    if (guiItem != null && guiItem.getType() != Material.ENCHANTING_TABLE) {
                        if (event.isLeftClick()) {
                            // 좌클릭 시 인첸트 부여
                            ItemMeta meta = guiItem.getItemMeta();
                            if (meta != null && hasUpgradeStars(meta)) {
                                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1.0f, 1.8f);
                                player.sendMessage(ChatColor.RED+"이 아이템은 이미 강화되었습니다. 더 이상 인첸트를 할 수 없습니다.");
                            } else if (enchantItem(guiItem, player)) {

                            } else {
                                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1.0f, 1.8f);
                                player.sendMessage(ChatColor.RED+"마석이 부족합니다.");
                            }
                        } else if (event.isRightClick()) {
                            // 우클릭 시 아이템 회수
                            player.getInventory().addItem(guiItem);
                            customInventory.setItem(4, new ItemStack(Material.ENCHANTING_TABLE)); // 4번 슬롯에 인첸트 테이블 다시 배치
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView().getTitle().equals(ENCHANT_GUI_TITLE)) {
            Inventory customInventory = event.getInventory();
            ItemStack item = customInventory.getItem(4);

            if (item != null && item.getType() != Material.ENCHANTING_TABLE) {
                // 인첸트 테이블이 아닌 아이템이 남아 있을 경우 플레이어 인벤토리에 추가
                Player player = (Player) event.getPlayer();
                player.getInventory().addItem(item);
            }
        }
    }

    private ItemStack createGuiItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name)
                    .color(NamedTextColor.GOLD)
                    .decoration(TextDecoration.ITALIC, false)); // 이탤릭체 제거
            meta.lore(Collections.singletonList(Component.text("").decoration(TextDecoration.ITALIC, false))); // Lore도 이탤릭 제거
            item.setItemMeta(meta);
        }
        return item;
    }

    private boolean enchantItem(ItemStack item, Player player) {
        // 마법부여 문서 여부 확인 및 소모
        if (!hasEnchantingDocument(player)) {
            // 플레이어 인벤토리에 마법부여 문서가 없으면 인첸트 불가
            return false;
        }

        Random random = new Random();
        double extraEnchantChance = 0.10; // 10% 추가 인첸트 확률
        double curseChance = 0.01; // 저주 확률 1%
        int maxEnchantments = 8; // 최대 인첸트 수

        // 제외할 인첸트의 리스트 작성
        List<Enchantment> excludedEnchants = new ArrayList<>();
        excludedEnchants.add(Enchantment.BINDING_CURSE);
        excludedEnchants.add(Enchantment.VANISHING_CURSE);
        excludedEnchants.add(Enchantment.THORNS);
        try {
            // Breach가 Bukkit에서 공식 인첸트로 추가되었는지 확인
            Enchantment breachEnchantment = Enchantment.getByName("BREACH");
            if (breachEnchantment != null) {
                excludedEnchants.add(breachEnchantment);
            }
        } catch (Exception e) {
            // Breach 인첸트가 없다면 무시
            player.sendMessage("Breach 인첸트를 확인할 수 없습니다.");
        }

        // 기존 인첸트와 저주를 분리하여 저장
        Map<Enchantment, Integer> existingEnchantments = new HashMap<>();
        Map<Enchantment, Integer> existingCurses = new HashMap<>();

        for (Map.Entry<Enchantment, Integer> entry : item.getEnchantments().entrySet()) {
            if (entry.getKey() == Enchantment.BINDING_CURSE || entry.getKey() == Enchantment.VANISHING_CURSE) {
                // 저주는 따로 저장
                existingCurses.put(entry.getKey(), entry.getValue());
            } else {
                existingEnchantments.put(entry.getKey(), entry.getValue());
            }
        }

        // 기존 인첸트 제거 (저주는 유지)
        for (Enchantment enchantment : existingEnchantments.keySet()) {
            item.removeEnchantment(enchantment);
        }

        // 새로운 인첸트 목록 초기화 (저주는 유지)
        Map<Enchantment, Integer> newEnchantments = new HashMap<>(existingCurses);
        int enchantmentCount = existingEnchantments.size();

        if (enchantmentCount == 0) {
            // 아이템에 처음 인첸트를 추가할 때, 반드시 하나의 인첸트를 추가
            Enchantment randomEnchant = Enchantment.values()[random.nextInt(Enchantment.values().length)];
            while (excludedEnchants.contains(randomEnchant)) {
                randomEnchant = Enchantment.values()[random.nextInt(Enchantment.values().length)];
            }
            newEnchantments.put(randomEnchant, random.nextInt(randomEnchant.getMaxLevel()) + 1);
            enchantmentCount++;
        } else {
            // 기존 인첸트 개수만큼 새로운 인첸트를 추가
            for (int i = 0; i < enchantmentCount; i++) {
                Enchantment randomEnchant = Enchantment.values()[random.nextInt(Enchantment.values().length)];
                while (excludedEnchants.contains(randomEnchant) || newEnchantments.containsKey(randomEnchant)) {
                    randomEnchant = Enchantment.values()[random.nextInt(Enchantment.values().length)];
                }
                newEnchantments.put(randomEnchant, random.nextInt(randomEnchant.getMaxLevel()) + 1);
            }
        }

        // 추가 인첸트 부여 (저주는 포함하지 않음)
        while (enchantmentCount < maxEnchantments && random.nextDouble() < extraEnchantChance) {
            Enchantment randomEnchant = Enchantment.values()[random.nextInt(Enchantment.values().length)];
            while (excludedEnchants.contains(randomEnchant) || newEnchantments.containsKey(randomEnchant)) {
                randomEnchant = Enchantment.values()[random.nextInt(Enchantment.values().length)];
            }
            newEnchantments.put(randomEnchant, random.nextInt(randomEnchant.getMaxLevel()) + 1);
            enchantmentCount++;
        }

        // 저주 추가 (최대 저주 수는 따로 제한 없음)
        if (!newEnchantments.containsKey(Enchantment.BINDING_CURSE) && random.nextDouble() < curseChance) {
            newEnchantments.put(Enchantment.BINDING_CURSE, 1);
        }
        if (!newEnchantments.containsKey(Enchantment.VANISHING_CURSE) && random.nextDouble() < curseChance) {
            newEnchantments.put(Enchantment.VANISHING_CURSE, 1);
        }

        // 정리된 인첸트를 아이템에 최종 적용
        for (Map.Entry<Enchantment, Integer> entry : newEnchantments.entrySet()) {
            item.addUnsafeEnchantment(entry.getKey(), entry.getValue());
        }

        // 아이템에 커스텀 데이터를 추가
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            PersistentDataContainer data = meta.getPersistentDataContainer();
            data.set(ENCHANTED_FLAG_KEY, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }

        // 마법부여 문서를 하나 소모
        consumeEnchantingDocument(player);

        // 성공적으로 인첸트가 완료되었음을 알리는 소리 재생
        player.playSound(player.getLocation(), Sound.BLOCK_CHAIN_PLACE, 1.0f, 1.0f);

        return true;
    }

    private boolean hasEnchantingDocument(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isEnchantingDocument(item)) {
                return true;
            }
        }
        return false;
    }

    private boolean isEnchantingDocument(ItemStack item) {
        if (item == null || item.getType() != Material.ECHO_SHARD) { // Material.ECHO_SHARD 확인
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName()
                && meta.displayName().equals(Component.text("마석")
                .color(NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false)); // 이탤릭체 없는 이름 확인
    }

    private void consumeEnchantingDocument(Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            if (isEnchantingDocument(contents[i])) {
                ItemStack item = contents[i];
                item.setAmount(item.getAmount() - 1);
                if (item.getAmount() <= 0) {
                    contents[i] = null;
                }
                player.getInventory().setContents(contents);
                return;
            }
        }
    }

    private boolean hasUpgradeStars(ItemMeta meta) {
        if (meta != null && meta.getLore() != null) {
            for (String line : meta.getLore()) {
                if (line.contains(ChatColor.GOLD + "★") || line.contains(ChatColor.GRAY + "☆")) {
                    return true;
                }
            }
        }
        return false;
    }
}