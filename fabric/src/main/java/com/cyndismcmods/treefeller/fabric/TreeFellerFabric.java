package com.cyndismcmods.treefeller.fabric;

import com.cyndismcmods.treefeller.common.BlockPos3;
import com.cyndismcmods.treefeller.common.FellRunner;
import com.cyndismcmods.treefeller.common.TreeScanner;
import java.util.List;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public class TreeFellerFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (!(world instanceof ServerLevel level) || FellRunner.isActive()) {
                return true;
            }
            ItemStack stack = player.getMainHandItem();
            if (!stack.is(ItemTags.AXES) || !state.is(BlockTags.LOGS)) {
                return true;
            }
            List<BlockPos3> logs = TreeScanner.findTree(
                    new LevelTreeWorld(level, state.getBlock()), new BlockPos3(pos.getX(), pos.getY(), pos.getZ()));
            if (logs.isEmpty()) {
                return true;
            }
            FellRunner.run(() -> FellRunner.fell(
                    logs,
                    p -> level.destroyBlock(LevelTreeWorld.toPos(p), true, player),
                    () -> {
                        stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
                        return stack.isEmpty();
                    },
                    player.getAbilities().instabuild));
            return false;
        });
    }
}
