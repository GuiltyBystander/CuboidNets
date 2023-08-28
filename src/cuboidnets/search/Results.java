package cuboidnets.search;

import cuboidnets.Utility;
import cuboidnets.structure.Cuboid;
import cuboidnets.structure.DenseFlatNet;

import java.util.*;

public class Results {

    public final int[] depthCount;
    public final State[] example;
    public final Collection<State> incomplete = new ArrayList<>();
    public final Collection<State> finished = new ArrayList<>();//TODO: stop using this
    public final Set<DenseFlatNet> flatFinished = new HashSet<>();
    public final long startTime = System.nanoTime();
    final int maxTiles;
    long endTime = startTime + 10 * 1_000_000_000L;
    int cnt = 0;
    int bestDepth = 0;
    //todo, do some kind of elapsed time tracker for the aggregator version

    public Results(int maxTiles) {
        this.maxTiles = maxTiles;
        depthCount = new int[maxTiles + 1];
        example = new State[maxTiles + 1];
    }

    public Results(Cuboid cuboid) {
        this(cuboid.tiles.size());
    }

    public Results(Cuboid[] cuboids) {
        this(cuboids[0].tiles.size());
    }

    public Results(State state) {
        this(state.maxTiles);
    }

    public Results(Workload work) {
        this(work.nets);
    }


    /**
     * @param state
     * @return Returns true if the search should stop here.  This includes:
     * - dead. usually from cutting off unreachable tiles
     * - timeout.  time to check back in with the main thread to report progress. 1 minute
     * - solved.  the net is complete
     */
    boolean reportProgress(State state) {
        //todo, add parameter for max depth
        if (state.dead) {
            return true;
        }

        if ((cnt > 10) && System.nanoTime() > endTime) {
            incomplete.add(state);
            return true;
        }

        ++cnt;
        ++depthCount[state.depth];
        if (example[state.depth] == null) {
            example[state.depth] = state;
        }
        if (bestDepth < state.depth) {
            bestDepth = state.depth;
        }


        if (state.isComplete()) {
            DenseFlatNet flat = DenseFlatNet.fromState(state);
            flatFinished.add(flat);
            finished.add(state);
            return true;
        }

        if (cnt % 100000 == 0) {
            System.out.printf("%s %s%n", Arrays.toString(depthCount), (System.nanoTime() - startTime) / 1000_000_000.);
            // loopCountTest();
            // Utility.saveImage(render(), "pic/" + maxTiles, "aaa-newest");
        }
        return false;
    }

    public void add(Results other) {
        for (int i = 0; i < depthCount.length; ++i) {
            depthCount[i] += other.depthCount[i];
            if (example[i] == null && other.example[i] != null) {
                example[i] = other.example[i];
                Utility.saveImage(example[i].render(), "pic/" + maxTiles, String.valueOf(i));
            }
        }
        cnt += other.cnt;
        bestDepth = Math.max(bestDepth, other.bestDepth);

        incomplete.addAll(other.incomplete);
        finished.addAll(other.finished);
        //flatFinished.addAll(other.flatFinished);
        for (var f : other.flatFinished) {
            if (flatFinished.add(f)) {
                Utility.saveImage(f.render(), "pic/" + maxTiles + "/flat", "f" + (flatFinished.size()));
            }
        }

        endTime += System.nanoTime() - other.startTime;
    }
}
