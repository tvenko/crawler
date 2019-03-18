package si.fri;


import si.fri.db.DatabaseManager;

import javax.inject.Inject;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {

    public static final int NUMBER_OF_PARALLEL_THREADS = 2;

    public static void main(String[] args) {

        ExecutorService executor = Executors.newFixedThreadPool(NUMBER_OF_PARALLEL_THREADS);

        Map<String, Zgodovina> zgodovina = new LinkedHashMap<String, Zgodovina>();

        Queue<Frontier> frontier = new LinkedList<Frontier>();

        boolean logger = true;
        boolean loggerHTMLUnit = false;

        // empty DB
        DatabaseManager dbManager = new DatabaseManager();
        try {
            dbManager.truncateDatabase();
        } catch (Exception e) {
            System.out.println("Failed to truncate Database" + e.getMessage());
        }

        frontier.add(new Frontier("http://evem.gov.si/", ""));

        zgodovina.put("http://evem.gov.si/", new Zgodovina("http://evem.gov.si/",""));

//        frontier.add(new Frontier("https://e-uprava.gov.si/", ""));

//        frontier.add(new Frontier("https://podatki.gov.si/", ""));

//        frontier.add(new Frontier("http://www.e-prostor.gov.si/", ""));

        executor.submit(new Crawler(frontier.remove().getUrl(), executor, zgodovina, frontier, new DatabaseManager(), logger, loggerHTMLUnit));

        /**
         *
         * TODO
         *
         * POZOR
         *
         * trenutno dela nas crawler samo 30 sekund !!!!!
         */
        try {
            executor.awaitTermination(30, TimeUnit.SECONDS);
            if (!executor.isTerminated()) {
                System.err.println("Timed out waiting for executor to terminate cleanly. Shutting down.");
                executor.shutdownNow();
            }
        } catch (final InterruptedException e) {
            System.err.println("Interrupted while waiting for executor shutdown.");
            Thread.currentThread().interrupt();
        }

        System.out.println("--------------------Zgodovina ----------------");
        for (String name: zgodovina.keySet()){

            String key = name;
            String value = zgodovina.get(name).urlParent;
            System.out.println(key + " " + value);
        }
        System.out.println("-------------------- Konec zgodovine ----------------");
        System.out.println("Velikost zgodovine: " + zgodovina.size());
    }
}

