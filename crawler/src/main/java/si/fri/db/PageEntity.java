package si.fri.db;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Collection;

@Entity
@Table(name = "page", schema = "crawldb", catalog = "crawldb")
public class PageEntity {
    private int id;
    private Integer siteId;
    private String pageTypeCode;
    private String url;
    private String htmlContent;
    private Integer httpStatusCode;
    private Timestamp accessedTime;
    private Collection<ImageEntity> imagesById;
//    private Collection<LinkEntity> linksById;
//    private Collection<LinkEntity> linksById_0;
    private SiteEntity siteBySiteId;
    private PageTypeEntity pageTypeByPageTypeCode;
    private Collection<PageDataEntity> pageDataById;

    private String hash;

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
    @Column(name = "site_id", insertable=false, updatable=false, nullable = true)
    public Integer getSiteId() {
        return siteId;
    }

    public void setSiteId(Integer siteId) {
        this.siteId = siteId;
    }

    @Basic
    @Column(name = "page_type_code", insertable=false, updatable=false, nullable = true, length = 20)
    public String getPageTypeCode() {
        return pageTypeCode;
    }

    public void setPageTypeCode(String pageTypeCode) {
        this.pageTypeCode = pageTypeCode;
    }

    @Basic
    @Column(name = "url", nullable = true, length = 3000)
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Basic
    @Column(name = "html_content", nullable = true, length = -1)
    public String getHtmlContent() {
        return htmlContent;
    }

    public void setHtmlContent(String htmlContent) {
        this.htmlContent = htmlContent;
    }

    @Basic
    @Column(name = "http_status_code", nullable = true)
    public Integer getHttpStatusCode() {
        return httpStatusCode;
    }

    public void setHttpStatusCode(Integer httpStatusCode) {
        this.httpStatusCode = httpStatusCode;
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

        PageEntity that = (PageEntity) o;

        if (id != that.id) return false;
        if (siteId != null ? !siteId.equals(that.siteId) : that.siteId != null) return false;
        if (pageTypeCode != null ? !pageTypeCode.equals(that.pageTypeCode) : that.pageTypeCode != null) return false;
        if (url != null ? !url.equals(that.url) : that.url != null) return false;
        if (htmlContent != null ? !htmlContent.equals(that.htmlContent) : that.htmlContent != null) return false;
        if (httpStatusCode != null ? !httpStatusCode.equals(that.httpStatusCode) : that.httpStatusCode != null)
            return false;
        if (accessedTime != null ? !accessedTime.equals(that.accessedTime) : that.accessedTime != null) return false;

        if (hash != null ? !hash.equals(that.hash) : that.hash != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (siteId != null ? siteId.hashCode() : 0);
        result = 31 * result + (pageTypeCode != null ? pageTypeCode.hashCode() : 0);
        result = 31 * result + (url != null ? url.hashCode() : 0);
        result = 31 * result + (htmlContent != null ? htmlContent.hashCode() : 0);
        result = 31 * result + (httpStatusCode != null ? httpStatusCode.hashCode() : 0);
        result = 31 * result + (accessedTime != null ? accessedTime.hashCode() : 0);
        result = 31 * result + (hash != null ? hash.hashCode() : 0);
        return result;
    }

    @OneToMany(mappedBy = "pageByPageId")
    public Collection<ImageEntity> getImagesById() {
        return imagesById;
    }

    public void setImagesById(Collection<ImageEntity> imagesById) {
        this.imagesById = imagesById;
    }

//    @OneToMany(mappedBy = "pageByFromPage")
//    public Collection<LinkEntity> getLinksById() {
//        return linksById;
//    }
//
//    public void setLinksById(Collection<LinkEntity> linksById) {
//        this.linksById = linksById;
//    }
//
//    @OneToMany(mappedBy = "pageByToPage")
//    public Collection<LinkEntity> getLinksById_0() {
//        return linksById_0;
//    }
//
//    public void setLinksById_0(Collection<LinkEntity> linksById_0) {
//        this.linksById_0 = linksById_0;
//    }

    @ManyToOne
    @JoinColumn(name = "site_id", referencedColumnName = "id")
    public SiteEntity getSiteBySiteId() {
        return siteBySiteId;
    }

    public void setSiteBySiteId(SiteEntity siteBySiteId) {
        this.siteBySiteId = siteBySiteId;
    }

    @ManyToOne
    @JoinColumn(name = "page_type_code", referencedColumnName = "code")
    public PageTypeEntity getPageTypeByPageTypeCode() {
        return pageTypeByPageTypeCode;
    }

    public void setPageTypeByPageTypeCode(PageTypeEntity pageTypeByPageTypeCode) {
        this.pageTypeByPageTypeCode = pageTypeByPageTypeCode;
    }

    @OneToMany(mappedBy = "pageByPageId")
    public Collection<PageDataEntity> getPageDataById() {
        return pageDataById;
    }

    public void setPageDataById(Collection<PageDataEntity> pageDataById) {
        this.pageDataById = pageDataById;
    }


    @Basic
    @Column(name = "hash", nullable = true)
    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }
}
