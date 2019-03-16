package src.main.java.db;

import javax.persistence.Column;
import javax.persistence.Id;
import java.io.Serializable;

public class LinkEntityPK implements Serializable {
    private int fromPage;
    private int toPage;

    @Column(name = "from_page", insertable=false, updatable=false)
    @Id
    public int getFromPage() {
        return fromPage;
    }

    public void setFromPage(int fromPage) {
        this.fromPage = fromPage;
    }

    @Column(name = "to_page", insertable=false, updatable=false)
    @Id
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

        LinkEntityPK that = (LinkEntityPK) o;

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
}
