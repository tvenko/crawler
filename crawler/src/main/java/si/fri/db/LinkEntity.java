package si.fri.db;

import javax.persistence.*;

@Entity
@Table(name = "link", schema = "crawldb", catalog = "crawldb")
@IdClass(LinkEntityPK.class)
public class LinkEntity {
    private int fromPage;
    private int toPage;
//    private PageEntity pageByFromPage;
//    private PageEntity pageByToPage;

    @Id
//    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name = "from_page", nullable = false)
    public int getFromPage() {
        return fromPage;
    }

    public void setFromPage(int fromPage) {
        this.fromPage = fromPage;
    }

    @Id
    @Column(name = "to_page", nullable = false)
    public int getToPage() {
        return toPage;
    }

    public void setToPage(int toPage) {
        this.toPage = toPage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LinkEntity that = (LinkEntity) o;

        if (fromPage != that.fromPage) return false;
        if (toPage != that.toPage) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = fromPage;
        result = 31 * result + toPage;
        return result;
    }

//    @ManyToOne
//    @JoinColumn(name = "from_page", referencedColumnName = "id", nullable = false)
//    public PageEntity getPageByFromPage() {
//        return pageByFromPage;
//    }
//
//    public void setPageByFromPage(PageEntity pageByFromPage) {
//        this.pageByFromPage = pageByFromPage;
//    }
//
//    @ManyToOne
//    @JoinColumn(name = "to_page", referencedColumnName = "id", nullable = false)
//    public PageEntity getPageByToPage() {
//        return pageByToPage;
//    }
//
//    public void setPageByToPage(PageEntity pageByToPage) {
//        this.pageByToPage = pageByToPage;
//    }
}
