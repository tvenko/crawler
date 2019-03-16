package src.main.java.db;

import javax.persistence.*;
import java.util.Collection;

@Entity
@Table(name = "page_type", schema = "crawldb", catalog = "crawldb")
public class PageTypeEntity {
    private String code;
    private Collection<PageEntity> pagesByCode;

    @Id
    @Column(name = "code", nullable = false, length = 20)
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PageTypeEntity that = (PageTypeEntity) o;

        if (code != null ? !code.equals(that.code) : that.code != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return code != null ? code.hashCode() : 0;
    }

    @OneToMany(mappedBy = "pageTypeByPageTypeCode")
    public Collection<PageEntity> getPagesByCode() {
        return pagesByCode;
    }

    public void setPagesByCode(Collection<PageEntity> pagesByCode) {
        this.pagesByCode = pagesByCode;
    }
}
