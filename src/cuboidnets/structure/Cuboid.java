package cuboidnets.structure;

import cuboidnets.Searchable;
import cuboidnets.Utility;
import cuboidnets.search.State;

import java.awt.image.BufferedImage;
import java.util.*;

import static cuboidnets.Utility.*;

public class Cuboid implements Searchable {
    /*
    0
    1234
    5
     */

    public final List<Tile> tiles = new ArrayList<>();
    public final Map<CuboidTileRootKey, TileLink> roots = new HashMap<>();
    final int x, y, z;
    final int flatW, flatH;
    final Face[] faces = new Face[6]; //might not need to keep this

    public Cuboid(int x, int y, int z) {
        // none of this has to be performant.
        // we shouldn't be running this code often.

        this.x = x;
        this.y = y;
        this.z = z;

        //if I want to reorient the cuboid, do it here instead of xyz
        int a = x;
        int b = y;
        int c = z;
        /*
           b  a  b  a
        a 000

          111 2 333 4
          111 2 333 4
        c 111 2 333 4
          111 2 333 4
          111 2 333 4

        a 555
         */
        flatW = b + a + b + a;
        flatH = a + c + a;
        // build the 6 faces
        faces[0] = new Face(this, 0, 0, b, a);
        faces[1] = new Face(this, 0, a, b, c);
        faces[2] = new Face(this, b, a, a, c);
        faces[3] = new Face(this, b + a, a, b, c);
        faces[4] = new Face(this, b + a + b, a, a, c);
        faces[5] = new Face(this, 0, a + c, b, a);

        // stitch the faces together
        for (int i = 0; i < 4; ++i) {
            /*
            top:    2310 side link
            side:   1234 index
            bottom: 0321 side link
            */
            faces[0].linkFace((i + 2) % 4, faces[i + 1], UP);
            faces[5].linkFace((4 - i) % 4, faces[i + 1], DOWN);
            faces[i + 1].linkFace(RIGHT, faces[((i + 1) % 4) + 1], LEFT);
        }

        // store all the tiles together
        for (Face f : faces) {
            tiles.addAll(f.tiles);
        }

        // pick out the starting search spots
        for (Tile t : tiles) {
            for (TileLink link : t.links) {
                CuboidTileRootKey key = new CuboidTileRootKey(link);
                if (!roots.containsKey(key)) {
                    roots.put(key, link);
                }
            }
        }
    }

    @Override
    public String toString() {
        return "Cuboid{%d, %d, %d}".formatted(x, y, z);
    }

    @Override
    public Collection<TileLink> getRoots() {
        return roots.values();
    }

    @Override
    public Collection<Tile> getTiles() {
        return tiles;
    }

    public BufferedImage render(State state) {
        return Utility.render(state, tiles, false);
    }
}
