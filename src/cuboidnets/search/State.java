package cuboidnets.search;

import cuboidnets.Searchable;
import cuboidnets.Utility;
import cuboidnets.structure.MetaTileLink;
import cuboidnets.structure.Tile;
import cuboidnets.structure.TileLink;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;

public class State implements Comparable<State> {
    //TODO, simplify this list of member variables so it's easier to save
    public final List<MetaTileLink> links = new ArrayList<>(); //all the potential links //todo: consider making `links` a set
    public final Map<Tile, Integer> tiles = new HashMap<>(); // all the tiles we've used and what depth
    public final int maxTiles; // total tiles on the cuboid
    public final Set<TileLink> bansLink = new HashSet<>();
    public final Searchable[] allNets; // first is a flat net, rest are cuboids
    final MetaTileLink root;
    final Map<TileLink, MetaTileLink> linkMap = new HashMap<>();
    final Set<MetaTileLink> bansMeta = new HashSet<>();
    final PriorityQueue<FloodFill> loops = new PriorityQueue<>();
    final Set<FloodFill> oldLoops = new HashSet<>();
    final Map<TileLink, FloodFill> testedForLoops = new HashMap<>();
    int depth; //basically final, but technically not
    boolean dead = false; // if true, the cuboid is unsolvable
    List<MetaTileLink> bestLoop = null;

    State(Searchable[] allNets, MetaTileLink root, int maxTiles) {
        this.allNets = allNets;
        this.depth = 1;
        this.root = root;
        this.maxTiles = maxTiles;

        for (Tile tile : root.tiles()) {
            tiles.put(tile, depth);
        }

        for (int i = 0; i < 4; ++i) {
            MetaTileLink metaLink = root.turn(i);
            ingestLink(metaLink);
        }

        bestLoop = links;
    }

    private State(State parent, MetaTileLink step) {
        this.allNets = parent.allNets;
        this.depth = parent.depth;
        this.root = parent.root;
        this.maxTiles = parent.maxTiles;
        this.tiles.putAll(parent.tiles);
        this.links.addAll(parent.links);
        this.linkMap.putAll(parent.linkMap);
        this.bansMeta.addAll(parent.bansMeta);
        this.bansLink.addAll(parent.bansLink);
        this.loops.addAll(parent.loops);
        this.oldLoops.addAll(parent.oldLoops);
        this.testedForLoops.putAll(parent.testedForLoops);
        this.dead = parent.dead;

        makeStep(step);
    }

    private void makeStep(MetaTileLink step) {
        depth += 1;
        // new step's mirror will contain the tile that we're adding
        MetaTileLink mirror = step.mirror();

        // add the new tile
        for (Tile tile : mirror.tiles()) {
            tiles.put(tile, depth);
        }

        //for each possible step off of new tile
        for (int i = 0; i < 4; ++i) {
            MetaTileLink nextStep = mirror.turn(i);

            // add the metaLink as a valid step to take
            ingestLink(nextStep);

            // check the mirror to see what tiles it might point to
            MetaTileLink nextStepMirror = nextStep.mirror();

            // If any of the parts of the mirror...
            for (TileLink l : nextStepMirror.links) {
                // Point to an old tile
                if (tiles.containsKey(l.tile)) {
                    //ban next step
                    ban(nextStep, false);
                    //ban the offending mirror
                    ban(linkMap.get(l), false);
                }
            }
        }

        for (TileLink tl : step.links) {
            if (testedForLoops.containsKey(tl)) {
                FloodFill ff = testedForLoops.get(tl);
                loops.remove(ff);
                oldLoops.add(ff);
                for (TileLink inlet : ff.inlets) {
                    testedForLoops.remove(inlet);
                }
            }
        }

        if (isComplete()) {
            return;
        }

        links.removeIf(this::isBanned);
        findSmallestLoop();
        //findSmallestArea();
        //findSmallestArea2();
        if (false) {
            //finding loops by area seemed always worse.  perhaps revisit later
            if (loops.size() > 3 &&
                    bestLoop != null &&
                    bestLoop.size() > 1) {
                System.out.println(depth + " " + bestLoop.size() + " " + loops);
                Utility.saveImage(render(), "pic/" + maxTiles, "aaa-dead");
            }
        }

        // if only one entrance to loop, take it
        if (!dead && bestLoop.size() == 1) {
            makeStep(bestLoop.get(0));
        }
    }

    private void findSmallestArea2() {
        if (dead) {
            return;
        }
        // if an edge gets banned before it gets tested, that's bad, but it should also be recognized as a failure then too
        for (MetaTileLink mtl : links) {
            if (testedForLoops.containsKey(mtl.links[1])) {
                continue;
            }
            boolean isCuboid = false;
            for (TileLink tl : mtl.links) {
                if (!isCuboid) {
                    isCuboid = true;
                    continue;
                }


                FloodFill ff = new FloodFill(this, tl.mirror.tile);
                loops.add(ff);
                for (TileLink inlet : ff.inlets) {
                    testedForLoops.put(inlet, ff);
                }
            }
        }

        if (loops.isEmpty()) {
            dead = true;
        } else {
            bestLoop = loops.peek().metaLoop(this);
        }
        // TODO:::!!!! when making a move , need to pull/ban out all the loops
    }

    private void findSmallestArea() {
        bestLoop = links;
        for (int i = 1; i < allNets.length && bestLoop.size() > 1; ++i) {
            Set<MetaTileLink> tested = new HashSet<>();

            for (MetaTileLink link : links) {
                if (tested.contains(link)) {
                    continue;
                }

                TileLink start = link.links[i];
                List<MetaTileLink> currentLoop = new ArrayList<>();

                for (TileLink f : new FloodFill(this, start.mirror.tile).inlets) {
                    currentLoop.add(linkMap.get(f));
                }


                if (bestLoop.size() > currentLoop.size()) {
                    bestLoop = currentLoop;
                }

                tested.addAll(currentLoop);
            }
        }
    }

    private void findSmallestLoop() {
        // Future ideas
        //  instead of measuring loop size, measure the number of tiles in the loop?
        //  sort the links based on how popular the tiles in it are

        // start with all links as the "best"
        bestLoop = links;

        //loop over all the cuboids
        for (int i = 1; i < allNets.length && bestLoop.size() > 1; ++i) {
            Set<MetaTileLink> tested = new HashSet<>();

            for (MetaTileLink link : links) {
                if (tested.contains(link)) {
                    continue;
                }


                List<MetaTileLink> currentLoop = new ArrayList<>();
                TileLink start = link.links[i];
                for (TileLink pnt = start;
                     currentLoop.isEmpty() || pnt != start;
                     pnt = Utility.linkBackHome(this, pnt)) {
                    currentLoop.add(linkMap.get(pnt));
                }
                if (bestLoop.size() > currentLoop.size()) {
                    bestLoop = currentLoop;
                }

                tested.addAll(currentLoop);
            }
        }
    }


    private void ban(MetaTileLink metaLink, boolean banExtended) {
        bansMeta.add(metaLink);
        bansMeta.add(metaLink.mirror());

        boolean isCuboid = false;
        for (TileLink link : metaLink.links) {
            bansLink.add(link);
            bansLink.add(link.mirror);
            //todo, find a way to clearly ban the other ways into this exact metaTile
            // we're banning a tile, not a link??? on part of a search.  not done on every ban....

            if (isCuboid && Utility.linkBackHome(this, link) == null) {
                dead = true;//todo, maybe make this the return
            }
            isCuboid = true;
        }
    }

    private void ingestLink(MetaTileLink metaLink) {
        links.add(metaLink);
        for (TileLink link : metaLink.links) {
            linkMap.put(link, metaLink);
        }
    }

    public boolean isBanned(TileLink tl) {
        return isBanned(linkMap.get(tl));
    }

    public boolean isBanned(MetaTileLink mtl) {
        if (mtl == null) {
            return false;
        }

        // banned if explicitly banned
        if (bansMeta.contains(mtl)) {
            return true;
        }

        //Or if by adding the tile, a banned link gets inadvertently used.
        // this might be doable by banning tiles on the flat net instead.
        MetaTileLink mirror = mtl.mirror();
        for (int i = 0; i < 4; ++i) {
            MetaTileLink turn = mirror.turn(i);
            if (bansMeta.contains(turn)) {
                //todo, better plz
                ban(mtl, false);
                return true;
            }

            if (bansMeta.contains(turn.mirror())) {
                throw new RuntimeException("Link was banned, but mirror was not");
            }
        }
        return false;
    }

    void loopCountTest() {
        if (links.isEmpty()) {
            return;
        }
        Utility.saveImage(render(), "pic/" + maxTiles, "aaa-newest");

        int numLinks = links.size();

        int[] counts = new int[numLinks + 1];
        for (MetaTileLink link : links) {
            boolean isCuboid = false;
            for (TileLink l : link.links) {
                if (!isCuboid) {
                    isCuboid = true;
                    //continue;
                }


                int i = 0;
                for (TileLink pnt = l;
                     i == 0 || pnt != l;
                     ++i) {
                    pnt = Utility.linkBackHome(this, pnt);
                }
                ++counts[i];
            }

        }
        for (int i = 1; i < counts.length; ++i) {
            counts[i] /= i;
        }

        System.out.printf("depth:%s links:%s  %s%n", depth, numLinks, Arrays.toString(counts));
    }


    public void search(Results results) {
        if (results.reportProgress(this)) {
            return;
        }

        for (MetaTileLink link : bestLoop) {
            if (isBanned(link)) {
                continue;
            }

            new State(this, link).search(results);

            ban(link, true);//todo, bad the MetaTile??
            if (dead) {
                break;
            }
        }

        //todo, reset the ban list?
    }

    BufferedImage render() {
        int width = 0;
        int height = 0;
        int numParts = allNets.length;
        BufferedImage[] parts = new BufferedImage[numParts];
        for (int i = 0; i < numParts; ++i) {
            parts[i] = allNets[i].render(this);
            width += parts[i].getWidth();
            height = Math.max(height, parts[i].getHeight());
        }

        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = bi.createGraphics();
        int x = 0;
        for (int i = 0; i < numParts; ++i) {
            g2.drawImage(parts[i], x, 0, null);
            x += parts[i].getWidth();
        }
        return bi;
    }

    boolean isComplete() {
        return depth == maxTiles;
    }

    @Override
    public int compareTo(State o) {
        {
            // deeper things first
            int a = depth;
            int b = o.depth;
            if (a != b) {
                return -Integer.compare(a, b);
            }
        }
        {
            // fewer links
            int a = links.size();
            int b = o.links.size();
            if (a != b) {
                return Integer.compare(a, b);
            }
        }


        return 0;
    }
}
