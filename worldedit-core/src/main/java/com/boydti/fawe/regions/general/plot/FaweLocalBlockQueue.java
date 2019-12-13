package com.boydti.fawe.regions.general.plot;

import com.boydti.fawe.FaweAPI;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.util.SetQueue;
import com.github.intellectualsites.plotsquared.plot.util.block.LocalBlockQueue;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;

// TODO FIXME
public class FaweLocalBlockQueue extends LocalBlockQueue {

    public final FaweQueue IMP;

    public FaweLocalBlockQueue(String world) {
        super(world);
        IMP = SetQueue.IMP.getNewQueue(FaweAPI.getWorld(world), true, false);
    }

    @Override
    public boolean next() {
        return IMP.size() > 0;
    }

    @Override
    public void startSet(boolean parallel) {
        IMP.startSet(parallel);
    }

    @Override
    public void endSet(boolean parallel) {
        IMP.endSet(parallel);
    }

    @Override
    public int size() {
        return IMP.size();
    }

    @Override
    public void optimize() {
        IMP.optimize();
    }

    @Override
    public void setModified(long l) {
        IMP.setModified(l);
    }

    @Override
    public long getModified() {
        return IMP.getModified();
    }

    @Override
    public boolean setBlock(final int x, final int y, final int z, final BlockState id) {
    	return IMP.setBlock(x, y, z, id);
    }

    @Override
    public boolean setBlock(final int x, final int y, final int z, final BaseBlock id) {
    	return IMP.setBlock(x, y, z, id);
    }

    @Override
    public BlockState getBlock(int x, int y, int z) {
        int combined = IMP.getCombinedId4Data(x, y, z);
        return BlockState.getFromInternalId(combined);
    }

    @Override
    public boolean setBiome(int x, int z, BiomeType biomeType) {
        return IMP.setBiome(x, 0, z, biomeType);
    }

    @Override
    public String getWorld() {
        return IMP.getWorldName();
    }

    @Override
    public void flush() {
        IMP.flush();
    }

    @Override
    public boolean enqueue() {
        super.enqueue();
        return IMP.enqueue();
    }

    @Override
    public void refreshChunk(int x, int z) {
        IMP.sendChunk(IMP.getFaweChunk(x, z));
    }

    @Override
    public void fixChunkLighting(int x, int z) {
    }

    @Override
    public void regenChunk(int x, int z) {
        IMP.regenerateChunk(x, z);
    }

    @Override
    public boolean setTile(int x, int y, int z, CompoundTag tag) {
        IMP.setTile(x, y, z, (com.sk89q.jnbt.CompoundTag) FaweCache.asTag(tag));
        return true;
    }
}
