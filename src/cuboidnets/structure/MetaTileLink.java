package cuboidnets.structure;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

public class MetaTileLink {
    public final TileLink[] links;
    final int n;

    public MetaTileLink(TileLink[] links) {
        n = links.length;
        this.links = links;
    }

    public MetaTileLink(MetaTileLink mtl, TileLink l) {
        n = mtl.n + 1;
        links = Arrays.copyOf(mtl.links, n);
        links[n - 1] = l;
    }

    public Tile[] tiles() {
        Tile[] tiles = new Tile[n];
        for (int i = 0; i < n; ++i) {
            tiles[i] = links[i].tile;
        }
        return tiles;
    }

    public MetaTileLink turn(int direction) {
        direction &= 0b11;
        if (direction == 0) {
            return this;
        }

        TileLink[] turned = new TileLink[n];
        for (int i = 0; i < n; ++i) {
            turned[i] = links[i].turn(direction);
        }
        return new MetaTileLink(turned);
    }

    public MetaTileLink mirror() {
        TileLink[] mirrored = new TileLink[n];
        for (int i = 0; i < n; ++i) {
            mirrored[i] = links[i].mirror;
        }
        return new MetaTileLink(mirrored);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MetaTileLink that = (MetaTileLink) o;
        return n == that.n && Arrays.equals(links, that.links);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(n);
        result = 31 * result + Arrays.hashCode(links);
        return result;
    }

    public boolean uses(Map<Tile, Integer> tiles) {
        for (TileLink link : links) {
            if (tiles.containsKey(link.tile)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return Arrays.toString(links);
    }
}
