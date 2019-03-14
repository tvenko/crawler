package main.java;

public class Frontier {

    public String url;
    public String urlParent;

    public Frontier(String url, String urlParent)
    {
        this.url = url;
        this.urlParent = urlParent;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    public String getUrl()
    {
        return this.url;
    }

    public void setUrlParent(String urlParent)
    {
        this.urlParent = urlParent;
    }

    public String getUrlParent()
    {
        return this.urlParent;
    }
}
