/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.function.visitor;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.example.MappedFaweQueue;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.object.HasFaweQueue;
import com.boydti.fawe.object.IntegerTrio;
import com.boydti.fawe.object.collection.BlockVectorSet;

import static com.google.common.base.Preconditions.checkNotNull;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.function.RegionFunction;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.RunContext;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.MutableBlockVector3;
import com.sk89q.worldedit.util.Direction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Performs a breadth-first search starting from points added with
 * {@link #visit(BlockVector3)}. The search continues
 * to a certain adjacent point provided that the method
 * {@link #isVisitable(BlockVector3, BlockVector3)}
 * returns true for that point.
 *
 * <p>As an abstract implementation, this class can be used to implement
 * functionality that starts at certain points and extends outward from
 * those points.</p>
 */
public abstract class BreadthFirstSearch implements Operation {

    public static final BlockVector3[] DEFAULT_DIRECTIONS = new BlockVector3[6];
    public static final BlockVector3[] DIAGONAL_DIRECTIONS;

    static {
        DEFAULT_DIRECTIONS[0] = (BlockVector3.at(0, -1, 0));
        DEFAULT_DIRECTIONS[1] = (BlockVector3.at(0, 1, 0));
        DEFAULT_DIRECTIONS[2] = (BlockVector3.at(-1, 0, 0));
        DEFAULT_DIRECTIONS[3] = (BlockVector3.at(1, 0, 0));
        DEFAULT_DIRECTIONS[4] = (BlockVector3.at(0, 0, -1));
        DEFAULT_DIRECTIONS[5] = (BlockVector3.at(0, 0, 1));
        List<BlockVector3> list = new ArrayList<>();
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x != 0 || y != 0 || z != 0) {
                    	BlockVector3 pos = BlockVector3.at(x, y, z);
                        if (!list.contains(pos)) {
                            list.add(pos);
                        }
                    }
                }
            }
        }
        list.sort((o1, o2) -> (int) Math.signum(o1.lengthSq() - o2.lengthSq()));
        DIAGONAL_DIRECTIONS = list.toArray(new BlockVector3[0]);
    }

    private final RegionFunction function;
    private BlockVectorSet visited;
    private final MappedFaweQueue mFaweQueue;
    private BlockVectorSet queue;
    private int currentDepth = 0;
    private final int maxDepth;
    private List<BlockVector3> directions = new ArrayList<>();
    private int affected = 0;
    private int maxBranch = Integer.MAX_VALUE;

    /**
     * Create a new instance.
     *
     * @param function the function to apply to visited blocks
     */
    public BreadthFirstSearch(RegionFunction function) {
        this(function, Integer.MAX_VALUE);
        checkNotNull(function);
    }

    public BreadthFirstSearch(RegionFunction function, int maxDepth) {
        this(function, maxDepth, null);
        checkNotNull(function);

    }

    public BreadthFirstSearch(RegionFunction function, int maxDepth, HasFaweQueue faweQueue) {
        checkNotNull(function);
        FaweQueue fq = faweQueue != null ? faweQueue.getQueue() : null;
        this.mFaweQueue = fq instanceof MappedFaweQueue ? (MappedFaweQueue) fq : null;
        this.queue = new BlockVectorSet();
        this.visited = new BlockVectorSet();
        this.function = function;
        this.directions.addAll(Arrays.asList(DEFAULT_DIRECTIONS));
        this.maxDepth = maxDepth;
    }

    public void setDirections(List<BlockVector3> directions) {
        this.directions = directions;
    }

    private IntegerTrio[] getIntDirections() {
        IntegerTrio[] array = new IntegerTrio[directions.size()];
        for (int i = 0; i < array.length; i++) {
        	BlockVector3 dir = directions.get(i);
            array[i] = new IntegerTrio(dir.getBlockX(), dir.getBlockY(), dir.getBlockZ());
        }
        return array;
    }

    /**
     * Get the list of directions will be visited.
     *
     * <p>Directions are {@link BlockVector3}s that determine
     * what adjacent points area available. Vectors should not be
     * unit vectors. An example of a valid direction is
     * {@code BlockVector3.at(1, 0, 1)}.</p>
     *
     * <p>The list of directions can be cleared.</p>
     *
     * @return the list of directions
     */
    protected Collection<BlockVector3> getDirections() {
        return directions;
    }

    /**
     * Add the directions along the axes as directions to visit.
     */
    protected void addAxes() {
        directions.add(BlockVector3.UNIT_MINUS_Y);
        directions.add(BlockVector3.UNIT_Y);
        directions.add(BlockVector3.UNIT_MINUS_X);
        directions.add(BlockVector3.UNIT_X);
        directions.add(BlockVector3.UNIT_MINUS_Z);
        directions.add(BlockVector3.UNIT_Z);
    }

    /**
     * Add the diagonal directions as directions to visit.
     */
    protected void addDiagonal() {
        directions.add(Direction.NORTHEAST.toBlockVector());
        directions.add(Direction.SOUTHEAST.toBlockVector());
        directions.add(Direction.SOUTHWEST.toBlockVector());
        directions.add(Direction.NORTHWEST.toBlockVector());
    }

    /**
     * Add the given location to the list of locations to visit, provided
     * that it has not been visited. The position passed to this method
     * will still be visited even if it fails
     * {@link #isVisitable(BlockVector3, BlockVector3)}.
     *
     * <p>This method should be used before the search begins, because if
     * the position <em>does</em> fail the test, and the search has already
     * visited it (because it is connected to another root point),
     * the search will mark the position as "visited" and a call to this
     * method will do nothing.</p>
     *
     * @param position the position
     */
    public void visit(BlockVector3 position) {
        if (!visited.contains(position)) {
            BlockVector3 blockVector = position;
            isVisitable(blockVector, blockVector); // Ignore this, just to initialize mask on this point
            queue.add(blockVector);
            visited.add(blockVector);
        }
    }

    public void setVisited(BlockVectorSet set) {
        this.visited = set;
    }

    public BlockVectorSet getVisited() {
        return visited;
    }

    public boolean isVisited(BlockVector3 pos) {
        return visited.contains(pos);
    }

    public void setMaxBranch(int maxBranch) {
        this.maxBranch = maxBranch;
    }
    /**
     * Try to visit the given 'to' location.
     *
     * @param from the origin block
     * @param to the block under question
     */
    private void visit(BlockVector3 from, BlockVector3 to) {
        BlockVector3 blockVector = to;
        if (!visited.contains(blockVector)) {
            visited.add(blockVector);
            if (isVisitable(from, to)) {
                queue.add(blockVector);
            }
        }
    }

    /**
     * Return whether the given 'to' block should be visited, starting from the
     * 'from' block.
     *
     * @param from the origin block
     * @param to the block under question
     * @return true if the 'to' block should be visited
     */
    protected abstract boolean isVisitable(BlockVector3 from, BlockVector3 to);

    /**
     * Get the number of affected objects.
     *
     * @return the number of affected
     */
    public int getAffected() {
        return affected;
    }

    @Override
    public Operation resume(RunContext run) throws WorldEditException {
        MutableBlockVector3 mutable = new MutableBlockVector3();
        IntegerTrio[] dirs = getIntDirections();
        BlockVectorSet tempQueue = new BlockVectorSet();
        BlockVectorSet chunkLoadSet = new BlockVectorSet();
        for (currentDepth = 0; !queue.isEmpty() && currentDepth <= maxDepth; currentDepth++) {
            if (mFaweQueue != null && Settings.IMP.QUEUE.PRELOAD_CHUNKS > 1) {
                int cx = Integer.MIN_VALUE;
                int cz = Integer.MIN_VALUE;
                for (BlockVector3 from : queue) {
                    for (IntegerTrio direction : dirs) {
                        int x = from.getBlockX() + direction.x;
                        int z = from.getBlockZ() + direction.z;
                        if (cx != (cx = x >> 4) || cz != (cz = z >> 4)) {
                            int y = from.getBlockY() + direction.y;
                            if (y < 0 || y >= 256) {
                                continue;
                            }
                            if (!visited.contains(x, y, z)) {
                                chunkLoadSet.add(cx, 0, cz);
                            }
                        }
                    }
                }
                for (BlockVector3 chunk : chunkLoadSet) {
                    mFaweQueue.queueChunkLoad(chunk.getBlockX(), chunk.getBlockZ());
                }
            }
            for (BlockVector3 from : queue) {
                if (function.apply(from)) affected++;
                for (int i = 0, j = 0; i < dirs.length && j < maxBranch; i++) {
                    IntegerTrio direction = dirs[i];
                    int y = from.getBlockY() + direction.y;
                    if (y < 0 || y >= 256) {
                        continue;
                    }
                    int x = from.getBlockX() + direction.x;
                    int z = from.getBlockZ() + direction.z;
                    if (!visited.contains(x, y, z)) {
                        if (isVisitable(from, mutable.setComponents(x, y, z))) {
                            j++;
                            visited.add(x, y, z);
                            tempQueue.add(x, y, z);
                        }
                    }
                }
            }
            if (currentDepth == maxDepth) {
                break;
            }
            BlockVectorSet tmp = queue;
            queue = tempQueue;
            tmp.clear();
            chunkLoadSet.clear();
            tempQueue = tmp;

        }
        return null;
    }

    public int getDepth() {
        return currentDepth;
    }

    @Override
    public void addStatusMessages(List<String> messages) {
        messages.add(BBC.VISITOR_BLOCK.format(getAffected()));
    }

    @Override
    public void cancel() {
        queue.clear();
        visited.clear();
        affected = 0;
    }
}
