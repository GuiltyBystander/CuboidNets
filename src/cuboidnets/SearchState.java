package cuboidnets;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.List;
import java.util.*;

public class SearchState implements Serializable {

    //todo move statics into a data accumulator class that's used per thread
    static final long startTime = System.nanoTime();
    static final int[] depthCount = new int[71];
    static int cnt;
    static int bestDepth = 0;
    final int index;
    final Searchable[] allNets; // first is a flat net, rest are cuboids
    final int maxTiles; // total tiles on the cuboid
    final MetaTileLink root;
    final Map<Tile, Integer> tiles = new HashMap<>(); // all the tiles we've used and what depth
    final List<MetaTileLink> links = new ArrayList<>(); //all the potential links //todo: consider making `links` a set
    final Map<TileLink, MetaTileLink> linkMap = new HashMap<>();
    final Set<MetaTileLink> bansMeta = new HashSet<>();
    final Set<TileLink> bansLink = new HashSet<>();
    int depth; //basically final, but technically not
    boolean dead = false; // if true, the cuboid is unsolvable
    List<MetaTileLink> bestLoop = null;

    SearchState(Searchable[] allNets, MetaTileLink root, int maxTiles) {
        index = cnt++;
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

    private SearchState(SearchState parent, MetaTileLink step) {
        index = cnt++;
        this.allNets = parent.allNets;
        this.depth = parent.depth;
        this.root = parent.root;
        this.maxTiles = parent.maxTiles;
        this.tiles.putAll(parent.tiles);
        this.links.addAll(parent.links);
        this.linkMap.putAll(parent.linkMap);
        this.bansMeta.addAll(parent.bansMeta);
        this.bansLink.addAll(parent.bansLink);
        this.dead = parent.dead;

        makeStep(step);
    }

    //SearchState(String encoding) {        //todo, find a way to decode    }
    //String encode() {        //todo find a way to encode        return "";    }

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


        links.removeIf(this::isBanned);

        findSmallestLoop();
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

        // if only one entrance to loop, take it
        if (bestLoop.size() == 1) {
            makeStep(bestLoop.get(0));
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
                dead = true;
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

    boolean isBanned(MetaTileLink mtl) {
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
        if (links.size() == 1 && false) {
            System.exit(1);
        }
    }

    void reportProgress() {
        ++depthCount[depth];
        if (index % 100000 == 0) {
            System.out.printf("%s %s%n", Arrays.toString(depthCount), (System.nanoTime() - startTime) / 1000_000_000.);
            // loopCountTest();
            // Utility.saveImage(render(), "pic/" + maxTiles, "aaa-newest");
        }
    }

    Collection<SearchState> search() {
        Collection<SearchState> out = new ArrayList<>();
        if (dead) {
            return out;
        }
        reportProgress();

        //todo, add parameter for max depth
        //todo, prune search if tiles are cut off
        if (bestDepth < depth) {
            bestDepth = depth;
            Utility.saveImage(render(), "pic/" + maxTiles, depth + "-" + index);
        }

        if (isComplete()) {
            //Utility.saveImage(render(), "pic/" + maxTiles, "complete" + index);
            //Utility.saveImage(DenseFlatNet.fromState(this).render(), "pic/" + maxTiles + "/bits", "f" + index);
            out.add(this);
            return out;
        }

        //for (MetaTileLink link : links) {
        for (MetaTileLink link : bestLoop) {
            if (isBanned(link)) {
                continue;
            }

            //todo: fancy stuff for parallel processing
            out.addAll(new SearchState(this, link).search());

            ban(link, true);//todo, bad the MetaTile??

            if (dead) {
                break;
            }
        }

        return out;
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
}
