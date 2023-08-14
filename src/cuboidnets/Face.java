package cuboidnets;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static cuboidnets.Utility.*;

public class Face implements Searchable {

    final Cuboid cuboid;
    final int flatX, flatY, w, h;
    final List<Tile> tiles = new ArrayList<>();
    private final Tile[][] field;

    Face(Cuboid cuboid, int flatX, int flatY, int w, int h) {
        this.cuboid = cuboid;
        this.flatX = flatX;
        this.flatY = flatY;
        this.w = w;
        this.h = h;
        field = new Tile[w][h];
        for (int i = 0; i < w; ++i) {
            for (int j = 0; j < h; ++j) {
                Tile t = new Tile(this, flatX + i, flatY + j);
                field[i][j] = t;
                tiles.add(t);
            }
        }

        // tie them together in a donut, fix up later
        for (int i = 0; i < w; ++i) {
            for (int j = 0; j < h; ++j) {
                Tile a = field[i][j];
                Tile b = field[i][(j + 1) % h];
                linkTiles(a, b, DOWN, UP);
            }
        }
        for (int j = 0; j < h; ++j) {
            for (int i = 0; i < w; ++i) {
                Tile a = field[i][j];
                Tile b = field[(i + 1) % w][j];
                linkTiles(a, b, RIGHT, LEFT);
            }
        }
    }


    private void linkTiles(Tile a, Tile b, int i, int j) {
        a.links[i].link(b.links[j]);
    }

    void linkFace(int thisSide, Face other, int otherSide) {
        Tile[] a = getSideTiles(thisSide, false);
        Tile[] b = other.getSideTiles(otherSide, true);

        for (int i = 0; i < a.length; ++i) {
            linkTiles(a[i], b[i], thisSide, otherSide);
        }
    }

    private Tile[] getSideTiles(int side, boolean reverse) {
        int size, x, y, dx = 0, dy = 0;
        switch (side & 0b11) {
            case UP -> {
                size = w;
                x = 0;
                y = 0;
                dx = 1;
            }
            case LEFT -> {
                size = h;
                x = 0;
                y = h - 1;
                dy = -1;
            }
            case DOWN -> {
                size = w;
                x = w - 1;
                y = h - 1;
                dx = -1;
            }
            case RIGHT -> {
                size = h;
                x = w - 1;
                y = 0;
                dy = 1;
            }
            default -> throw new IllegalStateException("Unexpected value: " + (side & 0b11));
        }
        if (reverse) {
            x = x + (size - 1) * dx;
            y = y + (size - 1) * dy;
            dx *= -1;
            dy *= -1;
        }
        Tile[] out = new Tile[size];
        for (int i = 0; i < size; ++i) {
            out[i] = field[x + dx * i][y + dy * i];
        }
        return out;
    }

    @Override
    public String toString() {
        return "%s".formatted(cuboid);
    }

    @Override
    public Collection<TileLink> getRoots() {
        List<TileLink> out = new ArrayList<>();
        //out.add(field[w / 2][h / 2].links[0]);
        out.add(field[0][0].links[0]);
        return out;
    }

    @Override
    public Collection<Tile> getTiles() {
        return tiles;
    }

    @Override
    public BufferedImage render(SearchState state) {
        return Utility.render(state, tiles, true);
    }
}
