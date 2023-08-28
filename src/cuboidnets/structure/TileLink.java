package cuboidnets.structure;

import java.util.Objects;

public class TileLink {
    // TODO consider just a winged edge data structure!!!  maybe not.   I like keeping the index thingy

    public final Tile tile;
    final int up; // direction leaving the tile.  todo, think about renaming
    public TileLink mirror; // will not be null at the end of setup.  I don't believe in getters/setters

    TileLink(Tile tile, int up) {
        this.tile = tile;
        this.up = up & 0b11;
    }

    public TileLink turn(int direction) {
        return tile.links[(up + direction) & 0b11];
    }

    TileLink follow() {
        return mirror.turn(2);
    }

    void link(TileLink other) {
        mirror = other;
        other.mirror = this;
    }

    @Override
    public String toString() {
        return "CuboidTileLink{t=%s, up=%d}".formatted(tile, up);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TileLink that = (TileLink) o;
        return up == that.up && Objects.equals(tile, that.tile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tile, up);
    }
}
