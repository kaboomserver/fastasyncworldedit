package com.boydti.fawe.bukkit.v1_13;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.bukkit.BukkitPlayer;
import com.boydti.fawe.bukkit.adapter.v1_13_1.BlockMaterial_1_13;
import com.boydti.fawe.bukkit.adapter.v1_13_1.Spigot_v1_13_R2;
import com.boydti.fawe.bukkit.v0.BukkitQueue_0;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.example.IntFaweChunk;
import com.boydti.fawe.jnbt.anvil.BitArray4096;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.RegionWrapper;
import com.boydti.fawe.object.brush.visualization.VisualChunk;
import com.boydti.fawe.object.visitor.FaweChunkVisitor;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.TaskManager;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockID;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import net.minecraft.server.v1_13_R2.*;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_13_R2.CraftChunk;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_13_R2.block.CraftBlock;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

public class BukkitQueue_1_13 extends BukkitQueue_0<net.minecraft.server.v1_13_R2.Chunk, ChunkSection[], ChunkSection> {

    final static Field fieldBits;
    final static Field fieldPalette;
    final static Field fieldSize;

    final static Field fieldHashBlocks;
    final static Field fieldLinearBlocks;
    private final static Field fieldHashIndex;
    final static Field fieldRegistryb;
    final static Field fieldRegistryc;
    final static Field fieldRegistryd;
    final static Field fieldRegistrye;
    final static Field fieldRegistryf;

    final static Field fieldLinearIndex;
    final static Field fieldDefaultBlock;

    private final static Field fieldFluidCount;
    final static Field fieldTickingBlockCount;
    final static Field fieldNonEmptyBlockCount;
    final static Field fieldSection;
    final static Field fieldLiquidCount;
    private final static ChunkSection emptySection;

    private final static Field fieldDirtyCount;
    private final static Field fieldDirtyBits;

    static {
        try {
            emptySection = new ChunkSection(0, true);
            Arrays.fill(emptySection.getSkyLightArray().asBytes(), (byte) 255);
            fieldSection = ChunkSection.class.getDeclaredField("blockIds");
            fieldLiquidCount = ChunkSection.class.getDeclaredField("e");
            fieldSection.setAccessible(true);
            fieldLiquidCount.setAccessible(true);

            fieldFluidCount = ChunkSection.class.getDeclaredField("e");
            fieldTickingBlockCount = ChunkSection.class.getDeclaredField("tickingBlockCount");
            fieldNonEmptyBlockCount = ChunkSection.class.getDeclaredField("nonEmptyBlockCount");
            fieldFluidCount.setAccessible(true);
            fieldTickingBlockCount.setAccessible(true);
            fieldNonEmptyBlockCount.setAccessible(true);

            fieldHashBlocks = DataPaletteHash.class.getDeclaredField("b");
            fieldHashBlocks.setAccessible(true);
            fieldLinearBlocks = DataPaletteLinear.class.getDeclaredField("b");
            fieldLinearBlocks.setAccessible(true);

            fieldHashIndex = DataPaletteHash.class.getDeclaredField("f");
            fieldHashIndex.setAccessible(true);

            fieldRegistryb = RegistryID.class.getDeclaredField("b");
            fieldRegistryc = RegistryID.class.getDeclaredField("c");
            fieldRegistryd = RegistryID.class.getDeclaredField("d");
            fieldRegistrye = RegistryID.class.getDeclaredField("e");
            fieldRegistryf = RegistryID.class.getDeclaredField("f");
            fieldRegistryb.setAccessible(true);
            fieldRegistryc.setAccessible(true);
            fieldRegistryd.setAccessible(true);
            fieldRegistrye.setAccessible(true);
            fieldRegistryf.setAccessible(true);

            fieldLinearIndex = DataPaletteLinear.class.getDeclaredField("f");
            fieldLinearIndex.setAccessible(true);

            fieldDefaultBlock = DataPaletteBlock.class.getDeclaredField("g");
            fieldDefaultBlock.setAccessible(true);

            fieldSize = DataPaletteBlock.class.getDeclaredField("i");
            fieldSize.setAccessible(true);

            fieldBits = DataPaletteBlock.class.getDeclaredField("a");
            fieldBits.setAccessible(true);

            fieldPalette = DataPaletteBlock.class.getDeclaredField("h");
            fieldPalette.setAccessible(true);

            fieldDirtyCount = PlayerChunk.class.getDeclaredField("dirtyCount");
            fieldDirtyBits = PlayerChunk.class.getDeclaredField("h");
            fieldDirtyCount.setAccessible(true);
            fieldDirtyBits.setAccessible(true);

            System.out.println("Using adapter: " + getAdapter());
            System.out.println("=========================================");
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public BukkitQueue_1_13(final com.sk89q.worldedit.world.World world) {
        super(world);
        getImpWorld();
    }

    public BukkitQueue_1_13(final String world) {
        super(world);
        getImpWorld();
    }

    private boolean save(net.minecraft.server.v1_13_R2.Chunk chunk, ChunkProviderServer cps) {
        cps.saveChunk(chunk, false);
        chunk.a(false);
        return true;
    }

    @Override
    public ChunkSection[] getSections(net.minecraft.server.v1_13_R2.Chunk chunk) {
        return chunk.getSections();
    }

    @Override
    public net.minecraft.server.v1_13_R2.Chunk loadChunk(World world, int x, int z, boolean generate) {
        ChunkProviderServer provider = ((CraftWorld) world).getHandle().getChunkProvider();
        if (generate) {
            return provider.getChunkAt(x, z, true, true);
        } else {
            return provider.getChunkAt(x, z, true, false);
        }
    }

    @Override
    public ChunkSection[] getCachedSections(World world, int cx, int cz) {
        net.minecraft.server.v1_13_R2.Chunk chunk = ((CraftWorld) world).getHandle().getChunkProvider().getChunkAt(cx, cz, false, false);
        if (chunk != null) {
            return chunk.getSections();
        }
        return null;
    }

    @Override
    public net.minecraft.server.v1_13_R2.Chunk getCachedChunk(World world, int cx, int cz) {
        return ((CraftWorld) world).getHandle().getChunkProvider().getChunkAt(cx, cz, false, false);
    }

    @Override
    public ChunkSection getCachedSection(ChunkSection[] chunkSections, int cy) {
        return chunkSections[cy];
    }

    @Override
    public void saveChunk(net.minecraft.server.v1_13_R2.Chunk chunk) {
        chunk.f(true); // Set Modified
        chunk.mustSave = true;
    }

    @Override
    public boolean regenerateChunk(World world, int x, int z, BiomeType biome, Long seed) {
        return super.regenerateChunk(world, x, z, biome, seed);
    }

    @Override
    public boolean setMCA(final int mcaX, final int mcaZ, final RegionWrapper allowed, final Runnable whileLocked, final boolean saveChunks, final boolean load) {
        throw new UnsupportedOperationException("Anvil not implemented yet");
//        TaskManager.IMP.sync(new RunnableVal<Boolean>() {
//            @Override
//            public void run(Boolean value) {
//                long start = System.currentTimeMillis();
//                long last = start;
//                synchronized (RegionFileCache.class) {
//                    World world = getWorld();
//                    if (world.getKeepSpawnInMemory()) world.setKeepSpawnInMemory(false);
//                    ChunkProviderServer provider = nmsWorld.getChunkProvider();
//
//                    boolean mustSave = false;
//                    boolean[][] chunksUnloaded = null;
//                    { // Unload chunks
//                        Iterator<net.minecraft.server.v1_13_R2.Chunk> iter = provider.a().iterator();
//                        while (iter.hasNext()) {
//                            net.minecraft.server.v1_13_R2.Chunk chunk = iter.next();
//                            if (chunk.locX >> 5 == mcaX && chunk.locZ >> 5 == mcaZ) {
//                                boolean isIn = allowed.isInChunk(chunk.locX, chunk.locZ);
//                                if (isIn) {
//                                    if (!load) {
//                                        mustSave |= saveChunks && save(chunk, provider);
//                                        continue;
//                                    }
//                                    iter.remove();
//                                    boolean save = saveChunks && chunk.a(false);
//                                    mustSave |= save;
//                                    provider.unloadChunk(chunk, save);
//                                    if (chunksUnloaded == null) {
//                                        chunksUnloaded = new boolean[32][];
//                                    }
//                                    int relX = chunk.locX & 31;
//                                    boolean[] arr = chunksUnloaded[relX];
//                                    if (arr == null) {
//                                        arr = chunksUnloaded[relX] = new boolean[32];
//                                    }
//                                    arr[chunk.locZ & 31] = true;
//                                }
//                            }
//                        }
//                    }
//                    if (mustSave) {
//                        provider.c(); // TODO only the necessary chunks
//                    }
//
//                    File unloadedRegion = null;
//                    if (load && !RegionFileCache.a.isEmpty()) {
//                        Map<File, RegionFile> map = RegionFileCache.a;
//                        Iterator<Map.Entry<File, RegionFile>> iter = map.entrySet().iterator();
//                        String requiredPath = world.getName() + File.separator + "region";
//                        while (iter.hasNext()) {
//                            Map.Entry<File, RegionFile> entry = iter.next();
//                            File file = entry.getKey();
//                            int[] regPos = MainUtil.regionNameToCoords(file.getPath());
//                            if (regPos[0] == mcaX && regPos[1] == mcaZ && file.getPath().contains(requiredPath)) {
//                                if (file.exists()) {
//                                    unloadedRegion = file;
//                                    RegionFile regionFile = entry.getValue();
//                                    iter.remove();
//                                    try {
//                                        regionFile.c();
//                                    } catch (IOException e) {
//                                        e.printStackTrace();
//                                    }
//                                }
//                                break;
//                            }
//                        }
//                    }
//
//                    long now = System.currentTimeMillis();
//                    if (whileLocked != null) whileLocked.run();
//                    if (!load) return;
//
//                    { // Load the region again
//                        if (unloadedRegion != null && chunksUnloaded != null && unloadedRegion.exists()) {
//                            final boolean[][] finalChunksUnloaded = chunksUnloaded;
//                            TaskManager.IMP.async(() -> {
//                                int bx = mcaX << 5;
//                                int bz = mcaZ << 5;
//                                for (int x = 0; x < finalChunksUnloaded.length; x++) {
//                                    boolean[] arr = finalChunksUnloaded[x];
//                                    if (arr != null) {
//                                        for (int z = 0; z < arr.length; z++) {
//                                            if (arr[z]) {
//                                                int cx = bx + x;
//                                                int cz = bz + z;
//                                                SetQueue.IMP.addTask(new Runnable() {
//                                                    @Override
//                                                    public void run() {
//                                                        net.minecraft.server.v1_13_R2.Chunk chunk = provider.getChunkAt(cx, cz, null, false);
//                                                        if (chunk != null) {
//                                                            PlayerChunk pc = getPlayerChunk(nmsWorld, cx, cz);
//                                                            if (pc != null) {
//                                                                sendChunk(pc, chunk, 0);
//                                                            }
//                                                        }
//                                                    }
//                                                });
//                                            }
//                                        }
//                                    }
//                                }
//                            });
//                        }
//                    }
//                }
//            }
//        });
//        return true;
    }

    @Override
    public boolean next(int amount, long time) {
        return super.next(amount, time);
    }

    @Override
    public void setSkyLight(ChunkSection section, int x, int y, int z, int value) {
        section.getSkyLightArray().a(x & 15, y & 15, z & 15, value);
    }

    @Override
    public void setBlockLight(ChunkSection section, int x, int y, int z, int value) {
        section.getEmittedLightArray().a(x & 15, y & 15, z & 15, value);
    }

    @Override
    public int getCombinedId4Data(ChunkSection lastSection, int x, int y, int z) {
        DataPaletteBlock<IBlockData> dataPalette = lastSection.getBlocks();
        IBlockData ibd = dataPalette.a(x & 15, y & 15, z & 15);
        int ordinal = ((Spigot_v1_13_R2) getAdapter()).adaptToInt(ibd);
        return BlockTypes.states[ordinal].getInternalId();
    }

    @Override
    public BiomeType getBiome(net.minecraft.server.v1_13_R2.Chunk chunk, int x, int z) {
        BiomeBase base = chunk.getBiomeIndex()[((z & 15) << 4) + (x & 15)];
        return getAdapter().adapt(CraftBlock.biomeBaseToBiome(base));
    }

    @Override
    public int getOpacity(ChunkSection section, int x, int y, int z) {
        DataPaletteBlock<IBlockData> dataPalette = section.getBlocks();
        IBlockData ibd = dataPalette.a(x & 15, y & 15, z & 15);
        pos.a(x, y, z);
        return ibd.b(nmsWorld, pos);
    }

    @Override
    public int getBrightness(ChunkSection section, int x, int y, int z) {
        DataPaletteBlock<IBlockData> dataPalette = section.getBlocks();
        IBlockData ibd = dataPalette.a(x & 15, y & 15, z & 15);
        return ibd.e();
    }

    @Override
    public int getOpacityBrightnessPair(ChunkSection section, int x, int y, int z) {
        DataPaletteBlock<IBlockData> dataPalette = section.getBlocks();
        IBlockData ibd = dataPalette.a(x & 15, y & 15, z & 15);
        pos.a(x, y, z);
        int opacity = ibd.b(nmsWorld, pos);
        int brightness = ibd.e();
        return MathMan.pair16(brightness, opacity);
    }

    @Override
    public void sendChunk(int x, int z, int bitMask) {
        net.minecraft.server.v1_13_R2.Chunk chunk = getCachedChunk(getWorld(), x, z);
        if (chunk != null) {
            sendChunk(getPlayerChunk((WorldServer) chunk.getWorld(), chunk.locX, chunk.locZ), chunk, bitMask);
        }
    }

    @Override
    public void sendChunkUpdatePLIB(FaweChunk chunk, FawePlayer... players) {
//        PlayerChunkMap playerManager = ((CraftWorld) getWorld()).getHandle().getPlayerChunkMap();
//        ProtocolManager manager = ProtocolLibrary.getProtocolManager();
//        WirePacket packet = null;
//        try {
//            for (int i = 0; i < players.length; i++) {
//                CraftPlayer bukkitPlayer = ((CraftPlayer) ((BukkitPlayer) players[i]).parent);
//                EntityPlayer player = bukkitPlayer.getHandle();
//
//                if (playerManager.a(player, chunk.getX(), chunk.getZ())) {
//                    if (packet == null) {
//                        byte[] data;
//                        byte[] buffer = new byte[8192];
//                        if (chunk instanceof LazyFaweChunk) {
//                            chunk = (FaweChunk) chunk.getChunk();
//                        }
//                        if (chunk instanceof MCAChunk) {
//                            data = new MCAChunkPacket((MCAChunk) chunk, true, true, hasSky()).apply(buffer);
//                        } else {
//                            data = new FaweChunkPacket(chunk, true, true, hasSky()).apply(buffer);
//                        }
//                        packet = new WirePacket(PacketType.Play.Server.MAP_CHUNK, data);
//                    }
//                    manager.sendWirePacket(bukkitPlayer, packet);
//                }
//            }
//        } catch (InvocationTargetException e) {
//            throw new RuntimeException(e);
//        }
        super.sendChunkUpdatePLIB(chunk, players); // TODO remove
    }

    @Override
    public void sendBlockUpdate(FaweChunk chunk, FawePlayer... players) {
        try {
            PlayerChunkMap playerManager = ((CraftWorld) getWorld()).getHandle().getPlayerChunkMap();
            boolean watching = false;
            boolean[] watchingArr = new boolean[players.length];
            for (int i = 0; i < players.length; i++) {
                EntityPlayer player = ((CraftPlayer) ((BukkitPlayer) players[i]).parent).getHandle();
                if (playerManager.a(player, chunk.getX(), chunk.getZ())) {
                    watchingArr[i] = true;
                    watching = true;
                }
            }
            if (!watching) return;
            final LongAdder size = new LongAdder();
            if (chunk instanceof VisualChunk) {
                size.add(((VisualChunk) chunk).size());
            } else if (chunk instanceof IntFaweChunk) {
                size.add(((IntFaweChunk) chunk).getTotalCount());
            } else {
                chunk.forEachQueuedBlock(new FaweChunkVisitor() {
                    @Override
                    public void run(int localX, int y, int localZ, int combined) {
                        size.add(1);
                    }
                });
            }
            if (size.intValue() == 0) return;
            PacketPlayOutMultiBlockChange packet = new PacketPlayOutMultiBlockChange();
            ByteBuf byteBuf = ByteBufAllocator.DEFAULT.buffer();
            final PacketDataSerializer buffer = new PacketDataSerializer(byteBuf);
            buffer.writeInt(chunk.getX());
            buffer.writeInt(chunk.getZ());
            buffer.d(size.intValue());
            chunk.forEachQueuedBlock(new FaweChunkVisitor() {
                @Override
                public void run(int localX, int y, int localZ, int combined) {
                    short index = (short) (localX << 12 | localZ << 8 | y);
                    if (combined < 16) combined = 0;
                    buffer.writeShort(index);
                    buffer.d(combined);
                }
            });
            packet.a(buffer);
            for (int i = 0; i < players.length; i++) {
                if (watchingArr[i]) ((CraftPlayer) ((BukkitPlayer) players[i]).parent).getHandle().playerConnection.sendPacket(packet);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void refreshChunk(FaweChunk fc) {
        sendChunk(fc.getX(), fc.getZ(), fc.getBitMask());
    }

    public void sendPacket(int cx, int cz, Packet packet) {
        PlayerChunk chunk = getPlayerChunk(nmsWorld, cx, cz);
        if (chunk != null) {
            for (EntityPlayer player : chunk.players) {
                player.playerConnection.sendPacket(packet);
            }
        }
    }

    private PlayerChunk getPlayerChunk(WorldServer w, int cx, int cz) {
        PlayerChunkMap chunkMap = w.getPlayerChunkMap();
        PlayerChunk playerChunk = chunkMap.getChunk(cx, cz);
        if (playerChunk == null) {
            return null;
        }
        if (playerChunk.players.isEmpty()) {
            return null;
        }
        return playerChunk;
    }

    private boolean sendChunk(PlayerChunk playerChunk, net.minecraft.server.v1_13_R2.Chunk nmsChunk, int mask) {
        if (playerChunk == null) {
            return false;
        }
        if (playerChunk.e()) {
            ChunkSection[] sections = nmsChunk.getSections();
            for (int layer = 0; layer < 16; layer++) {
                if (sections[layer] == null && (mask & (1 << layer)) != 0) {
                    sections[layer] = new ChunkSection(layer << 4, nmsWorld.worldProvider.g());
                }
            }
            TaskManager.IMP.sync(() -> {
                try {
                    int dirtyBits = fieldDirtyBits.getInt(playerChunk);
                    if (dirtyBits == 0) {
                        ((CraftWorld) getWorld()).getHandle().getPlayerChunkMap().a(playerChunk);
                    }
                    if (mask == 0) {
                        dirtyBits = 65535;
                    } else {
                        dirtyBits |= mask;
                    }

                    fieldDirtyBits.set(playerChunk, dirtyBits);
                    fieldDirtyCount.set(playerChunk, 64);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                return null;
            });
        }
        return true;
    }

    public boolean hasEntities(net.minecraft.server.v1_13_R2.Chunk nmsChunk) {
        try {
            final Collection<Entity>[] entities = nmsChunk.entitySlices;
            for (Collection<Entity> slice : entities) {
                if (slice != null && !slice.isEmpty()) {
                    return true;
                }
            }
        } catch (Throwable ignore) {}
        return false;
    }

    @Override
    public boolean removeSectionLighting(ChunkSection section, int layer, boolean sky) {
        if (section != null) {
            Arrays.fill(section.getEmittedLightArray().asBytes(), (byte) 0);
            if (sky) {
                byte[] light = section.getSkyLightArray().asBytes();
                if (light != null) {
                    Arrays.fill(light, (byte) 0);
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public void setFullbright(ChunkSection[] sections) {
        for (ChunkSection section : sections) {
            if (section != null) {
                byte[] bytes = section.getSkyLightArray().asBytes();
                Arrays.fill(bytes, (byte) 255);
            }
        }
    }

    @Override
    public int getSkyLight(ChunkSection section, int x, int y, int z) {
        return section.c(x & 15, y & 15, z & 15);
    }

    @Override
    public int getEmmittedLight(ChunkSection section, int x, int y, int z) {
        return section.d(x & 15, y & 15, z & 15);
    }

    @Override
    public void relightBlock(int x, int y, int z) {
        pos.c(x, y, z);
        nmsWorld.c(EnumSkyBlock.BLOCK, pos);
    }

    @Override
    public void relightSky(int x, int y, int z) {
        pos.c(x, y, z);
        nmsWorld.c(EnumSkyBlock.SKY, pos);
    }

    @Override
    public void relight(int x, int y, int z) {
        pos.c(x, y, z);
        nmsWorld.r(pos);
    }

    private WorldServer nmsWorld;

    @Override
    public World getImpWorld() {
        World world = super.getImpWorld();
        if (world != null) {
            this.nmsWorld = ((CraftWorld) world).getHandle();
            return super.getImpWorld();
        } else {
            return null;
        }
    }

    static void setCount(int tickingBlockCount, int nonEmptyBlockCount, ChunkSection section) throws NoSuchFieldException, IllegalAccessException {
        fieldFluidCount.set(section, 0); // TODO FIXME
        fieldTickingBlockCount.set(section, tickingBlockCount);
        fieldNonEmptyBlockCount.set(section, nonEmptyBlockCount);
    }

    int getNonEmptyBlockCount(ChunkSection section) throws IllegalAccessException {
        return (int) fieldNonEmptyBlockCount.get(section);
    }

    public void setPalette(ChunkSection section, DataPaletteBlock palette) throws NoSuchFieldException, IllegalAccessException {
        fieldSection.set(section, palette);
        Arrays.fill(section.getEmittedLightArray().asBytes(), (byte) 0);
    }

    static ChunkSection newChunkSection(int y2, boolean flag, int[] blocks) {
        if (blocks == null) {
            return new ChunkSection(y2 << 4, flag);
        } else {
            ChunkSection section = new ChunkSection(y2 << 4, flag);
            int[] blockToPalette = FaweCache.BLOCK_TO_PALETTE.get();
            int[] paletteToBlock = FaweCache.PALETTE_TO_BLOCK.get();
            long[] blockstates = FaweCache.BLOCK_STATES.get();
            int[] blocksCopy = FaweCache.SECTION_BLOCKS.get();
            try {
                int num_palette = 0;
                int air = 0;
                for (int i = 0; i < 4096; i++) {
                    int stateId = blocks[i];
                    switch (stateId) {
                        case 0:
                        case BlockID.AIR:
                        case BlockID.CAVE_AIR:
                        case BlockID.VOID_AIR:
                            stateId = BlockID.AIR;
                            air++;
                    }
                    int ordinal = BlockState.getFromInternalId(stateId).getOrdinal(); // TODO fixme Remove all use of BlockTypes.BIT_OFFSET so that this conversion isn't necessary
                    int palette = blockToPalette[ordinal];
                    if (palette == Integer.MAX_VALUE) {
                        blockToPalette[ordinal] = palette = num_palette;
                        paletteToBlock[num_palette] = ordinal;
                        num_palette++;
                    }
                    blocksCopy[i] = palette;
                }

                // BlockStates
                int bitsPerEntry = MathMan.log2nlz(num_palette - 1);
                if (Settings.IMP.PROTOCOL_SUPPORT_FIX || num_palette != 1) {
                    bitsPerEntry = Math.max(bitsPerEntry, 4); // Protocol support breaks <4 bits per entry
                } else {
                    bitsPerEntry = Math.max(bitsPerEntry, 1); // For some reason minecraft needs 4096 bits to store 0 entries
                }

                int blockBitArrayEnd = (bitsPerEntry * 4096) >> 6;
                if (num_palette == 1) {
                    for (int i = 0; i < blockBitArrayEnd; i++) blockstates[i] = 0;
                } else {
                    BitArray4096 bitArray = new BitArray4096(blockstates, bitsPerEntry);
                    bitArray.fromRaw(blocksCopy);
                }

                // set palette & data bits
                DataPaletteBlock<IBlockData> dataPaletteBlocks = section.getBlocks();
                // private DataPalette<T> h;
                // protected DataBits a;
                long[] bits = Arrays.copyOfRange(blockstates, 0, blockBitArrayEnd);
                DataBits nmsBits = new DataBits(bitsPerEntry, 4096, bits);
                DataPalette<IBlockData> palette = new DataPaletteLinear<>(Block.REGISTRY_ID, bitsPerEntry, dataPaletteBlocks, GameProfileSerializer::d);

                // set palette
                for (int i = 0; i < num_palette; i++) {
                    int ordinal = paletteToBlock[i];
                    blockToPalette[ordinal] = Integer.MAX_VALUE;
                    BlockState state = BlockTypes.states[ordinal];
                    IBlockData ibd = ((BlockMaterial_1_13) state.getMaterial()).getState();
                    palette.a(ibd);
                }
                try {
                    fieldBits.set(dataPaletteBlocks, nmsBits);
                    fieldPalette.set(dataPaletteBlocks, palette);
                    fieldSize.set(dataPaletteBlocks, bitsPerEntry);
                    setCount(0, 4096 - air, section);
                } catch (IllegalAccessException | NoSuchFieldException e) {
                    throw new RuntimeException(e);
                }

                return section;
            } catch (Throwable e){
                Arrays.fill(blockToPalette, Integer.MAX_VALUE);
                throw e;
            }
        }
    }

    protected BlockPosition.MutableBlockPosition pos = new BlockPosition.MutableBlockPosition(0, 0, 0);

    @Override
    public CompoundTag getTileEntity(net.minecraft.server.v1_13_R2.Chunk chunk, int x, int y, int z) {
        Map<BlockPosition, TileEntity> tiles = chunk.getTileEntities();
        pos.c(x, y, z);
        TileEntity tile = tiles.get(pos);
        return tile != null ? getTag(tile) : null;
    }

    CompoundTag getTag(TileEntity tile) {
        try {
            NBTTagCompound tag = new NBTTagCompound();
            tile.save(tag); // readTagIntoEntity
            return (CompoundTag) toNative(tag);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Deprecated
    public boolean unloadChunk(final String world, final Chunk chunk) {
        net.minecraft.server.v1_13_R2.Chunk c = ((CraftChunk) chunk).getHandle();
        c.mustSave = false;
        if (chunk.isLoaded()) {
            chunk.unload(false);
        }
        return true;
    }

    @Override
    public BukkitChunk_1_13 getFaweChunk(int x, int z) {
        return new BukkitChunk_1_13(this, x, z);
    }
}
