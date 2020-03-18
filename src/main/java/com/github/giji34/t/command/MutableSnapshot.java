package com.github.giji34.t.command;

import com.github.giji34.t.Loc;
import org.bukkit.Server;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class MutableSnapshot implements Snapshot {
    final BlockRange range;
    @Nullable String errorMessage;
    final String[] blockData;

    MutableSnapshot(BlockRange range) {
        this.range = range;
        this.blockData = new String[range.volume()];
    }

    void setErrorMessage(@NotNull String s) {
        this.errorMessage = s;
    }

    void setBlockData(int x, int y, int z, String blockData) {
        final int idx = getIndex(x, y, z);
        if (0 <= idx && idx < this.blockData.length) {
            this.blockData[idx] = blockData;
        }
    }

    int getIndex(int x, int y, int z) {
        final int dx = range.getMaxX() - range.getMinX() + 1;
        final int dz = range.getMaxZ() - range.getMinZ() + 1;
        return dx * dz * (y - range.getMinY()) + dx * (z - range.getMinZ()) + (x - range.getMinX());
    }

    @Override
    public @NotNull BlockRange getRange() {
        return range;
    }

    @Override
    public @Nullable BlockData blockAt(Loc loc, Server server) {
        final int idx = getIndex(loc.x, loc.y, loc.z);
        if (0 <= idx && idx < this.blockData.length) {
            final String bd = this.blockData[idx];
            if (bd == null) {
                return  null;
            }
            return server.createBlockData(bd);
        } else {
            return null;
        }
    }

    @Override
    public @Nullable String getErrorMessage() {
        return errorMessage;
    }
}
