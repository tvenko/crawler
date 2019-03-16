package src.main.java.db;

import javax.persistence.*;
import java.util.Collection;

@Entity
@Table(name = "data_type", schema = "crawldb", catalog = "crawldb")
public class DataTypeEntity {
    private String code;
    private Collection<PageDataEntity> pageDataByCode;

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

        DataTypeEntity that = (DataTypeEntity) o;

        if (code != null ? !code.equals(that.code) : that.code != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return code != null ? code.hashCode() : 0;
    }

    @OneToMany(mappedBy = "dataTypeByDataTypeCode")
    public Collection<PageDataEntity> getPageDataByCode() {
        return pageDataByCode;
    }

    public void setPageDataByCode(Collection<PageDataEntity> pageDataByCode) {
        this.pageDataByCode = pageDataByCode;
    }
}
