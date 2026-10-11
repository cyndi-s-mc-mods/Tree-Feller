package com.cyndismcmods.treefeller.forge;

import com.cyndismcmods.treefeller.common.BlockPos3;
import com.cyndismcmods.treefeller.common.FellRunner;
import com.cyndismcmods.treefeller.common.TreeScanner;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.fml.common.Mod;

@Mod("treefeller")
public class TreeFellerForge {
    public TreeFellerForge() {
        // Predicate listener: returning true cancels the event (vanilla break).
        BlockEvent.BreakEvent.BUS.addListener(TreeFellerForge::onBreak);
    }

    private static boolean onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || FellRunner.isActive()) {
            return false;
        }
        Player player = event.getPlayer();
        BlockPos pos = event.getPos();
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(ItemTags.AXES) || !event.getState().is(BlockTags.LOGS)) {
            return false;
        }
        List<BlockPos3> logs = TreeScanner.findTree(
                new LevelTreeWorld(level, event.getState().getBlock()), new BlockPos3(pos.getX(), pos.getY(), pos.getZ()));
        if (logs.isEmpty()) {
            return false;
        }
        FellRunner.run(() -> FellRunner.fell(
                logs,
                p -> level.destroyBlock(LevelTreeWorld.toPos(p), true, player),
                () -> {
                    stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
                    return stack.isEmpty();
                },
                player.getAbilities().instabuild));
        return true;
    }
}
