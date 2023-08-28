package cuboidnets.structure;

import java.util.Arrays;

public class CuboidTileRootKey {

    final TileLink tileLink;
    // we find the distance to the edge of the face in each direction
    final int[] key = new int[4];

    CuboidTileRootKey(TileLink tileLink) {
        this.tileLink = tileLink;
        // we measure the distance to the edge in each direction
        for (int i = 0; i < 4; ++i) {
            int j = 0;
            TileLink start = tileLink.turn(i);
            TileLink f = start;
            while ((f.tile.face == tileLink.tile.face) &&
                    (j == 0 || f != start)) {
                ++j;
                f = f.follow();
            }
            key[i] = j;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CuboidTileRootKey that = (CuboidTileRootKey) o;
        return Arrays.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(key);
    }
}
