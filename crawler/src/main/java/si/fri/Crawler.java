package si.fri;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebClientOptions;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.panforge.robotstxt.Grant;
import com.panforge.robotstxt.RobotsTxt;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.LogFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.netpreserve.urlcanon.Canonicalizer;
import org.netpreserve.urlcanon.ParsedUrl;
import si.fri.db.DatabaseManager;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class Crawler implements Runnable
{

	private final static String BASE_URL_GOV = "gov.si";
	private final static int TIMEOUT = 15000; // in milliseconds

	private final String url;
	private final String urlParent;
	private final ExecutorService executor;
	private final Map<String, Zgodovina> zgodovina;
	private final Queue<Frontier> frontier;
	private final DatabaseManager dbManager;
	private final Map<String, String> robotsInfo;
	private final Map<String, Integer> robotsDelay;
	private final List<String> originalSites;
	private final Map<String, String> hashCode;
    private final String useragent;

    private final boolean logger;
	private final boolean loggerHTMLUnit;
	private static final int DEFAULT_CRAWL_DELAY = 4;
	private static final int LIMIT_HALT_SIZE = 100000;

	public Crawler(String url, String urlParent, ExecutorService executor,
				   Map<String, Zgodovina> zgodovina,
				   Queue<Frontier> frontier, DatabaseManager dbManager,
				   boolean logger, boolean loggerHTMLUnit,
				   Map<String, String> robotsInfo,
				   Map<String, Integer> robotsDelay, List<String> originalSites,
				   Map<String, String> hashCode, String useragent) {
		this.url = url;
		this.urlParent = urlParent;
		this.executor = executor;
		this.zgodovina = zgodovina;
		this.frontier = frontier;
		this.dbManager = dbManager;
		this.logger = logger;
		this.loggerHTMLUnit = loggerHTMLUnit;
		this.robotsInfo = robotsInfo;
		this.robotsDelay = robotsDelay;
		this.originalSites = originalSites;
		this.hashCode = hashCode;
		this.useragent = useragent;
	}

	public void run() {
		visit();

		if(logger)
			System.out.println("Executor: " + url + " " + executor.toString());
		if(logger)
			System.out.println("Velikost frontier-ja: " + frontier.size());
	}

	public void init() {
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

				// Ali smo dosegli mejo strani
				if (zgodovina.size() >= LIMIT_HALT_SIZE) {
					halt();
				}

				// Ali smo stran že obiskali?
				if (zgodovina.containsKey(url)) {
					zgodovina.get(url).n++;
				} else {
					future = executor.submit(new Crawler(url, urlParent, executor, zgodovina, frontier, dbManager, logger,
							loggerHTMLUnit, robotsInfo, robotsDelay, originalSites, hashCode, useragent));
				}

			}
		}

		synchronized (frontier)
		{
			if(frontier.isEmpty()) // TODO - dodaj stop ko pride do 100k strani
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

	private void halt()
	{
		System.out.println("----" + frontier.size());
		try {
			executor.awaitTermination(10, TimeUnit.SECONDS);
			if (!executor.isTerminated()) {
				System.err.println("Timed out waiting for executor to terminate cleanly. Shutting down.");
				executor.shutdownNow();
			}
			printHistory();
            // TIMESTAMP end
            LocalDateTime datetime = LocalDateTime.now();
            System.out.println("End time: " + datetime);
		} catch (final InterruptedException e) {
			System.err.println("Interrupted while waiting for executor shutdown." + e.getMessage());
			Thread.currentThread().interrupt();
		}
	}

	private void printHistory()
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

	private boolean shouldIVisit(String url) {

		// get base URL
		String baseUrl = getBaseUrl(url);

		// 1. ali je stran sploh iz domene .gov.si
		if (!baseUrl.contains(BASE_URL_GOV)) {
			return false;
		}

		// 2. ali robots dovoli dostop?
		if (!robotsInfo.containsKey(baseUrl)) {
			String[] robots = robots(baseUrl);
			saveSiteToDB(getDomain(url), robots[0], robots[1]);
		}

		String robotsTxtString = robotsInfo.get(baseUrl);
		if (robotsTxtString != null) {

			try {
				ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(robotsTxtString.getBytes("UTF-8"));
				RobotsTxt robotsTxt = RobotsTxt.read(byteArrayInputStream);

				Grant grant = robotsTxt.ask(useragent, url);
				if (grant == null || grant.hasAccess()) {
					if (logger)
						System.out.println("robots allow access for url " + url);
					return true;
				}

			} catch (Exception e) {
				System.out.println("Exception when parsing robots for url " + url + " " + e.getMessage());

			}
		}

		return false;
	}

	public void visit() {

        zgodovina.put(url, new Zgodovina(url, urlParent));

        // respect delay
        try {
			int sleepTime = robotsDelay.get(getBaseUrl(url)) * 1000;
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


			// Parse the HTML to extract a with href attributes
			Elements linksOnPage = document.select("a[href]");

			String p;
			ParsedUrl parsedUrl;
			for (Element page : linksOnPage) {
				parsedUrl = ParsedUrl.parseUrl(page.attr("abs:href"));
				// https://github.com/iipc/urlcanon
				// Make sure that you work with canonicalized URLs only!
				Canonicalizer.SEMANTIC_PRECISE.canonicalize(parsedUrl);
				p = parsedUrl.toString();

				if (p.contains("#")) {
					p = splitHash(p);
				}
				if (!p.contains("(at)") && p.length() > 1) {
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

			// TODO - we check only a and button, we should check every element
			// Parse the HTML to extract a onclick events
			Elements linksOnClickOnPage = document.select("a[onclick]");
			extractLinks(linksOnClickOnPage, "onclick");

			// Parse the HTML to extract button onclick events
			Elements buttonsOnClickOnPage = document.select("button[onclick]");
			extractLinks(buttonsOnClickOnPage, "onclick");


			// Parse the HTML to extract imgs with src ending (dot for mark)
			Elements srcImgs = document.select("img[src$='']");

			for (Element page : srcImgs) {
				parsedUrl = ParsedUrl.parseUrl(page.attr("abs:src"));
				// https://github.com/iipc/urlcanon
				// Make sure that you work with canonicalized URLs only!
				Canonicalizer.SEMANTIC_PRECISE.canonicalize(parsedUrl);
				p = parsedUrl.toString();

				if (p.contains("#")) {
					p = splitHash(p);
				}
				if (p.length() > 1) {
					if (!p.contains("http://") && !p.contains("https://"))
						p = getBaseUrl(url) + "/" + p;
					if (originalSites.contains(getDomain(url)))
						saveImageToDB(url, p);
				}
			}
		}
	}

	public String splitHash(String p) {
		String[] tmp = p.split("#");
		return tmp[0];
	}

	public void extractLinks(Elements elements, String key) {
		String p;
		ParsedUrl parsedUrl;
		for (Element page : elements) {
			parsedUrl = ParsedUrl.parseUrl(page.attr(key));
			// https://github.com/iipc/urlcanon
			// Make sure that you work with canonicalized URLs only!
			Canonicalizer.SEMANTIC_PRECISE.canonicalize(parsedUrl);
			p = parsedUrl.toString();
			if (!p.contains("javascript") && p.contains("http")) {

				p = p.replace("document.location.href=", "");
				p = p.replace("location.href=", "");

				/* is URL valid*/
				try {
					String uri = new URL(p).toURI().toString();
					if (shouldIVisit(uri)) {
						frontier.add(new Frontier(uri, url));
					}
				}
				catch (Exception e) {
					if (logger)
						System.out.println("Not a link on onclick element, " + e.getMessage());
				}
			}
		}
	}

    // For each domain respect the robots.txt file if it exists.
    public String[] robots(String baseUrl) {

		String[] tmp = new String[2];

		try (InputStream robotsTxtStream = new URL(baseUrl + "/robots.txt").openStream()) {
			RobotsTxt robotsTxt = RobotsTxt.read(robotsTxtStream);
			tmp = new String[2];
			tmp[0] = robotsTxt.toString();
			robotsInfo.put(baseUrl, robotsTxt.toString());

			// crawl delay
			Grant grant = robotsTxt.ask(useragent, baseUrl);
			if (grant == null || grant.hasAccess()) {
				Integer crawlDelay = grant.getCrawlDelay();
				if (grant != null && crawlDelay != null) {
					robotsDelay.put(baseUrl, crawlDelay);
				} else {
					robotsDelay.put(baseUrl, DEFAULT_CRAWL_DELAY);
					if (logger)
						System.out.println("crawlDelay is null for page " + baseUrl);
				}
			}

			// site map
            List<String> sitemaps = robotsTxt.getSitemaps();
			if (!sitemaps.isEmpty()) {
                StringBuilder stringBuilder = new StringBuilder();

                for (String sitemap : sitemaps) {
                    WebResponse response = getWebResponse(sitemap);
                    if (response != null) {
                        stringBuilder.append(response.getContentAsString());

                        Document sitemapDoc = Jsoup.parse(response.getContentAsString(), "", Parser.xmlParser());
                        for (Element e : sitemapDoc.select("loc")) {
                            String p = e.toString();
                            p = p.replace("<loc>\n ", "");
                            p = p.replace("\n</loc>", "");

                            ParsedUrl parsedUrl = ParsedUrl.parseUrl(p);
                            // https://github.com/iipc/urlcanon
                            //	 Make sure that you work with canonicalized URLs only!
                            Canonicalizer.SEMANTIC_PRECISE.canonicalize(parsedUrl);
                            p = parsedUrl.toString();

							if (shouldIVisit(p)) {
								frontier.add(new Frontier(p, baseUrl));
								if (logger)
									System.out.println("New url " + p + " added to frontier from sitemap.");
							}
                        }
                    }
                }

				String sitemapsContent = stringBuilder.toString();
				if (sitemapsContent.equals("")) {
                    tmp[1] = null;
                }
				else {
				    tmp[1] = sitemapsContent;
                }
			}
			else {
				tmp[1] = null;
			}

			return tmp;
		} catch (Exception e) {
			if (logger) {
				System.out.println("Cannot parse robots.");
				e.printStackTrace();
			}
		}

		tmp[0] = null;
		tmp[1] = null;
		robotsDelay.put(baseUrl, DEFAULT_CRAWL_DELAY);

		return tmp;
    }

    private WebResponse getWebResponse(String url) {
		// first run with headless browser, so we can run potential JS
		WebClient webClient = new WebClient();
		WebClientOptions options = webClient.getOptions();
		options.setJavaScriptEnabled(true);
		options.setRedirectEnabled(true);
		options.setTimeout(TIMEOUT);

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

    private void savePageToDB(Document document, String pageType, int httpStatusCode) {

		//hash
		String hash = generateHash(document.toString());

		//PageEntity p = dbManager.duplicateExistsInDB(hash);

		if (hashCode.containsKey(hash)) {
			pageType = "DUPLICATE";

			String baseUrl = getDomain(url);

			dbManager.addPageToDB(pageType, baseUrl, url, "", httpStatusCode, new Timestamp(System.currentTimeMillis()), hash);
			dbManager.addLinkToDB(hashCode.get(hash), url);

		} else {
	    	if (pageType.equals("text/html"))
				pageType = "HTML";
			else
				pageType = "BINARY";

			hashCode.put(hash, url);

			String baseUrl = getDomain(url);
			dbManager.addPageToDB(pageType, baseUrl, url, document.toString(), httpStatusCode, new Timestamp(System.currentTimeMillis()), hash);
			dbManager.addLinkToDB(urlParent, url);
		}
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

	private String generateHash(String text)
	{

		String generatedText = null;
		try {
			// https://howtodoinjava.com/security/how-to-generate-secure-password-hash-md5-sha-pbkdf2-bcrypt-examples/
			// Create MessageDigest instance for MD5
			MessageDigest md = MessageDigest.getInstance("MD5");
			//Add text bytes to digest
			md.update(text.getBytes());
			//Get the hash's bytes
			byte[] bytes = md.digest();
			//This bytes[] has bytes in decimal format;
			//Convert it to hexadecimal format
			StringBuilder sb = new StringBuilder();
			for(int i=0; i< bytes.length ;i++)
			{
				sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
			}
			//Get complete hashed text in hex format
			generatedText = sb.toString();
		}
		catch (NoSuchAlgorithmException e)
		{
			e.printStackTrace();
		}
		return generatedText;
	}
}

