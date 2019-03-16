package si.fri;

import java.sql.Timestamp;
import java.util.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import si.fri.db.DatabaseManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Crawler implements Runnable
{
	/**
	 * TODO	
	 * 
	 * Parallel
	 * 
	 * frontier - breadth-first strategy - first in first out
	 * 
	 * for each domain respect the robots.txt file if it exists.
	 * 
	 * Correctly respect the commands
	 * 	- User-agent, 
	 *  - Allow, 
	 *  - Disallow, 
	 *  - Crawl-delay and 
	 *  - Sitemap. 
	 *  
	 * If a sitemap is defined, all the URLs that are defined within it, should be added to the frontier. 
	 * 
	 * Make sure to respect robots.txt as sites that define special crawling rules often contain spider traps.
	 * 
	 * During crawling you need to detect duplicate web pages.
	 * For the deduplicated web pages, 
	 * 	- check URLs that you already parsed and 
	 *  - URLs that you have in the frontier if a duplicate exist. 
	 *  - check if a web page with the same content was parsed already
	 *  
	 *  Links:
	 *   When parsing links, include links from href attributes and 
	 *   onclick Javascript events (e.g. location.href or document.location). Be careful to correctly extend the relative URLs before adding them to the frontier.
	 *   
	 *  Images:
	 *   - Detect images on a web page only based on img tag, where the src attribute points to an image URL.
	 *   
	 *   Apart from web pages only, download also other files that web pages point to (there is no need to parse them). 
	 *   File formats that you should take into account are .pdf, .doc, .docx, .ppt and .pptx.
	 *   
	 *   
	 *   Database
	 *
	 *   robots sam za
	 */

	final static Pattern urlPattern = Pattern.compile("(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");

	private final String url;
	private final ExecutorService executor;
	private final Map<String, Zgodovina> zgodovina;
	private final Queue<Frontier> frontier;
	private final DatabaseManager dbManager;

	private final boolean logger;

	public Crawler(String url, ExecutorService executor, Map<String, Zgodovina> zgodovina, Queue<Frontier> frontier, DatabaseManager dbManager, boolean logger) {
		this.url = url;
		this.executor = executor;
		this.zgodovina = zgodovina;
		this.frontier = frontier;
		this.dbManager = dbManager;
		this.logger = logger;
	}

	public void run() {
		visit();
		robots();
		if(logger)
			System.out.println(url + " " + executor.toString());
		if(logger)
			System.out.println("Velikost frontier-ja: " + frontier.size());
		while (!frontier.isEmpty()){
			synchronized(frontier) {
				Frontier f = frontier.remove();
				String u = f.getUrl();
				if (zgodovina.containsKey(u)) {
					zgodovina.get(u).n++;
				} else {
					zgodovina.put(u, new Zgodovina(u, f.getUrlParent()));
					executor.submit(new Crawler(u, executor, zgodovina, frontier, dbManager, logger));
				}
			}
		}
	}

	//TODO - popravi parsanje linkov
	public void visit()
	{
		try {

			// Fetch the HTML code
			if(logger)
				System.out.println("Trenutni url: " + url);
			Document document = Jsoup.connect(url).get();

			// Parse the HTML to extract links to other URLs
			Elements linksOnPage = document.select("a[href]");

			// img with src ending .png
			Elements pngs = document.select("img[src$=.png]");

			// img with src ending .jpg
			Elements jpgs = document.select("img[src$=.jpg]");

			String p;
			for (Element page : linksOnPage) {
				p = page.attr("abs:href");
				if(!p.contains("#") &&
						!p.contains("?") &&
						p.length() > 1) {
					if(p.contains(".pdf") || p.contains(".doc") || p.contains(".docx") || p.contains(".ppt") || p.contains(".pptx"))
						continue;//documents.add(page);
					else if((p.contains("http://") || p.contains("https://")) &&
							(!p.contains(".zip") && !p.contains(".xlsx") &&
									!p.contains(".xls") && !p.contains(".pps") &&
									!p.contains(".jpg") && !p.contains(".png") &&
									!p.contains(".jspx") && !p.contains(".jsp") &&
									!p.contains(".mp4") && !p.contains(".exe")))
						frontier.add(new Frontier(p, url));
				}
			}


			// For each extracted png image.
			/*for (Element s : pngs) {
				//System.out.println("SRC do slike (png): " + s.attr("abs:src"));
				images.add(s.attr("abs:src"));
			}

			// For each extracted jpg image
			for (Element s : jpgs) {
				//System.out.println("SRC do slike (jpg): " + s.attr("abs:src"));
				images.add(s.attr("abs:src"));
			}*/

			saveToDB(document);

		} catch (IOException e) {
			System.err.println("For '" + url + "': " + e.getMessage());
		}
	}

    // For each domain respect the robots.txt file if it exists.
    public void robots()
    {
    	String baseUrl = "";
    	// Iz url dobimo BASE URL
		try {
			URL base = new URL(url);
			//String path = base.getFile().substring(0, xx.getFile().lastIndexOf('/'));
			baseUrl = base.getProtocol() + "://" + base.getHost();
		}
		catch (Exception e)
		{
			System.err.println("For '" + baseUrl + "': " + e.getMessage() + " can't get base url");
		}

		String robots = baseUrl + "/robots.txt";
    	try {
			BufferedReader in = new BufferedReader(new InputStreamReader(new URL(robots).openStream()));
			String subLink = null;

			String sitemap;
			while((subLink = in.readLine()) != null) {

				// If a sitemap is defined, all the URLs that are defined within it, should be added to the frontier.
				if (subLink.toLowerCase().contains("sitemap")){
					sitemap = subLink.split("map: ")[1];
					//if(frontier.contains())
					frontier.add(new Frontier(subLink.split("map: ")[1], baseUrl));
				}

				//TODO ??
				// If a Disallow is defined, all the URLs that are disallow should be removed from the frontier
				if (subLink.toLowerCase().contains("disallow")){
					if(logger)
						System.out.println("----disallow pages: "+ baseUrl + subLink.split("llow: ")[1]);
					frontier.remove(baseUrl + subLink.split("llow: ")[1]);
				}

			}
		} catch (IOException e) {
			System.err.println("For '" + baseUrl + "': " + e.getMessage());
		}
    }

    private void saveToDB(Document document) {
		dbManager.addPageToDB("HTML", url, document.toString(), 200, new Timestamp(System.currentTimeMillis()));
	}
}

