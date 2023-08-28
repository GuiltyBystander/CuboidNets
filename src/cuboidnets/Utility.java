package cuboidnets;

import cuboidnets.search.State;
import cuboidnets.structure.Tile;
import cuboidnets.structure.TileLink;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;

public class Utility {
    //todo: consider enum
    public static final int cores = Runtime.getRuntime().availableProcessors() - 2;
    public static final int UP = 0;
    public static final int LEFT = 1;
    public static final int DOWN = 2;
    public static final int RIGHT = 3;
    static int saveProtection = 0;


    private Utility() {
    }

    public static void saveImage(BufferedImage bi, String folderName, String name) {
        if (saveProtection++ > 5000) {
            return;
        }

        try {
            Files.createDirectories(Paths.get(folderName));
            // retrieve image
            File outputfile = new File(String.format("%s\\%s.png", folderName, name));
            ImageIO.write(bi, "png", outputfile);
        } catch (IOException e) {
            System.err.println("failed to save image");
        }
    }

    public static int[] calcOffset(State state, Collection<Tile> tiles, boolean includeAdjacent) {
        tiles.removeIf(tile -> {
            if (state.tiles.containsKey(tile)) {
                return false;
            }
            if (includeAdjacent) {
                for (var link : tile.links) {
                    Tile t2 = link.mirror.tile;
                    if (state.tiles.containsKey(t2)) {
                        return false;
                    }
                }
            }
            return true;
        });


        int flatW = 0, flatH = 0;
        for (Tile t : tiles) {
            flatW = Math.max(t.flatX + 1, flatW);
            flatH = Math.max(t.flatY + 1, flatH);
        }

        boolean[] countX = new boolean[flatW];
        boolean[] countY = new boolean[flatH];
        for (Tile t : tiles) {
            countX[t.flatX] = true;
            countY[t.flatY] = true;
        }

        int x = flatW, y = flatH;
        while (x > 0 && countX[x - 1]) {
            --x;
        }
        while (y > 0 && countY[y - 1]) {
            --y;
        }
        x = flatW - x;
        y = flatH - y;


        return new int[]{x, y, flatW, flatH};
    }

    public static int[] remap(Tile tile, int[] offset) {
        return new int[]{
                (offset[0] + tile.flatX) % offset[2],
                (offset[1] + tile.flatY) % offset[3],
        };
    }

    public static BufferedImage render(State state, Collection<Tile> tiles, boolean recenter) {
        int[] offset = {0, 0, 9999, 9999};
        if (recenter) {
            tiles = new ArrayList<>(tiles);
            offset = calcOffset(state, tiles, true);
        }


        //todo: option to hide tiles not connected. only for the net. we want to show all tiles on the cuboids.
        //TODO: maybe some kind of bounding box for the net which is massively oversized
        int flatW = 0, flatH = 0;
        for (Tile t : tiles) {
            var xy = remap(t, offset);
            flatW = Math.max(xy[0] + 1, flatW);
            flatH = Math.max(xy[1] + 1, flatH);
        }


        int scale = 10;
        BufferedImage bi = new BufferedImage((flatW + 2) * scale, (flatH + 2) * scale, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = bi.createGraphics();

        for (Tile t : tiles) {
            var xy = remap(t, offset);
            int x = (xy[0] + 1) * scale;
            int y = (xy[1] + 1) * scale;
            Shape s = new Rectangle(x, y, scale, scale);

            if (state.tiles.containsKey(t)) {
                float hue = 1f * (state.tiles.get(t) - 1) / state.maxTiles;
                g2.setColor(Utility.getColor(hue));
                g2.fill(s);
            } else {
                int[] dx = {scale / 2 - 1, 0, scale / 2 - 1, scale - 2};
                int[] dy = {0, scale / 2 - 1, scale - 2, scale / 2 - 1};
                for (int i = 0; i < 4; ++i) {
                    //TileLink l = t.links[i].mirror;
                    //MetaTileLink ml = state.linkMap.get(l);
                    //if (ml != null && state.bansLink.contains(l)) {
                    if (state.bansLink.contains(t.links[i])) {
                        g2.setColor(Color.gray);
                        g2.fillRect(x + dx[i], y + dy[i], 3, 3);
                    } else if (state.isBanned(t.links[i].mirror)) {
                        g2.setColor(Color.red);
                        g2.fillRect(x + dx[i], y + dy[i], 3, 3);
                    }
                }
            }

            g2.setColor(Color.white);
            g2.draw(s);
        }
        return bi;
    }

    static Color getColor(float hue) {
        return Color.getHSBColor(.9f * hue, 1, 1);
    }


    public static TileLink linkBackHome(State state, TileLink justBanned) {
        // input just banned is to be from a tile pointing into unmapped tiles
        //     if it is mapped, just return it immediately
        // return will be a link from mapped pointing to an unmapped tile.

        TileLink start = justBanned.mirror;
        if (state.tiles.containsKey(start.tile)) {
            // if justBanned points to a tile, just return.
            return justBanned;
        }

        // I originally thought I'd want to look both ways, but I don't see why anymore.  Just does right hand search
        int[] rightSearch = {LEFT, DOWN, RIGHT, UP};
        int[] leftSearch = {RIGHT, DOWN, LEFT, UP};
        boolean rightHand = true;
        int[] searchDirection = rightHand ? rightSearch : leftSearch;


        TileLink pointer = start;

        while (true) {
            for (int i : searchDirection) {
                TileLink turn = pointer.turn(i);
                //if turn is the start pointer, return failure.  we have no way home
                if (turn == start) {
                    // todo: if this isn't banned, it means our only way out is the way we came in
                    if (!state.bansLink.contains(turn)) {
                        return justBanned;// todo rename if we're not using only for banned inputs
                    }
                    return null;
                }

                // if turn is banned, continue
                if (state.bansLink.contains(turn)) {
                    continue;
                }

                //if turn points to a tile, continue, yay, we win,
                if (state.tiles.containsKey(turn.mirror.tile)) {
                    return turn.mirror;
                }

                //else, move on to the next tile
                pointer = turn.mirror;
                break;
            }
            //todo: double check that we never fall out the bottom of the loop besides the break
        }
    }
}
