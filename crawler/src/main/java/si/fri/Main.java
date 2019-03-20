package si.fri;


import si.fri.db.DatabaseManager;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

    public static final int NUMBER_OF_PARALLEL_THREADS = 2;

    public static void main(String[] args) {

        ExecutorService executor = Executors.newFixedThreadPool(NUMBER_OF_PARALLEL_THREADS);

        Map<String, Zgodovina> zgodovina = new LinkedHashMap<String, Zgodovina>();

        Queue<Frontier> frontier = new LinkedList<Frontier>();

        Map<String, ArrayList<String>> robotsDisallow = new LinkedHashMap<>();

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

//        frontier.add(new Frontier("https://e-uprava.gov.si/", ""));
//
//        frontier.add(new Frontier("https://podatki.gov.si/", ""));
//
//        frontier.add(new Frontier("http://www.e-prostor.gov.si/", ""));

        Crawler crawler = new Crawler("", "",
                                        executor, zgodovina, frontier,
                                        new DatabaseManager(), logger,
                                        loggerHTMLUnit, robotsDisallow);
        crawler.init();
    }
}

