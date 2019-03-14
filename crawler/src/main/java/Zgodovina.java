package src.main.java;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Zgodovina {

    //Url naslov
    public String url;

    public String timeStamp;

    public String urlParent;

    //kolikokrat smo stran videli
    public int n;

    public Zgodovina(String url, String urlParent)
    {
        this.url = url;
        this.urlParent = urlParent;
        this.timeStamp = (new SimpleDateFormat("yyyy.MM.dd HH:mm:ss")).format(new Date());
        this.n = 0;
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

    public void setTimeStamp(String timeStamp)
    {
        this.timeStamp = timeStamp;
    }

    public String getTimeStamp()
    {
        return this.timeStamp;
    }

    public void setN(int n)
    {
        this.n = n;
    }

    public int getN()
    {
        return this.n;
    }

}
