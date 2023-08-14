package cuboidnets;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Objects;
import java.util.PriorityQueue;

public class DenseFlatNet implements Comparable<DenseFlatNet> {

    final int w, h;
    final BitSet bits;


    private DenseFlatNet(SearchState state, Searchable face) {
        var tiles = new ArrayList<>(face.getTiles());
        var offset = Utility.calcOffset(state, tiles, false);

        int flatW = 0, flatH = 0;
        for (Tile t : tiles) {
            var xy = Utility.remap(t, offset);
            flatW = Math.max(xy[0] + 1, flatW);
            flatH = Math.max(xy[1] + 1, flatH);
        }

        w = flatW;
        h = flatH;
        bits = new BitSet(w * h);
        for (Tile t : tiles) {
            var xy = Utility.remap(t, offset);
            int k = xy[0] + xy[1] * w;
            bits.set(k);
        }
    }

    private DenseFlatNet(int w, int h, BitSet bits) {
        this.w = w;
        this.h = h;
        this.bits = bits;
    }

    static DenseFlatNet fromState(SearchState state) {
        PriorityQueue<DenseFlatNet> options = new PriorityQueue<>();

        Searchable face = state.allNets[0];

        DenseFlatNet f1 = new DenseFlatNet(state, face);
        options.add(f1);
        options.add(f1.flipX());
        options.add(f1.flipY());
        options.add(f1.flipX().flipY());

        f1 = f1.flipXY();
        options.add(f1);
        options.add(f1.flipX());
        options.add(f1.flipY());
        options.add(f1.flipX().flipY());


        return options.poll();
    }

    private DenseFlatNet flipX() {
        BitSet newBits = new BitSet(w * h);
        for (int i = 0; i < w; ++i) {
            for (int j = 0; j < h; ++j) {
                int k1 = i + j * w;
                int x = w - 1 - i;
                int y = j;
                int k2 = x + y * w;
                newBits.set(k2, bits.get(k1));
            }
        }

        return new DenseFlatNet(w, h, newBits);
    }

    private DenseFlatNet flipY() {
        BitSet newBits = new BitSet(w * h);
        for (int i = 0; i < w; ++i) {
            for (int j = 0; j < h; ++j) {
                int k1 = i + j * w;
                int x = i;
                int y = h - 1 - j;
                int k2 = x + y * w;
                newBits.set(k2, bits.get(k1));
            }
        }

        return new DenseFlatNet(w, h, newBits);
    }

    private DenseFlatNet flipXY() {
        BitSet newBits = new BitSet(w * h);
        for (int i = 0; i < w; ++i) {
            for (int j = 0; j < h; ++j) {
                int k1 = i + j * w;
                int k2 = i * h + j;
                newBits.set(k2, bits.get(k1));
            }
        }

        return new DenseFlatNet(h, w, newBits);
    }

    public int compareTo(DenseFlatNet o) {
        if (w != o.w) {
            return Integer.compare(w, o.w);
        }
        if (h != o.h) {
            return Integer.compare(h, o.h);
        }

        for (int i = 0; i < w * h; ++i) {
            boolean a = bits.get(i);
            boolean b = o.bits.get(i);
            if (a != b) {
                return Boolean.compare(a, b);
            }
        }
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DenseFlatNet that = (DenseFlatNet) o;
        return w == that.w &&
                h == that.h &&
                Objects.equals(bits, that.bits);
    }

    @Override
    public int hashCode() {
        return Objects.hash(w, h, bits);
    }

    public BufferedImage render() {
        int scale = 20;
        int margin = 1;
        int wm = w + margin * 2;
        int hm = h + margin * 2;
        BufferedImage bi = new BufferedImage(wm * scale, hm * scale, BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D g2 = bi.createGraphics();
        g2.setColor(Color.white);
        g2.fillRect(0, 0, wm * scale, hm * scale);

        g2.setColor(Color.black);
        for (int i = 0; i < w; ++i) {
            for (int j = 0; j < h; ++j) {
                int k = i + j * w;
                if (bits.get(k)) {
                    g2.drawRect((i + margin) * scale, (j + margin) * scale, scale, scale);
                    //bi.setRGB(i + margin, j + margin, 0);
                }
            }
        }
        return bi;
    }
}
