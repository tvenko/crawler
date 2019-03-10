package crawl.crawler;

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

    public void crawl()
    {
    	// Strani, ki jih moramo preiskati
    	frontier = new ArrayList<String>();
        preiskana = new ArrayList<String>();
        
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
                Document document = Jsoup.connect(url).get();

                // For each domain respect the robots.txt file if it exists. 
                robots(url);
                
                // Parse the HTML to extract links to other URLs
                Elements linksOnPage = document.select("a[href]");

                // img with src ending .png
                Elements pngs = document.select("img[src$=.png]");
                	
                // For each extracted URL...
                for (Element page : linksOnPage) {
                	if(!frontier.contains((page.attr("abs:href"))))
                		frontier.add((page.attr("abs:href")));
                }
            } catch (IOException e) {
                System.err.println("For '" + url + "': " + e.getMessage());
            }
        }
    }
    
    // For each domain respect the robots.txt file if it exists.
    public void robots(String url)
    {
    	url = url + "/robots.txt";
    	if(!preiskana.contains(url)) {
    		// JSoup is really built for reading and parsing HTML files. The robots.txt file is not an HTML file and would be better to be read by a simple input stream. 
    		try {
    			BufferedReader in = new BufferedReader(new InputStreamReader(new URL(url).openStream()));
    	        String subLink = null;
    	        while((subLink = in.readLine()) != null) {
    	        	
    	        	// If a sitemap is defined, all the URLs that are defined within it, should be added to the frontier.
    	        	if (subLink.toLowerCase().contains("sitemap")){
    	        		//System.out.println(subLink.split("map: ")[1]);
    	                frontier.add(subLink.split("map: ")[1]);
    	            }

    	        }
    	    } catch (IOException e) {
    	    	System.err.println("For '" + url + "': " + e.getMessage());
    	    }	
        }
    }
}

