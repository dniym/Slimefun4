package io.github.thebusybiscuit.slimefun4.implementation.items.tools;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.cscorelib2.item.CustomItem;
import io.github.thebusybiscuit.cscorelib2.materials.MaterialCollections;
import io.github.thebusybiscuit.cscorelib2.protection.ProtectableAction;
import io.github.thebusybiscuit.slimefun4.api.items.ItemSetting;
import io.github.thebusybiscuit.slimefun4.core.attributes.DamageableItem;
import io.github.thebusybiscuit.slimefun4.core.attributes.NotPlaceable;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunPlugin;
import me.mrCookieSlime.Slimefun.Lists.RecipeType;
import me.mrCookieSlime.Slimefun.Objects.Category;
import me.mrCookieSlime.Slimefun.Objects.SlimefunBlockHandler;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.SimpleSlimefunItem;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.SlimefunItem;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.UnregisterReason;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.Slimefun;
import me.mrCookieSlime.Slimefun.api.SlimefunItemStack;

class ExplosiveTool extends SimpleSlimefunItem<BlockBreakHandler> implements NotPlaceable, DamageableItem {

    private final ItemSetting<Boolean> damageOnUse = new ItemSetting<>("damage-on-use", true);
    private final ItemSetting<Boolean> callExplosionEvent = new ItemSetting<>("call-explosion-event", false);

    public ExplosiveTool(Category category, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(category, item, recipeType, recipe);
        addItemSetting(damageOnUse, callExplosionEvent);
    }

    @Override
    public BlockBreakHandler getItemHandler() {
        return new BlockBreakHandler() {

            @Override
            public boolean isPrivate() {
                return false;
            }

            @Override
            public boolean onBlockBreak(BlockBreakEvent e, ItemStack item, int fortune, List<ItemStack> drops) {
                if (isItem(item)) {
                    Player p = e.getPlayer();
                    if (Slimefun.hasUnlocked(p, ExplosiveTool.this, true)) {
                        Block b = e.getBlock();

                        b.getWorld().createExplosion(b.getLocation(), 0.0F);
                        b.getWorld().playSound(b.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.2F, 1F);

                        List<Block> blocks = findBlocks(b);
                        if (callExplosionEvent.getValue().booleanValue()) {
                            BlockExplodeEvent blockExplodeEvent = new BlockExplodeEvent(b, blocks, 0);
                            Bukkit.getServer().getPluginManager().callEvent(blockExplodeEvent);

                            if (!blockExplodeEvent.isCancelled()) {
                                for (Block block : blockExplodeEvent.blockList()) {
                                    breakBlock(p, item, block, fortune, drops);
                                }
                            }
                        }
                        else {
                            for (Block block : blocks) {
                                breakBlock(p, item, block, fortune, drops);
                            }
                        }
                    }

                    return true;
                }
                else {
                    return false;
                }
            }
        };
    }

    private List<Block> findBlocks(Block b) {
        List<Block> blocks = new ArrayList<>(26);

        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    // We can skip the center block since that will break as usual
                    if (x == 0 && y == 0 && z == 0) {
                        continue;
                    }

                    blocks.add(b.getRelative(x, y, z));
                }
            }
        }

        return blocks;
    }

    @Override
    public boolean isDamageable() {
        return damageOnUse.getValue();
    }

    protected void breakBlock(Player p, ItemStack item, Block b, int fortune, List<ItemStack> drops) {
        if (b.getType() != Material.AIR && !b.isLiquid() && !MaterialCollections.getAllUnbreakableBlocks().contains(b.getType()) && SlimefunPlugin.getProtectionManager().hasPermission(p, b.getLocation(), ProtectableAction.BREAK_BLOCK)) {
            SlimefunPlugin.getProtectionManager().logAction(p, b, ProtectableAction.BREAK_BLOCK);

            b.getWorld().playEffect(b.getLocation(), Effect.STEP_SOUND, b.getType());
            SlimefunItem sfItem = BlockStorage.check(b);

            if (sfItem != null && !sfItem.useVanillaBlockBreaking()) {
                SlimefunBlockHandler handler = SlimefunPlugin.getRegistry().getBlockHandlers().get(sfItem.getID());

                if (handler != null && !handler.onBreak(p, b, sfItem, UnregisterReason.PLAYER_BREAK)) {
                    drops.add(BlockStorage.retrieve(b));
                }
            }
            else if (b.getType() == Material.PLAYER_HEAD || b.getType().name().endsWith("_SHULKER_BOX")) {
                b.breakNaturally();
            }
            else {
                boolean applyFortune = b.getType().name().endsWith("_ORE") && b.getType() != Material.IRON_ORE && b.getType() != Material.GOLD_ORE;

                for (ItemStack drop : b.getDrops(getItem())) {
                    // For some reason this check is necessary with Paper
                    if (drop != null && drop.getType() != Material.AIR) {
                        b.getWorld().dropItemNaturally(b.getLocation(), applyFortune ? new CustomItem(drop, fortune) : drop);
                    }
                }

                b.setType(Material.AIR);
            }

            damageItem(p, item);
        }
    }

}
