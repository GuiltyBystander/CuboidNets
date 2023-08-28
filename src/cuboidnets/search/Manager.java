package cuboidnets.search;

import cuboidnets.Utility;

import java.util.ArrayList;
import java.util.List;

public class Manager extends Thread {

    final Workload work;
    final List<Worker> workers = new ArrayList<>();

    public Manager(Workload work) {
        this.work = work;
        for (int i = 0; i < Utility.cores; ++i) {
            workers.add(new Worker(work));
        }
    }

    @Override
    public void run() {
        try {
            for (Worker worker : workers) {
                worker.start();
            }
            System.out.println("Started " + workers.size());

            while (!work.isDone()) {
                work.printProgress();
                sleep(5000);
            }

            for (Worker worker : workers) {
                worker.join();
            }


        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}



