import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main
{
    public static void main(String[] args) {

        ExecutorService executor = Executors.newFixedThreadPool(5);

        Map<String, Zgodovina> zgodovina = new LinkedHashMap<String, Zgodovina>();

        zgodovina.put("http://www.e-prostor.gov.si/", new Zgodovina("http://www.e-prostor.gov.si/", ""));

        //frontier.add("http://evem.gov.si/evem/drzavljani/zacetna.evem");
        //frontier.add("https://e-uprava.gov.si/");
        //frontier.add("https://podatki.gov.si/");
        //frontier.add("http://www.e-prostor.gov.si/");


        executor.submit(new Crawler("http://www.e-prostor.gov.si/", executor, zgodovina));

        try {
            executor.awaitTermination(5*60, TimeUnit.SECONDS);
            if (!executor.isTerminated()) {
                System.err.println("Timed out waiting for executor to terminate cleanly. Shutting down.");
                executor.shutdownNow();
            }
        } catch (final InterruptedException e) {
            System.err.println("Interrupted while waiting for executor shutdown.");
            Thread.currentThread().interrupt();
        }

    //	Crawler crawler = new Crawler();
    //	crawler.crawl();
    }
}

