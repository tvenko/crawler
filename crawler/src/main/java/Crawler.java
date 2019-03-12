import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Crawler
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
    public ArrayList<String> frontier;
    public ArrayList<String> preiskana;
	public ArrayList<String> images;
	public Elements documents;

    public void crawl()
    {
    	// Strani, ki jih moramo preiskati
    	frontier = new ArrayList<String>();
        preiskana = new ArrayList<String>();
		images = new ArrayList<String>();

		//Set<String> result = new HashSet<>();

        //seznam dokumentov
		documents = new Elements();

        //frontier.add("http://evem.gov.si/evem/drzavljani/zacetna.evem");
        //frontier.add("https://e-uprava.gov.si/");
        //frontier.add("https://podatki.gov.si/");
        frontier.add("http://www.e-prostor.gov.si/");

        while(!frontier.isEmpty()) {
            visit(frontier.get(0));
            frontier.remove(0);
        }
    }
    
    public void visit(String url)
    {
        if(!preiskana.contains(url)) {
            try {
                preiskana.add(url);
                
                // Fetch the HTML code
				System.out.println("url: " + url + "------" + frontier.size());
                Document document = Jsoup.connect(url).get();
                
                // Parse the HTML to extract links to other URLs
                Elements linksOnPage = document.select("a[href]");

                // img with src ending .png
                Elements pngs = document.select("img[src$=.png]");

				// img with src ending .jpg
				Elements jpgs = document.select("img[src$=.jpg]");

				// For each extracted png image.
				for (Element s : pngs) {
					//System.out.println("SRC do slike (png): " + s.attr("abs:src"));
					images.add(s.attr("abs:src"));
				}

				// For each extracted jpg image
				for (Element s : jpgs) {
					//System.out.println("SRC do slike (jpg): " + s.attr("abs:src"));
					images.add(s.attr("abs:src"));
				}

				String p;

                // For each extracted URL...
                for (Element page : linksOnPage) {
					p = page.attr("abs:href");
                	if(!p.contains("#") &&
							!p.contains("?") &&
							!frontier.contains(p) &&
							p.length() > 1) {
                		if(p.contains(".pdf") || p.contains(".doc") || p.contains(".docx") || p.contains(".ppt") || p.contains(".pptx"))
							documents.add(page);
                		else if((p.contains("http://") || p.contains("https://")) &&
							   (!p.contains(".zip") && !p.contains(".xlsx") &&
								!p.contains(".xls") && !p.contains(".pps") &&
								!p.contains(".jpg") && !p.contains(".png") &&
								!p.contains(".jspx") && !p.contains(".jsp") &&
								!p.contains(".mp4") && !p.contains(".exe")))
							frontier.add((page.attr("abs:href")));
					}
                }

				// For each domain respect the robots.txt file if it exists.
				//TODO A je pravilno, da je na tem mestu robots ??
				robots(url);

            } catch (IOException e) {
                System.err.println("For '" + url + "': " + e.getMessage());
            }
        }
    }
    
    // For each domain respect the robots.txt file if it exists.
    public void robots(String url)
    {
		String robot = url + "/robots.txt";
    	if(!preiskana.contains(url)) {
    		// JSoup is really built for reading and parsing HTML files. The robots.txt file is not an HTML file and would be better to be read by a simple input stream. 
    		try {
    			BufferedReader in = new BufferedReader(new InputStreamReader(new URL(robot).openStream()));
    	        String subLink = null;

    	        System.out.println("Ma kaj odstranimo ?  "+frontier.size());
    	        while((subLink = in.readLine()) != null) {
    	        	
    	        	// If a sitemap is defined, all the URLs that are defined within it, should be added to the frontier.
    	        	if (subLink.toLowerCase().contains("sitemap")){
    	        		//System.out.println("---------------------"+subLink.split("map: ")[1]);
    	                frontier.add(subLink.split("map: ")[1]);
    	            }

    	            //TODO kul?
					// If a Disallow is defined, all the URLs that are disallow should be removed from the frontier
					if (subLink.toLowerCase().contains("disallow")){
						System.out.println(subLink.split("allow: ")[1]);
						System.out.println("----disallow pages: "+url + subLink.split("allow: ")[1]);
						frontier.remove(url + "/" + subLink.replace("Disallow: /", ""));
					}

    	        }
				System.out.println("Maybe ?  "+frontier.size());
    	    } catch (IOException e) {
    	    	System.err.println("For '" + url + "': " + e.getMessage());
    	    }	
        }
    }
}

