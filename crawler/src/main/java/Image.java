package main.java;

import java.util.Date;

public class Image {

    //@Id
    //@GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    //page id

    public String filename;

    public String content_type;

    //??
    public byte data;

    public Date accessed_time;

    //get / set

    public void setFilename(String filename)
    {
        this.filename = filename;
    }

    public String getFilename()
    {
        return filename;
    }

    //TODO
}
