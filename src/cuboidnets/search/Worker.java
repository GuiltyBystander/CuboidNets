package cuboidnets.search;

public class Worker extends Thread {
    final Workload work;

    Worker(Workload work) {
        this.work = work;
    }

    @Override
    public void run() {
        try {
            while (!work.isDone()) {
                Results results = new Results(work);
                cuboidnets.search.State w = work.getWork(results);
                if (w == null) {
                    //all work is currently handed out, but may be returned with subtasks
                    sleep(1000);
                } else {
                    w.search(results);
                    work.returnWork(w, results);
                }
            }
            System.out.println("i'm quitting");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
