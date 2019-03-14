package src.main.java.db;

import javax.persistence.*;
import java.util.Collection;

@Entity
@Table(name = "site", schema = "crawldb", catalog = "crawldb")
public class SiteEntity {
    private int id;
    private String domain;
    private String robotsContent;
    private String sitemapContent;
    private Collection<PageEntity> pagesById;

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
    @Column(name = "domain", nullable = true, length = 500)
    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    @Basic
    @Column(name = "robots_content", nullable = true, length = -1)
    public String getRobotsContent() {
        return robotsContent;
    }

    public void setRobotsContent(String robotsContent) {
        this.robotsContent = robotsContent;
    }

    @Basic
    @Column(name = "sitemap_content", nullable = true, length = -1)
    public String getSitemapContent() {
        return sitemapContent;
    }

    public void setSitemapContent(String sitemapContent) {
        this.sitemapContent = sitemapContent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SiteEntity that = (SiteEntity) o;

        if (id != that.id) return false;
        if (domain != null ? !domain.equals(that.domain) : that.domain != null) return false;
        if (robotsContent != null ? !robotsContent.equals(that.robotsContent) : that.robotsContent != null)
            return false;
        if (sitemapContent != null ? !sitemapContent.equals(that.sitemapContent) : that.sitemapContent != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (domain != null ? domain.hashCode() : 0);
        result = 31 * result + (robotsContent != null ? robotsContent.hashCode() : 0);
        result = 31 * result + (sitemapContent != null ? sitemapContent.hashCode() : 0);
        return result;
    }

    @OneToMany(mappedBy = "siteBySiteId")
    public Collection<PageEntity> getPagesById() {
        return pagesById;
    }

    public void setPagesById(Collection<PageEntity> pagesById) {
        this.pagesById = pagesById;
    }
}
