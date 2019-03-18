package si.fri;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebClientOptions;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.apache.commons.logging.LogFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import si.fri.db.DatabaseManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	public final static String BASE_URL_GOV = "gov.si";

	private final String url;
	private final ExecutorService executor;
	private final Map<String, Zgodovina> zgodovina;
	private final Queue<Frontier> frontier;
	private final DatabaseManager dbManager;
	private final Map<String, ArrayList<String>> robotsDisallow;

	private final boolean logger;
	private final boolean loggerHTMLUnit;

	public Crawler(String url, ExecutorService executor,
				   Map<String, Zgodovina> zgodovina,
				   Queue<Frontier> frontier, DatabaseManager dbManager,
				   boolean logger, boolean loggerHTMLUnit,
				   Map<String, ArrayList<String>> robotsDisallow) {
		this.url = url;
		this.executor = executor;
		this.zgodovina = zgodovina;
		this.frontier = frontier;
		this.dbManager = dbManager;
		this.logger = logger;
		this.loggerHTMLUnit = loggerHTMLUnit;
		this.robotsDisallow = robotsDisallow;
	}

	public void run() {

		visit();
//		robots(url);

		synchronized(executor) { //https://stackoverflow.com/questions/1537116/illegalmonitorstateexception-on-wait-call
			if (frontier.isEmpty()) {
				try {
					executor.wait(5000); // TODO: THIS NEEDS FURTHER INSPECTION
					if (frontier.isEmpty()) {
						try {
							executor.awaitTermination(5, TimeUnit.SECONDS);
							if (!executor.isTerminated()) {
								System.err.println("Timed out waiting for executor to terminate cleanly. Shutting down.");
								executor.shutdownNow();
								System.out.println("--------------------Zgodovina ----------------");
								for (String name : zgodovina.keySet()) {

									String key = name;
									String value = zgodovina.get(name).urlParent;
									System.out.println(key + " " + value);
								}
								System.out.println("-------------------- Konec zgodovine ----------------");
								System.out.println("Velikost zgodovine: " + zgodovina.size());
							}
						} catch (final InterruptedException e) {
							System.err.println("Interrupted while waiting for executor shutdown." + e.getMessage());
							Thread.currentThread().interrupt();
						}
					}
				} catch (IllegalMonitorStateException | InterruptedException e) {
					System.err.println("Err while waiting." + e.getMessage());
				}


			}

		}

		if(logger)
			System.out.println("Executor: " + url + " " + executor.toString());
		if(logger)
			System.out.println("Velikost frontier-ja: " + frontier.size());
		while (!frontier.isEmpty()){
			synchronized(frontier) {
				Frontier f = frontier.remove();
				String u = f.getUrl();

				// Ali smo stran že obiskali?
				if (zgodovina.containsKey(u)) {
					zgodovina.get(u).n++;
				} else {
					zgodovina.put(u, new Zgodovina(u, f.getUrlParent()));
					executor.submit(new Crawler(u, executor, zgodovina, frontier, dbManager, logger, loggerHTMLUnit, robotsDisallow));
				}
			}
		}
	}

	// Iz url dobimo BASE URL
	public String getBaseUrl(String url) throws MalformedURLException {
		String baseUrl = "";
//		try {
			//URI uri = new URI(url); // should we use this?? https://stackoverflow.com/questions/9607903/get-domain-name-from-given-url
			URL base = new URL(url);

			//String path = base.getFile().substring(0, xx.getFile().lastIndexOf('/'));
			baseUrl = base.getProtocol() + "://" + base.getHost();
		/*}
		catch (Exception e)
		{
			System.err.println("For '" + baseUrl + "': " + e.getMessage() + " can't get base url");
		}*/
		return baseUrl;
	}

	//TODO
	public boolean shouldIVisit(String url) {

		// get base URL
		String baseUrl = "";
		try {
			baseUrl = getBaseUrl(url);
		} catch (MalformedURLException e) {
			System.err.println("For '" + url + "': " + e.getMessage() + " can't get base url");
		}

		// 1. ali je stran sploh iz domene .gov.si
		if (!baseUrl.contains(BASE_URL_GOV)) {
			return false;
		}

		// 2. ali robots dovoli dostop?
		if (!robotsDisallow.containsKey(baseUrl)) {
			robots(baseUrl);
		}
		ArrayList<String> pages = robotsDisallow.get(baseUrl);
		if (pages == null) {
			return true;
		}
		else if (pages.contains(url)) {
			return false;
		}

		return true;
	}

	//TODO - popravi parsanje linkov
	public void visit() {
		try {

			// Fetch the HTML code
			if(logger)
				System.out.println("Trenutni url: " + url);

			// first run with headless browser, so we can run potential JS
			WebClient webClient = new WebClient();
			WebClientOptions options = webClient.getOptions();
			options.setJavaScriptEnabled(true);
			options.setRedirectEnabled(true);

			// disable logging of htmlunit
			if (!loggerHTMLUnit) {
				LogFactory.getFactory().setAttribute("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");

				java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(Level.OFF);
				java.util.logging.Logger.getLogger("org.apache.commons.httpclient").setLevel(Level.OFF);
			}

			HtmlPage htmlPage = webClient.getPage(url);
			WebResponse response = htmlPage.getWebResponse();
			String content = response.getContentAsString();

			// PARSE WITH JSOUP
			Document document = Jsoup.parse(content);


			// WITHOUT JS
//			Document document = Jsoup.connect(url).get();

			// Parse the HTML to extract links to other URLs
			Elements linksOnPage = document.select("a[href]");

			// Javascript
//			Elements scriptTags = document.getElementsByTag("script");

			// img with src ending .png
//			Elements pngs = document.select("img[src$=.png]");

			// img with src ending .jpg
//			Elements jpgs = document.select("img[src$=.jpg]");


			String p;
			for (Element page : linksOnPage) {
				p = page.attr("abs:href");
				if (!p.contains("#") && !p.contains("?") && p.length() > 1) {
					if (p.contains(".pdf") || p.contains(".doc") || p.contains(".docx") || p.contains(".ppt") || p.contains(".pptx"))
						continue;//documents.add(page); // TODO SHRANI V BAZO
					else if ((p.contains("http://") || p.contains("https://")) &&
							(!p.contains(".zip") && !p.contains(".xlsx") &&
									!p.contains(".xls") && !p.contains(".pps") &&
									!p.contains(".jpg") && !p.contains(".png") &&
									!p.contains(".jspx") && !p.contains(".jsp") &&
									!p.contains(".mp4") && !p.contains(".exe"))) {

						if (shouldIVisit(p)) {
							frontier.add(new Frontier(p, url));
						}
					}
				}
			}

			// Parse the HTML to extract imgs with src ending (dot for mark)
			Elements srcImgs = document.select("img[src$='.']");

			for (Element page : srcImgs) {
				p = page.attr("abs:src");
				if (!p.contains("#") && !p.contains("?") && p.length() > 1) {
					if (p.contains("http://") || p.contains("https://")) { //TODO, this needs work
						// TODO: SAVE TO DB??
						if (shouldIVisit(p)) {
							frontier.add(new Frontier(p, url));
						}
					}
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

			saveToDB(document, response.getContentType(), response.getStatusCode());

		} catch (IOException e) {
			System.err.println("For '" + url + "': " + e.getMessage());
		}
	}

    // For each domain respect the robots.txt file if it exists.
    public void robots(String baseUrl) {

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
					frontier.add(new Frontier(subLink.split("map: ")[1], baseUrl));  //TODO CHECK
				}

				// TODO USER AGENT CHECK HERE
				if (subLink.toLowerCase().contains("user-agent")){
					System.out.println("TODO: user-agent check here!");
				}

				ArrayList<String> robotsDisallowLinks = new ArrayList<>();
				//TODO CHECK
				if (subLink.toLowerCase().contains("disallow")){
					if(logger)
						System.out.println("----disallow pages: "+ baseUrl + subLink.split("llow: ")[1]);
//					frontier.remove(baseUrl + subLink.split("llow: ")[1]);
					robotsDisallowLinks.add(baseUrl + subLink.split("llow: "));
				}
				robotsDisallow.put(baseUrl, robotsDisallowLinks);

			}
		} catch (IOException e) {
    		robotsDisallow.put(baseUrl, null);
			System.err.println("For '" + baseUrl + "': " + e.getMessage());
		}
    }

    private void saveToDB(Document document, String pageType, int httpStatusCode) {
	    if (pageType.equals("text/html"))
	        pageType = "HTML";
	    else
	        pageType = "BINARY";
		dbManager.addPageToDB(pageType, url, document.toString(), httpStatusCode, new Timestamp(System.currentTimeMillis()));
	}
}

