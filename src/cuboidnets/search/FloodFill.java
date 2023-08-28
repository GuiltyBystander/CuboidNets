package cuboidnets.search;

import cuboidnets.structure.MetaTileLink;
import cuboidnets.structure.Tile;
import cuboidnets.structure.TileLink;

import java.util.*;

public class FloodFill implements Comparable<FloodFill> {


    static int floods = 0;
    final Set<Tile> tiles = new HashSet<>();
    final List<TileLink> inlets = new ArrayList<>();

    FloodFill(State state, Tile seed) {
        ++floods;
        Queue<Tile> open = new ArrayDeque<>();
        open.add(seed);
        while (!open.isEmpty()) {
            Tile t = open.poll();
            if (tiles.contains(t)) {
                continue;
            }
            tiles.add(t);


            for (TileLink link : t.links) {

                Tile neighbor = link.mirror.tile;
                if (state.tiles.containsKey(neighbor)) {
                    if (!state.isBanned(link.mirror)) {
                        inlets.add(link.mirror);
                    }
                } else {
                    if (!tiles.contains(neighbor)) {
                        open.add(neighbor);
                    }
                }
            }
        }
    }

    @Override
    public int compareTo(FloodFill o) {
        {
            int a = tiles.size();
            int b = o.tiles.size();
            if (a != b) {
                return a - b;
            }
        }
        {
            int a = inlets.size();
            int b = o.inlets.size();
            if (a != b) {
                return a - b;
            }
        }
        return 0;
    }

    public List<MetaTileLink> metaLoop(State state) {
        List<MetaTileLink> out = new ArrayList<>();
        for (TileLink f : inlets) {
            out.add(state.linkMap.get(f));
        }
        return out;
    }

    @Override
    public String toString() {
        return "{FF:inlets=%d tiles=%d}".formatted(inlets.size(), tiles.size());
    }
}
