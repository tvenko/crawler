package si.fri;


import si.fri.db.DatabaseManager;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

    private static final int NUMBER_OF_PARALLEL_THREADS = 2;

    public static void main(String[] args) {

        ExecutorService executor = Executors.newFixedThreadPool(NUMBER_OF_PARALLEL_THREADS);

        Map<String, Zgodovina> zgodovina = new LinkedHashMap<>();

        Queue<Frontier> frontier = new LinkedList<>();

        Map<String, String> robotsInfo = new LinkedHashMap<>();
        Map<String, Integer> robotsDelay = new LinkedHashMap<>();

        Map<String, String> hashCode = new HashMap<>();

        List<String> originalSites;
        originalSites = Arrays.asList("evem.gov.si", "www.evem.gov.si", "e-uprava.gov.si", "www.e-uprava.gov.si",
                "podatki.gov.si", "www.podatki.gov.si", "www.e-prostor.gov.si", "e-prostor.gov.si/");


        boolean logger = true;
        boolean loggerHTMLUnit = false;

        // empty DB
        DatabaseManager dbManager = new DatabaseManager();
        try {
            dbManager.truncateDatabase();
        } catch (Exception e) {
            System.out.println("Failed to truncate Database" + e.getMessage());
        }

//        frontier.add(new Frontier("http://evem.gov.si/", ""));

//        frontier.add(new Frontier("https://e-uprava.gov.si/", ""));
//
//        frontier.add(new Frontier("https://podatki.gov.si/", ""));
//
        frontier.add(new Frontier("http://www.e-prostor.gov.si/", ""));

        // CUSTOM SELECTION

//        frontier.add(new Frontier("http://www.gu.gov.si/", ""));

        Crawler crawler = new Crawler("", "",
                                        executor, zgodovina, frontier,
                                        new DatabaseManager(), logger,
                                        loggerHTMLUnit, robotsInfo,
                                        robotsDelay, originalSites, hashCode);
        crawler.init();
    }
}

