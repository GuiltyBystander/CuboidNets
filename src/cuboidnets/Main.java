package cuboidnets;

import cuboidnets.search.Manager;
import cuboidnets.search.Workload;
import cuboidnets.structure.Cuboid;

import java.util.Arrays;

// Press Shift twice to open the Search Everywhere dialog and type `show whitespaces`,
// then press Enter. You can now see whitespace characters in your code.
public class Main {
    public static void main(String[] args) {
        long start = System.nanoTime();

        Cuboid[][] cuboids = {
                { // 6 - 0
                        //x1    11   2.2  sec
                        //x2   723   6.95 sec
                        //x3 15057  75.65 sec
                        new Cuboid(1, 1, 1),
                },
                {// 22 - 1
                        new Cuboid(1, 1, 5),
                        new Cuboid(1, 2, 3),
                },
                {// 30 - 2
                        new Cuboid(1, 1, 7),
                        new Cuboid(1, 3, 3),
                },
                {// 46 - 3 - 1058 roots
                        new Cuboid(1, 1, 11),
                        new Cuboid(1, 2, 7),
                        new Cuboid(1, 3, 5),
                },
                {// 54 - 4 - 243 roots
                        new Cuboid(1, 1, 13),
                        new Cuboid(1, 3, 6),
                        new Cuboid(3, 3, 3),
                },
                {// 70 - 5 - 85750 roots
                        //1 × 1 × 17, 1 × 2 × 11, 1 × 3 × 8, 1 × 5 × 5
                        new Cuboid(1, 1, 17),
                        new Cuboid(1, 2, 11),
                        new Cuboid(1, 3, 8),
                        new Cuboid(1, 5, 5),
                },
                {
                        //'2x4x6', '1x4x8', '2x2x10', '1x2x14'
                        new Cuboid(1, 2, 14),
                        new Cuboid(2, 2, 10),
                        new Cuboid(1, 4, 8),
                        new Cuboid(2, 4, 6),
                },
                {
                        // 94 - 7 - too big
                        //'3x4x5', '1x5x7', '1x3x11', '1x2x15', '1x1x23'
                        new Cuboid(1, 1, 23),
                        new Cuboid(1, 2, 15),
                        new Cuboid(1, 3, 11),
                        new Cuboid(1, 5, 7),
                        new Cuboid(3, 4, 5),
                },
                {
                        // 238 - 8
                        // '5x7x7', '4x5x11', '1x9x11', '3x5x13', '1x7x14',
                        // '1x5x19', '1x4x23', '1x3x29', '1x2x39', '1x1x59'
                        new Cuboid(5, 7, 7),
                        new Cuboid(4, 5, 11),
                        new Cuboid(1, 9, 11),
                        new Cuboid(3, 5, 13),
                        new Cuboid(1, 7, 14),
                        new Cuboid(1, 5, 19),
                        new Cuboid(1, 4, 23),
                        new Cuboid(1, 3, 29),
                        new Cuboid(1, 2, 39),
                        new Cuboid(1, 1, 59),
                },
        };
        for (Cuboid[] cc : cuboids) {
            for (Cuboid c : cc) {
                System.out.println(c + " " + c.tiles.size() + " " + c.roots.size());
            }
            System.out.println();
            Workload w = new Workload(cc);
            if (!w.randomExplore) {
                System.out.println("roots " + w.queueWork.size());
            }
            System.out.println("**");
        }

        System.out.println("-----------------");

        Workload work = new Workload(cuboids[1]);

        Manager manager = new Manager(work);
        manager.start();
        try {
            manager.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        System.out.println(Arrays.toString(work.allResults.depthCount));
        System.out.println(work);
        System.out.println(work.allResults.flatFinished.size());
        System.out.println((System.nanoTime() - start) / 1000_000_000.);
    }
}

