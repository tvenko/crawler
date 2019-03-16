package src.main.java.db;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Arrays;

@Entity
@Table(name = "image", schema = "crawldb", catalog = "crawldb")
public class ImageEntity {
    private int id;
    private Integer pageId;
    private String filename;
    private String contentType;
    private byte[] data;
    private Timestamp accessedTime;
    private PageEntity pageByPageId;

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Basic
    @Column(name = "page_id", insertable=false, updatable=false)
    public Integer getPageId() {
        return pageId;
    }

    public void setPageId(Integer pageId) {
        this.pageId = pageId;
    }

    @Basic
    @Column(name = "filename", nullable = true, length = 255)
    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    @Basic
    @Column(name = "content_type", nullable = true, length = 50)
    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    @Basic
    @Column(name = "data", nullable = true)
    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    @Basic
    @Column(name = "accessed_time", nullable = true)
    public Timestamp getAccessedTime() {
        return accessedTime;
    }

    public void setAccessedTime(Timestamp accessedTime) {
        this.accessedTime = accessedTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ImageEntity that = (ImageEntity) o;

        if (id != that.id) return false;
        if (pageId != null ? !pageId.equals(that.pageId) : that.pageId != null) return false;
        if (filename != null ? !filename.equals(that.filename) : that.filename != null) return false;
        if (contentType != null ? !contentType.equals(that.contentType) : that.contentType != null) return false;
        if (!Arrays.equals(data, that.data)) return false;
        if (accessedTime != null ? !accessedTime.equals(that.accessedTime) : that.accessedTime != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (pageId != null ? pageId.hashCode() : 0);
        result = 31 * result + (filename != null ? filename.hashCode() : 0);
        result = 31 * result + (contentType != null ? contentType.hashCode() : 0);
        result = 31 * result + Arrays.hashCode(data);
        result = 31 * result + (accessedTime != null ? accessedTime.hashCode() : 0);
        return result;
    }

    @ManyToOne
    @JoinColumn(name = "page_id", referencedColumnName = "id")
    public PageEntity getPageByPageId() {
        return pageByPageId;
    }

    public void setPageByPageId(PageEntity pageByPageId) {
        this.pageByPageId = pageByPageId;
    }
}
