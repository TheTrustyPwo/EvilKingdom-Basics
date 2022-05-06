package net.minecraft.world.level.block.grower;

import java.util.Iterator;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
// CraftBukkit start
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import org.bukkit.TreeType;
// CraftBukkit end

public abstract class AbstractTreeGrower {

    public AbstractTreeGrower() {}

    @Nullable
    protected abstract Holder<? extends ConfiguredFeature<?, ?>> getConfiguredFeature(Random random, boolean bees);

    public boolean growTree(ServerLevel world, ChunkGenerator chunkGenerator, BlockPos pos, BlockState state, Random random) {
        Holder<? extends ConfiguredFeature<?, ?>> holder = this.getConfiguredFeature(random, this.hasFlowers(world, pos));

        if (holder == null) {
            return false;
        } else {
            this.setTreeType(holder); // CraftBukkit
            ConfiguredFeature<?, ?> worldgenfeatureconfigured = (ConfiguredFeature) holder.value();

            world.setBlock(pos, Blocks.AIR.defaultBlockState(), 4);
            if (worldgenfeatureconfigured.place(world, chunkGenerator, random, pos)) {
                return true;
            } else {
                world.setBlock(pos, state, 4);
                return false;
            }
        }
    }

    private boolean hasFlowers(LevelAccessor world, BlockPos pos) {
        Iterator iterator = BlockPos.MutableBlockPos.betweenClosed(pos.below().north(2).west(2), pos.above().south(2).east(2)).iterator();

        BlockPos blockposition1;

        do {
            if (!iterator.hasNext()) {
                return false;
            }

            blockposition1 = (BlockPos) iterator.next();
        } while (!world.getBlockState(blockposition1).is(BlockTags.FLOWERS));

        return true;
    }

    // CraftBukkit start
    protected void setTreeType(Holder<? extends ConfiguredFeature<?, ?>> worldgentreeabstract) {
        if (worldgentreeabstract == TreeFeatures.OAK || worldgentreeabstract == TreeFeatures.OAK_BEES_005) {
            SaplingBlock.treeType = TreeType.TREE;
        } else if (worldgentreeabstract == TreeFeatures.HUGE_RED_MUSHROOM) {
            SaplingBlock.treeType = TreeType.RED_MUSHROOM;
        } else if (worldgentreeabstract == TreeFeatures.HUGE_BROWN_MUSHROOM) {
            SaplingBlock.treeType = TreeType.BROWN_MUSHROOM;
        } else if (worldgentreeabstract == TreeFeatures.JUNGLE_TREE) {
            SaplingBlock.treeType = TreeType.COCOA_TREE;
        } else if (worldgentreeabstract == TreeFeatures.JUNGLE_TREE_NO_VINE) {
            SaplingBlock.treeType = TreeType.SMALL_JUNGLE;
        } else if (worldgentreeabstract == TreeFeatures.PINE) {
            SaplingBlock.treeType = TreeType.TALL_REDWOOD;
        } else if (worldgentreeabstract == TreeFeatures.SPRUCE) {
            SaplingBlock.treeType = TreeType.REDWOOD;
        } else if (worldgentreeabstract == TreeFeatures.ACACIA) {
            SaplingBlock.treeType = TreeType.ACACIA;
        } else if (worldgentreeabstract == TreeFeatures.BIRCH || worldgentreeabstract == TreeFeatures.BIRCH_BEES_005) {
            SaplingBlock.treeType = TreeType.BIRCH;
        } else if (worldgentreeabstract == TreeFeatures.SUPER_BIRCH_BEES_0002) {
            SaplingBlock.treeType = TreeType.TALL_BIRCH;
        } else if (worldgentreeabstract == TreeFeatures.SWAMP_OAK) {
            SaplingBlock.treeType = TreeType.SWAMP;
        } else if (worldgentreeabstract == TreeFeatures.FANCY_OAK || worldgentreeabstract == TreeFeatures.FANCY_OAK_BEES_005) {
            SaplingBlock.treeType = TreeType.BIG_TREE;
        } else if (worldgentreeabstract == TreeFeatures.JUNGLE_BUSH) {
            SaplingBlock.treeType = TreeType.JUNGLE_BUSH;
        } else if (worldgentreeabstract == TreeFeatures.DARK_OAK) {
            SaplingBlock.treeType = TreeType.DARK_OAK;
        } else if (worldgentreeabstract == TreeFeatures.MEGA_SPRUCE) {
            SaplingBlock.treeType = TreeType.MEGA_REDWOOD;
        } else if (worldgentreeabstract == TreeFeatures.MEGA_PINE) {
            SaplingBlock.treeType = TreeType.MEGA_REDWOOD;
        } else if (worldgentreeabstract == TreeFeatures.MEGA_JUNGLE_TREE) {
            SaplingBlock.treeType = TreeType.JUNGLE;
        } else if (worldgentreeabstract == TreeFeatures.AZALEA_TREE) {
            SaplingBlock.treeType = TreeType.AZALEA;
        } else {
            throw new IllegalArgumentException("Unknown tree generator " + worldgentreeabstract);
        }
    }
    // CraftBukkit end
}