package com.boydti.fawe.bukkit.regions;

import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.regions.FaweMask;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.wasteofplastic.askyblock.ASkyBlockAPI;
import com.wasteofplastic.askyblock.Island;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public class ASkyBlockHook extends BukkitMaskManager implements Listener {

    public ASkyBlockHook(final Plugin aSkyBlock) {
        super(aSkyBlock.getName());

    }

    public boolean isAllowed(Player player, Island island, MaskType type) {
        return island != null && (player.getUniqueId().equals(island.getOwner()) || (type == MaskType.MEMBER && island.getMembers().contains(player.getUniqueId()) && hasMemberPermission(player)));
    }

    @Override
    public FaweMask getMask(final FawePlayer<Player> fp, MaskType type) {
        final Player player = fp.parent;
        final Location location = player.getLocation();

        Island island = ASkyBlockAPI.getInstance().getIslandAt(location);
        if (island != null && isAllowed(player, island, type)) {

            World world = location.getWorld();
            Location center = island.getCenter();
            Location pos1 = new Location(world, island.getMinProtectedX(), 0, island.getMinProtectedZ());
            Location pos2 = center.add(center.subtract(pos1));
            pos2.setY(255);

            return new FaweMask(BukkitAdapter.adapt(pos1).toBlockPoint(), BukkitAdapter.adapt(pos2).toBlockPoint()) {
                @Override
                public boolean isValid(FawePlayer player, MaskType type) {
                    return isAllowed((Player) player.parent, island, type);
                }
            };
        }

        return null;
    }
}
