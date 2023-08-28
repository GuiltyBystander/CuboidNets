package cuboidnets.structure;

public class Tile {

    public final TileLink[] links = new TileLink[4];
    public final int flatX, flatY;
    final Face face;

    Tile(Face face, int flatX, int flatY) {
        this.face = face;
        this.flatX = flatX;
        this.flatY = flatY;
        for (int i = 0; i < 4; ++i) {
            links[i] = new TileLink(this, i);
        }
    }

    @Override
    public String toString() {
        return "Tile{flatXY=%d,%d, face=%s}".formatted(flatX, flatY, face);
    }
}
