package cuboidnets.search;

import cuboidnets.Searchable;
import cuboidnets.structure.Cuboid;
import cuboidnets.structure.Face;
import cuboidnets.structure.MetaTileLink;
import cuboidnets.structure.TileLink;

import java.util.*;

public class Workload {

    public final Results allResults;
    public final Cuboid[] nets;
    public final int numNets;
    public final int numTiles;
    public final boolean randomExplore;
    public final PriorityQueue<State> queueWork = new PriorityQueue<>();
    public final List<State> activeWork = new ArrayList<>();

    public Workload(Cuboid[] nets) {
        this.nets = nets;
        numNets = nets.length;
        numTiles = nets[0].tiles.size();

        allResults = new Results(nets[0]);
        allResults.endTime = allResults.startTime;

        randomExplore = nets.length > 4;

        if (!randomExplore) {
            queueWork.addAll(getSeeds());
        }
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

    public static Workload load(String filename) {
        //TODO... using java's serialization didn't work.
        return null;
    }

    private List<State> getSeeds() {
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
        for (Searchable net : allNets) {
            if (net == largest) {
                Collection<TileLink> single = new ArrayList<>();
                single.add(net.getRoots().iterator().next());
                roots.add(single);
            } else {
                roots.add(net.getRoots());
            }
        }

        List<MetaTileLink> allRoots = buildRoots(roots);
        List<State> out = new ArrayList<>();
        for (MetaTileLink r : allRoots) {
            out.add(new State(allNets, r, numTiles));
        }
        return out;
    }

    private State randomSeed() {
        Searchable[] allNets = new Searchable[nets.length + 1];
        allNets[0] = new Face(null, 0, 0, numTiles, numTiles);
        System.arraycopy(nets, 0, allNets, 1, nets.length);

        TileLink[] links = new TileLink[allNets.length];
        for (int i = 0; i < allNets.length; ++i) {
            List<TileLink> r = new ArrayList<>(allNets[i].getRoots());
            int j = (int) (Math.random() * r.size());
            links[i] = r.get(j);
        }

        MetaTileLink r = new MetaTileLink(links);
        return new State(allNets, r, numTiles);
    }

    public synchronized State getWork(Results results) {
        //todo, store results as a handle to stop worker thread later
        if (randomExplore && queueWork.isEmpty()) {
            queueWork.add(randomSeed());
        }

        if (queueWork.isEmpty()) {
            return null;
        }

        State s = queueWork.poll();
        activeWork.add(s);
        return s;
    }

    public synchronized void returnWork(State s, Results results) {
        // add unfinished work to the log
        queueWork.addAll(results.incomplete);
        results.incomplete.clear();

        // accumulate results
        allResults.add(results);

        // mark that the work isn't outstanding
        activeWork.remove(s);
    }

    public synchronized boolean hasWork() {
        return randomExplore || !queueWork.isEmpty();
    }

    public synchronized boolean isDone() {
        return !hasWork() && activeWork.isEmpty();
    }

    @Override
    public String toString() {
        int[] depths = new int[numTiles + 1];
        for (var s : queueWork) {
            ++depths[s.depth];
        }
        depths[numTiles] += allResults.finished.size();
        depths[0] += allResults.flatFinished.size();


        return "queue:%s working:%s qdepths:%s".formatted(queueWork.size(), activeWork.size(), Arrays.toString(depths));
    }

    public void save(String filename) {
        //TODO... using java's serialization didn't work.
    }

    public synchronized void printProgress() {
        System.out.printf("all depths: %s %s%n", Arrays.toString(allResults.depthCount), (System.nanoTime() - allResults.startTime) / 1000_000_000.);
        //System.out.printf("solutions:%d unique:%d %s whole%n", work.allResults.finished.size(), work.allResults.flatFinished.size(), (System.nanoTime() - start) / 1000_000_000.);
        System.out.println(this);
        System.out.println();
    }
}
