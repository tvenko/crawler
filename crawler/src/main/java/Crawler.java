import java.util.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;

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
	 */

	final static Pattern urlPattern = Pattern.compile("(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");

	private final String url;
	private final ExecutorService executor;
	private final Map<String, Zgodovina> zgodovina;

	private final ArrayList<String> frontier;

	public Crawler(String url, ExecutorService executor, Map<String, Zgodovina> zgodovina) {
		this.url = url;
		this.executor = executor;
		this.zgodovina = zgodovina;
		this.frontier = new ArrayList<String>();
	}

	public void run() {
		visit();
		robots();
		for (String u : frontier) {
			synchronized(zgodovina) {
				if (zgodovina.containsKey(u)) {
					zgodovina.get(u).n++;
				} else {
					zgodovina.put(u, new Zgodovina(u, url));
					executor.submit(new Crawler(u, executor, zgodovina));
				}
			}
		}
	}

	public void visit()
	{
		try {

			// Fetch the HTML code
			System.out.println("-------------"+"url: " + url);
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
						frontier.add(p);
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

		} catch (IOException e) {
			System.err.println("For '" + url + "': " + e.getMessage());
		}
	}

    // For each domain respect the robots.txt file if it exists.
    public void robots()
    {
		String robots = url + "/robots.txt";
    	try {
			BufferedReader in = new BufferedReader(new InputStreamReader(new URL(robots).openStream()));
			String subLink = null;


			while((subLink = in.readLine()) != null) {

				// If a sitemap is defined, all the URLs that are defined within it, should be added to the frontier.
				if (subLink.toLowerCase().contains("sitemap")){
					//System.out.println("---------------------"+subLink.split("map: ")[1]);
					frontier.add(subLink.split("map: ")[1]);
				}

				//TODO
				// If a Disallow is defined, all the URLs that are disallow should be removed from the frontier
				if (subLink.toLowerCase().contains("disallow")){
					System.out.println("----disallow pages: "+url + subLink.split("allow: ")[1]);
					frontier.remove(url + subLink.split("allow: ")[1]);
				}

			}
		} catch (IOException e) {
			System.err.println("For '" + url + "': " + e.getMessage());
		}
    }
}

