package si.fri;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebClientOptions;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.xml.XmlPage;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.LogFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.netpreserve.urlcanon.Canonicalizer;
import org.netpreserve.urlcanon.ParsedUrl;
import si.fri.db.DatabaseManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.regex.Pattern;

import java.util.concurrent.Future;

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
	private final String urlParent;
	private final ExecutorService executor;
	private final Map<String, Zgodovina> zgodovina;
	private final Queue<Frontier> frontier;
	private final DatabaseManager dbManager;
	private final Map<String, ArrayList<String>> robotsDisallow;
	private final Map<String, Integer> robotsDelay;
	private final List<String> originalSites;

	private final boolean logger;
	private final boolean loggerHTMLUnit;
	private String useragent;
	private static final int DEFAULT_CRAWL_DELAY = 4;

	public Crawler(String url, String urlParent, ExecutorService executor,
				   Map<String, Zgodovina> zgodovina,
				   Queue<Frontier> frontier, DatabaseManager dbManager,
				   boolean logger, boolean loggerHTMLUnit,
				   Map<String, ArrayList<String>> robotsDisallow,
				   Map<String, Integer> robotsDelay, List<String> originalSites) {
		this.url = url;
		this.urlParent = urlParent;
		this.executor = executor;
		this.zgodovina = zgodovina;
		this.frontier = frontier;
		this.dbManager = dbManager;
		this.logger = logger;
		this.loggerHTMLUnit = loggerHTMLUnit;
		this.robotsDisallow = robotsDisallow;
		this.robotsDelay = robotsDelay;
		this.originalSites = originalSites;
	}

	public void run() {
		visit();

		if(logger)
			System.out.println("Executor: " + url + " " + executor.toString());
		if(logger)
			System.out.println("Velikost frontier-ja: " + frontier.size());
	}

	public void init()
	{
		Future future = null;
		while (!frontier.isEmpty()){
			synchronized(frontier) {
				Frontier f = frontier.remove();
				String url = f.getUrl();
				String urlParent = f.getUrlParent();
				if (urlParent.equals("")) {
					String baseUrl = getBaseUrl(url);
					String[] robots = robots(baseUrl);
					saveSiteToDB(getDomain(url), robots[0], robots[1]);
				}

				// Ali smo stran že obiskali?
				if (zgodovina.containsKey(url)) {
					zgodovina.get(url).n++;
				} else {
					future = executor.submit(new Crawler(url, urlParent, executor, zgodovina, frontier, dbManager, logger, loggerHTMLUnit, robotsDisallow, robotsDelay, originalSites));
				}

			}
		}

		synchronized (frontier)
		{
			if(frontier.isEmpty())
			{
				try
				{
					Thread.sleep(10000);

					while(!future.isDone()){}

					if (frontier.isEmpty()) {
						halt();
					}
					else
						init();
				}
				catch (Exception e)
				{
					System.err.println("err while sleeping because " + e.getMessage());
				}
			}
		}
	}

	public void halt()
	{
		System.out.println("----" + frontier.size());
		try {
			executor.awaitTermination(10, TimeUnit.SECONDS);
			if (!executor.isTerminated()) {
				System.err.println("Timed out waiting for executor to terminate cleanly. Shutting down.");
				executor.shutdownNow();
			}
			//printHistory();
		} catch (final InterruptedException e) {
			System.err.println("Interrupted while waiting for executor shutdown." + e.getMessage());
			Thread.currentThread().interrupt();
		}
	}

	public void printHistory()
	{
		System.out.println("--------------------Zgodovina ----------------");
		for (String name : zgodovina.keySet()) {

			String key = name;
			String value = zgodovina.get(name).urlParent;
			System.out.println(key + " " + value);
		}
		System.out.println("-------------------- Konec zgodovine ----------------");
		System.out.println("Velikost zgodovine: " + zgodovina.size());
	}

	//TODO
	public boolean shouldIVisit(String url) {

		// get base URL
		String baseUrl = getBaseUrl(url);

		// 1. ali je stran sploh iz domene .gov.si
		if (!baseUrl.contains(BASE_URL_GOV)) {
			return false;
		}

		// 2. ali robots dovoli dostop?
		if (!robotsDisallow.containsKey(baseUrl)) {
			String[] robots = robots(baseUrl);
			saveSiteToDB(getDomain(url), robots[0], robots[1]);
		}
		ArrayList<String> pages = robotsDisallow.get(baseUrl);
		if (pages == null) {
			return true;
		}
		else {
			for (String disallow : pages) {
				if (url.contains(disallow)) {
					return false;
				}
			}
		}

		return true;
	}

	//TODO - popravi parsanje linkov
	public void visit() {
        zgodovina.put(url, new Zgodovina(url, urlParent));

        // TODO CHECK THIS
        try {
			int sleepTime = DEFAULT_CRAWL_DELAY;
			for (String baseSite : robotsDelay.keySet()) {
				if (url.contains(baseSite)) {
					sleepTime = robotsDelay.get(baseSite) * 1000;
					break;
				}
			}
			Thread.sleep(sleepTime);
		}
        catch (Exception e) {
        	System.out.println("Cannot sleep when visiting this url: " + url + ", reason: " + e.getMessage());
		}


		// Fetch the HTML code
		if(logger)
			System.out.println("Trenutni url: " + url);

		WebResponse response = getWebResponse(url);
		if (response != null) {

			// PARSE WITH JSOUP
			Document document = Jsoup.parse(response.getContentAsString());

			savePageToDB(document, response.getContentType(), response.getStatusCode());

			Elements linksOnPage = document.select("a[href]");

			String p;
			ParsedUrl parsedUrl;
			for (Element page : linksOnPage) {
				parsedUrl = ParsedUrl.parseUrl(page.attr("abs:href"));
				// https://github.com/iipc/urlcanon
				// Make sure that you work with canonicalized URLs only!
				Canonicalizer.SEMANTIC_PRECISE.canonicalize(parsedUrl);
				p = parsedUrl.toString();
				if (!p.contains("#") && !p.contains("?") && p.length() > 1) {
					if (p.contains(".pdf") || p.contains(".doc") || p.contains(".docx") || p.contains(".ppt") || p.contains(".pptx")) {
						if (originalSites.contains(getDomain(url))) {
							savePageDataToDB(url, p);
						}
					} else if ((p.contains("http://") || p.contains("https://")) &&
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
			Elements srcImgs = document.select("img[src$='']");

			for (Element page : srcImgs) {
				p = page.attr("src");
				if (!p.contains("#") && !p.contains("?") && p.length() > 1) {
					if (!p.contains("http://") && !p.contains("https://"))
						p = getBaseUrl(url) + "/" + p;
					if (originalSites.contains(getDomain(url)))
						saveImageToDB(url, p);
				}
			}
		}
	}

    // For each domain respect the robots.txt file if it exists.
    public String[] robots(String baseUrl) {

		String robots = baseUrl + "/robots.txt";
        String siteMapContent = "";
        StringBuilder robotsContent = new StringBuilder();
        boolean respectRobots = true;
        boolean respectRobotsStar = true;
        int crawlDelay = DEFAULT_CRAWL_DELAY;

    	try {
			BufferedReader in = new BufferedReader(new InputStreamReader(new URL(robots).openStream()));
			String subLink;
			String sitemap;
            ArrayList<String> robotsDisallowLinks = new ArrayList<>();

			while((subLink = in.readLine()) != null) {

				// TODO USER AGENT is this ok?
				if (subLink.toLowerCase().contains("user-agent")){
					String line = subLink.split("llow: ")[0];
					if(logger)
						System.out.println("Useragent: " + line);

					//get our user agent info
					if (useragent == null) {
						setUseragent();
					}

					if (line.toLowerCase().contains("*")) {
						respectRobotsStar = true;
						System.out.println("User-agent: Robots have some specifics for * user agent !!!!!!!!!");
					}
					else if (line.toLowerCase().contains(useragent)) {
						respectRobots = true;
						robotsDisallowLinks = new ArrayList<>();
						System.out.println("User-agent: Robots have some specifics for our user agent !!!!!!!!!");
					}
					else {
						respectRobots = false;
						respectRobotsStar = false;
					}
				}

				if (subLink.toLowerCase().contains("crawl-delay")){
					String[] line = subLink.toLowerCase().split(":");
					String[] number = line[1].split(" ");
					crawlDelay = Integer.parseInt(number[1]);
					if(logger)
						System.out.println("CRAWL delay: " + line[1]);
				}


				// If a sitemap is defined, all the URLs that are defined within it, should be added to the frontier.
				// TODO: parse sitemap content and add it to frontier
				if (subLink.toLowerCase().contains("sitemap")){
					sitemap = subLink.split("map: ")[1];
                    WebResponse response = getWebResponse(sitemap);
                    if (response != null)
                    	siteMapContent = response.getContentAsString();
				}

				//TODO CHECK
				if (subLink.toLowerCase().contains("disallow") && (respectRobots || respectRobotsStar)){
					String line = subLink.split("llow: ")[1];
					if(logger)
						System.out.println("----disallow pages: "+ baseUrl + line);

					// pravilo za dissalow vsebuje zvezdico, gledali bomo če je to containano v linku
					if (line.toLowerCase().contains("*")) {
						String[] split = line.split("\\*");
						if (split.length > 1) {
							robotsDisallowLinks.add(split[1]);
						}
						else {
							robotsDisallowLinks.add(split[0]);
						}
					}
					else {
						robotsDisallowLinks.add(baseUrl + line);
					}
				}
				robotsContent.append(subLink).append("\n");
			}

//			if (respectRobots || respectRobotsStar) {
			robotsDisallow.put(baseUrl, robotsDisallowLinks);
			robotsDelay.put(baseUrl, crawlDelay);
//			}

		} catch (IOException e) {
    		robotsDisallow.put(baseUrl, null);
			robotsDelay.put(baseUrl, DEFAULT_CRAWL_DELAY);
			System.err.println("For '" + baseUrl + "': " + e.getMessage());
		}
    	return new String[]{robotsContent.toString(), siteMapContent};
    }

    private WebResponse getWebResponse(String url) {
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

		try {
			Page page = webClient.getPage(url);
			return page.getWebResponse();
		} catch (IOException e) {
			System.err.println("For '" + url + "': " + e.getMessage());
			return null;
		}
	}

	private void setUseragent() {
		WebClient webClient = new WebClient();
		WebClientOptions options = webClient.getOptions();
		options.setJavaScriptEnabled(true);
		options.setRedirectEnabled(true);

		useragent = webClient.getBrowserVersion().getUserAgent();
	}

    private void savePageToDB(Document document, String pageType, int httpStatusCode) {
	    if (pageType.equals("text/html"))
	        pageType = "HTML";
	    else
	        pageType = "BINARY";
	    String baseUrl = getDomain(url);
		dbManager.addPageToDB(pageType, baseUrl, url, document.toString(), httpStatusCode, new Timestamp(System.currentTimeMillis()));
        dbManager.addLinkToDB(urlParent, url);
	}

	private void savePageDataToDB(String urlParent, String url) {
		byte[] data = getBinaryDocument(url);
		if (data != null) {
			String code = FilenameUtils.getExtension(url).toUpperCase();
			dbManager.addPageDataToDB(urlParent, data, code);
		}
	}

	private void saveImageToDB(String urlParent, String url) {
		byte[] data = getBinaryDocument(url);
		if (data != null) {
			String fileName = FilenameUtils.getName(url);
			String contentType = FilenameUtils.getExtension(url);
			dbManager.addImageToDB(urlParent, fileName, contentType, data, new Timestamp(System.currentTimeMillis()));
		}
	}

	private void saveSiteToDB(String domain, String robots, String sitemap) {
        dbManager.addSiteToDB(domain, robots, sitemap);
    }

    private byte[] getBinaryDocument(String url) {
		try {
			URL document = new URL(url);
			InputStream stream = document.openStream();
			byte[] data = new byte[stream.available()];
			stream.read(data);
			stream.close();
			return data;
		} catch (IOException e) {

		}
		return null;
	}

	// Iz url dobimo BASE URL
	private String getBaseUrl(String url) {
		String baseUrl = "";

		try {
			URL base = new URL(url);
			baseUrl = base.getProtocol() + "://" + base.getHost();
		} catch (MalformedURLException e) {
			System.err.println("For '" + url + "': " + e.getMessage() + " can't get base url");
		}

		return baseUrl;
	}

	private String getDomain(String url) {
		String domain = "";

		try {
			URL base = new URL(url);
			domain = base.getHost();
		} catch (MalformedURLException e) {
			System.err.println("For '" + url + "': " + e.getMessage() + " can't get base url");
		}

		return domain;
	}
}

