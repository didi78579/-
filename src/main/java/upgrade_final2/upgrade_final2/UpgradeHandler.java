package upgrade_final2.upgrade_final2;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.bukkit.block.Block;
import org.bukkit.event.block.Action;

public class UpgradeHandler implements Listener {

    private static final int UPGRADE_ITEM_SLOT = 4; // 4번 슬롯
    private static final int MAX_UPGRADE_LEVEL = 10;
    private static final String ANVIL_NAME = ChatColor.LIGHT_PURPLE + "강화 모루";
    private static final Random random = new Random();
    private final JavaPlugin plugin;

    public UpgradeHandler(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onAnvilClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block clickedBlock = event.getClickedBlock();

        // 블록이 클릭되었는지 확인
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && clickedBlock != null) {
            Material blockType = clickedBlock.getType();

            // 클릭된 블록이 모루 종류인지 확인
            if (blockType == Material.ANVIL || blockType == Material.CHIPPED_ANVIL || blockType == Material.DAMAGED_ANVIL) {
                // GUI 열기
                openUpgradeGUI(player);
                event.setCancelled(true); // 모루 UI의 기본 동작 막기
            }
        }
    }

    public void openUpgradeGUI(Player player) {
        Inventory upgradeInventory = Bukkit.createInventory(null, 9, "장비 대장간");

        // 강화 모루 슬롯 초기화
        upgradeInventory.setItem(UPGRADE_ITEM_SLOT, createAnvil());

        // 강화석 구매 슬롯 (다이아몬드)
        ItemStack diamondItem = new ItemStack(Material.DIAMOND);
        ItemMeta diamondMeta = diamondItem.getItemMeta();
        if (diamondMeta != null) {
            diamondMeta.setDisplayName(ChatColor.AQUA + "다이아몬드로 강화석 구매");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "다이아 16개=>강화석 8개");
            diamondMeta.setLore(lore);
            diamondItem.setItemMeta(diamondMeta);
        }
        upgradeInventory.setItem(0, diamondItem);

        // 강화석 구매 슬롯 (경험치)
        ItemStack xpItem = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta xpMeta = xpItem.getItemMeta();
        if (xpMeta != null) {
            xpMeta.setDisplayName(ChatColor.GREEN + "경험치로 강화석 구매");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "40 레벨=>강화석 128개");
            xpMeta.setLore(lore);
            xpItem.setItemMeta(xpMeta);
        }
        upgradeInventory.setItem(8, xpItem);

        // GUI 열기
        player.openInventory(upgradeInventory);
    }


    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inventory = event.getInventory();

        if ("장비 대장간".equals(event.getView().getTitle())) {
            int slot = event.getRawSlot();
            ItemStack clickedItem = event.getCurrentItem();
            Player player = (Player) event.getWhoClicked();

            if (clickedItem != null && clickedItem.getType() != Material.AIR) {
                switch (slot) {
                    case 0 -> handleBuyWithDiamonds(event, player);
                    case 8 -> handleBuyWithXP(event, player);
                    case UPGRADE_ITEM_SLOT -> {
                        if (event.isLeftClick()) {
                            handleUpgrade(event, player);
                        } else if (event.isRightClick()) {
                            handleWithdraw(event, player, inventory, clickedItem);
                        }
                    }
                    default -> handleSlotUpgrade(event, player, inventory, clickedItem);
                }
                // 이벤트 취소
                event.setCancelled(true);
            }
        }
    }

    private void handleBuyWithDiamonds(InventoryClickEvent event, Player player) {
        ItemStack diamonds = new ItemStack(Material.DIAMOND, 16);
        if (player.getInventory().containsAtLeast(diamonds, 16)) {
            player.getInventory().removeItem(diamonds);
            player.sendMessage(ChatColor.AQUA + "다이아몬드로 강화석을 구매하셨습니다!");
            giveOrDropItems(player, UpgradeItem.getUpgradeStone(8));
        } else {
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1.0f, 1.8f);
            player.sendMessage(ChatColor.RED + "다이아몬드가 부족합니다!");
        }
    }

    private void handleBuyWithXP(InventoryClickEvent event, Player player) {
        int xpCost = 40;
        if (player.getLevel() >= xpCost) {
            player.setLevel(player.getLevel() - xpCost);
            player.sendMessage(ChatColor.GREEN + "경험치로 강화석을 구매하셨습니다!");
            giveOrDropItems(player, UpgradeItem.getUpgradeStone(128));
        } else {
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1.0f, 1.8f);
            player.sendMessage(ChatColor.RED + "경험치가 부족합니다!");
        }
    }

    private void giveOrDropItems(Player player, ItemStack items) {
        Inventory playerInventory = player.getInventory();
        Map<Integer, ItemStack> notAdded = playerInventory.addItem(items);

        if (!notAdded.isEmpty()) {
            Location playerLocation = player.getLocation();
            for (ItemStack item : notAdded.values()) {
                player.getWorld().dropItem(playerLocation, item);
            }
        }
    }

    private void handleSlotUpgrade(InventoryClickEvent event, Player player, Inventory inventory, ItemStack clickedItem) {
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        if (event.getClickedInventory() != null && event.getClickedInventory().equals(player.getInventory())) {
            ItemStack itemInUpgradeSlot = inventory.getItem(UPGRADE_ITEM_SLOT);

            if (itemInUpgradeSlot == null || itemInUpgradeSlot.getType() == Material.ANVIL) {
                if (UpgradableItems.isUpgradable(clickedItem.getType())) {
                    inventory.setItem(UPGRADE_ITEM_SLOT, new ItemStack(clickedItem));
                    player.getInventory().setItem(event.getSlot(), new ItemStack(Material.AIR));

                } else {
                    player.sendMessage(ChatColor.RED + "이 아이템은 강화할 수 없습니다.");
                }
            }
            event.setCancelled(true);
        }
    }

    private void handleWithdraw(InventoryClickEvent event, Player player, Inventory inventory, ItemStack clickedItem) {
        ItemStack itemInUpgradeSlot = inventory.getItem(UPGRADE_ITEM_SLOT);
        if (itemInUpgradeSlot != null && itemInUpgradeSlot.getType() != Material.ANVIL) {
            giveOrDropItems(player, new ItemStack(itemInUpgradeSlot));
            inventory.setItem(UPGRADE_ITEM_SLOT, createAnvil());
        }
    }

    private void handleUpgrade(InventoryClickEvent event, Player player) {
        Inventory inventory = event.getInventory();
        ItemStack itemInUpgradeSlot = inventory.getItem(UPGRADE_ITEM_SLOT);

        if (itemInUpgradeSlot != null && UpgradableItems.isUpgradable(itemInUpgradeSlot.getType())) {
            int currentLevel = getUpgradeLevel(itemInUpgradeSlot);

            if (currentLevel >= MAX_UPGRADE_LEVEL) {
                player.sendMessage("아이템이 최대 강화 레벨에 도달했습니다.");
                return;
            }

            int requiredUpgradeStones = currentLevel + 1;

            // 인벤토리에서 "강화석" 이름을 가진 아이템을 찾습니다.
            int foundStones = 0;
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && item.hasItemMeta()) {
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null && meta.hasDisplayName() && meta.getDisplayName().equals(ChatColor.LIGHT_PURPLE + "강화석")) {
                        foundStones += item.getAmount();
                        if (foundStones >= requiredUpgradeStones) {
                            break;
                        }
                    }
                }
            }

            if (foundStones >= requiredUpgradeStones) {
                // "강화석" 아이템 수량 차감
                int remainingStones = requiredUpgradeStones;
                for (ItemStack item : player.getInventory().getContents()) {
                    if (item != null && item.hasItemMeta()) {
                        ItemMeta meta = item.getItemMeta();
                        if (meta != null && meta.hasDisplayName() && meta.getDisplayName().equals(ChatColor.LIGHT_PURPLE + "강화석")) {
                            int amount = item.getAmount();
                            if (amount <= remainingStones) {
                                player.getInventory().remove(item);
                                remainingStones -= amount;
                            } else {
                                item.setAmount(amount - remainingStones);
                                remainingStones = 0;
                            }

                            if (remainingStones == 0) {
                                break;
                            }
                        }
                    }
                }

                int successChance = calculateSuccessChance(currentLevel);
                int failChance = calculateFailChance(currentLevel);
                int downgradeChance = calculateDowngradeChance(currentLevel);
                int destroyChance = calculateDestroyChance(currentLevel);

                int randomValue = random.nextInt(100) + 1;

                if (randomValue <= successChance) {
                    int newLevel = currentLevel + 1;
                    applyUpgrade(plugin, player, itemInUpgradeSlot, newLevel);
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.8f);
                    setUpgradeLevel(itemInUpgradeSlot, newLevel);
                } else if (randomValue <= successChance + failChance) {
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                } else if (randomValue <= successChance + failChance + downgradeChance) {
                    int newLevel = Math.max(currentLevel - 1, 0);
                    applyUpgrade(plugin, player, itemInUpgradeSlot, newLevel);
                    player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 1.5f);
                    setUpgradeLevel(itemInUpgradeSlot, newLevel);
                } else if (randomValue <= successChance + failChance + downgradeChance + destroyChance) {
                    inventory.setItem(UPGRADE_ITEM_SLOT, createAnvil());
                    player.getInventory().removeItem(itemInUpgradeSlot);

                    // 파괴 메시지
                    String itemName = itemInUpgradeSlot.getItemMeta().hasDisplayName() ? itemInUpgradeSlot.getItemMeta().getDisplayName() : itemInUpgradeSlot.getType().name();
                    String destroyMessage = ChatColor.GRAY + itemLevelString(currentLevel) + " " + itemName + "이(가) 연기가 되어 사라집니다.";
                    Bukkit.getServer().broadcastMessage(destroyMessage); // 전체 플레이어에게 메시지 전송

                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                }
            } else {
                player.sendMessage("강화석이 부족합니다!");
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1.0f, 1.8f);
            }
        } else {
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1.0f, 1.8f);
        }
    }

    public static void applyUpgrade(JavaPlugin plugin, Player player, ItemStack item, int level) {
        if (UpgradableItems.isSword(item.getType())) {
            SwordUpgrade.applySwordUpgrade(plugin, player, item, level);
        } else if (UpgradableItems.isHelmet(item.getType())) {
            HelmetUpgrade.applyHelmetUpgrade(plugin, player, item, level);
        } else if (UpgradableItems.isChestplate(item.getType())) {
            ChestplateUpgrade.applyChestplateUpgrade(plugin, player, item, level);
        } else if (UpgradableItems.isLeggings(item.getType())) {
            LeggingsUpgrade.applyLeggingsUpgrade(plugin, player, item, level);
        } else if (UpgradableItems.isBoots(item.getType())) {
            BootUpgrade.applyBootUpgrade(plugin, player, item, level);
        } else if (UpgradableItems.isBow(item.getType())) {
            BowUpgrade.applyBowUpgrade(plugin, player, item, level);
        } else if (UpgradableItems.isTrident(item.getType())) {
            TridentUpgrade.applyTridentUpgrade(plugin, player, item, level);
        } else if (UpgradableItems.isCrossBow(item.getType())) {
            CrossbowUpgrade.applyCrossBowUpgrade(plugin, player, item, level);
        } else if (UpgradableItems.isShield(item.getType())) {
            ShieldUpgrade shieldUpgrade = new ShieldUpgrade(plugin);
            shieldUpgrade.applyShieldUpgrade(player, item, level);
        } else if (UpgradableItems.isTool(item.getType())) {
            ToolUpgrade toolUpgrade = new ToolUpgrade(plugin);
            toolUpgrade.applyToolUpgrade(player, item, level);
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

    private void setUpgradeLevel(ItemStack item, int level) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.getLore();
            if (lore == null) {
                lore = new ArrayList<>();
            }

            lore.removeIf(line -> line.contains(ChatColor.GOLD + "★") || line.contains(ChatColor.GRAY + "☆"));

            StringBuilder stars = new StringBuilder();
            for (int i = 0; i < level; i++) {
                stars.append(ChatColor.GOLD).append("★");
            }
            for (int i = level; i < MAX_UPGRADE_LEVEL; i++) {
                stars.append(ChatColor.GRAY).append("☆");
            }

            lore.add(0, stars.toString().trim());
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
    }

    private int getUpgradeLevel(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.getLore() != null) {
            for (String line : meta.getLore()) {
                if (line.contains(ChatColor.GOLD + "★") || line.contains(ChatColor.GRAY + "☆")) {
                    int level = 0;
                    for (char c : line.toCharArray()) {
                        if (c == '★') {
                            level++;
                        }
                    }
                    return level;
                }
            }
        }
        return 0;
    }

    private ItemStack createAnvil() {
        ItemStack anvil = new ItemStack(Material.ANVIL);
        ItemMeta meta = anvil.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ANVIL_NAME);
            anvil.setItemMeta(meta);
        }
        return anvil;
    }

    private String itemLevelString(int level) {
        return switch (level) {
            case 0 -> "0강";
            case 1 -> "1강";
            case 2 -> "2강";
            case 3 -> "3강";
            case 4 -> "4강";
            case 5 -> "5강";
            case 6 -> "6강";
            case 7 -> "7강";
            case 8 -> "8강";
            case 9 -> "9강";
            default -> "알 수 없는 레벨";
        };
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();

        if ("장비 대장간".equals(event.getView().getTitle())) {
            ItemStack itemInSlot4 = inventory.getItem(UPGRADE_ITEM_SLOT);
            Player player = (Player) event.getPlayer();

            if (itemInSlot4 != null && itemInSlot4.getType() != Material.ANVIL) {
                giveOrDropItems(player, new ItemStack(itemInSlot4));
            }
            inventory.setItem(UPGRADE_ITEM_SLOT, createAnvil());
        }
    }
}