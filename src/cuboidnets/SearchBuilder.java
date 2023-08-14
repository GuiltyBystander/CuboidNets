package cuboidnets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

// todo, turn into an entire search manager
public class SearchBuilder {

    private SearchBuilder() {
    }

    static SearchState randomSeed(Cuboid[] nets) {
        int numTiles = nets[0].tiles.size();

        Searchable[] allNets = new Searchable[nets.length + 1];
        allNets[0] = new Face(null, 0, 0, numTiles, numTiles);
        for (int i = 0; i < nets.length; ++i) {
            allNets[i + 1] = nets[i];
        }

        TileLink[] links = new TileLink[allNets.length];
        for (int i = 0; i < allNets.length; ++i) {
            List<TileLink> r = new ArrayList<>(allNets[i].getRoots());
            int j = (int) (Math.random() * r.size());
            links[i] = r.get(j);
        }

        MetaTileLink r = new MetaTileLink(links);
        return new SearchState(allNets, r, numTiles);
    }

    static List<SearchState> getSeeds(Cuboid[] nets) {
        int numTiles = nets[0].tiles.size();

        Searchable[] allNets = new Searchable[nets.length + 1];
        allNets[0] = new Face(null, 0, 0, numTiles + 20, numTiles + 20);
        Searchable largest = allNets[0];
        for (int i = 0; i < nets.length; ++i) {
            allNets[i + 1] = nets[i];
            if (largest.getRoots().size() < nets[i].getRoots().size()) {
                largest = nets[i];
            }
        }

        List<Collection<TileLink>> roots = new ArrayList<>();
        for (int i = 0; i < allNets.length; ++i) {
            if (allNets[i] == largest) {
                Collection<TileLink> single = new ArrayList<>();
                single.add(allNets[i].getRoots().iterator().next());
                roots.add(single);
            } else {
                roots.add(allNets[i].getRoots());
            }
            //System.out.println(roots.get(i).size());
        }

        List<MetaTileLink> allRoots = buildRoots(roots);
        List<SearchState> out = new ArrayList<>();
        for (MetaTileLink r : allRoots) {
            out.add(new SearchState(allNets, r, numTiles));
        }
        return out;
    }

    private static List<MetaTileLink> buildRoots(List<Collection<TileLink>> roots) {
        if (roots.size() <= 1) {
            List<MetaTileLink> out = new ArrayList<>();
            for (TileLink r : roots.get(0)) {
                out.add(new MetaTileLink(new TileLink[]{r}));
            }
            return out;
        }

        Collection<TileLink> root = roots.remove(roots.size() - 1);
        List<MetaTileLink> in = buildRoots(roots);
        List<MetaTileLink> out = new ArrayList<>();
        for (MetaTileLink i : in) {
            for (TileLink r : root) {
                out.add(new MetaTileLink(i, r));
            }
        }
        return out;
    }
}
