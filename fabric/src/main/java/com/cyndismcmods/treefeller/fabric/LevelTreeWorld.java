package com.cyndismcmods.treefeller.fabric;

import com.cyndismcmods.treefeller.common.BlockPos3;
import com.cyndismcmods.treefeller.common.TreeWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

final class LevelTreeWorld implements TreeWorld {
    private final ServerLevel level;
    private final Block origin;

    LevelTreeWorld(ServerLevel level, Block origin) {
        this.level = level;
        this.origin = origin;
    }

    @Override
    public boolean isSameLog(BlockPos3 pos) {
        return level.getBlockState(toPos(pos)).is(origin);
    }

    @Override
    public boolean isNaturalLeaf(BlockPos3 pos) {
        BlockState state = level.getBlockState(toPos(pos));
        return state.is(BlockTags.LEAVES) && !state.getValue(LeavesBlock.PERSISTENT);
    }

    static BlockPos toPos(BlockPos3 p) {
        return new BlockPos(p.x(), p.y(), p.z());
    }
}
