package main.java.db;

import javax.persistence.*;
import java.util.Arrays;

@Entity
@Table(name = "page_data", schema = "crawldb", catalog = "crawldb")
public class PageDataEntity {
    private int id;
    private Integer pageId;
    private String dataTypeCode;
    private byte[] data;
    private PageEntity pageByPageId;
    private DataTypeEntity dataTypeByDataTypeCode;

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
    @Column(name = "page_id", insertable=false, updatable=false, nullable = true)
    public Integer getPageId() {
        return pageId;
    }

    public void setPageId(Integer pageId) {
        this.pageId = pageId;
    }

    @Basic
    @Column(name = "data_type_code", insertable=false, updatable=false, length = 20)
    public String getDataTypeCode() {
        return dataTypeCode;
    }

    public void setDataTypeCode(String dataTypeCode) {
        this.dataTypeCode = dataTypeCode;
    }

    @Basic
    @Column(name = "data", nullable = true)
    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PageDataEntity that = (PageDataEntity) o;

        if (id != that.id) return false;
        if (pageId != null ? !pageId.equals(that.pageId) : that.pageId != null) return false;
        if (dataTypeCode != null ? !dataTypeCode.equals(that.dataTypeCode) : that.dataTypeCode != null) return false;
        if (!Arrays.equals(data, that.data)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (pageId != null ? pageId.hashCode() : 0);
        result = 31 * result + (dataTypeCode != null ? dataTypeCode.hashCode() : 0);
        result = 31 * result + Arrays.hashCode(data);
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

    @ManyToOne
    @JoinColumn(name = "data_type_code", referencedColumnName = "code")
    public DataTypeEntity getDataTypeByDataTypeCode() {
        return dataTypeByDataTypeCode;
    }

    public void setDataTypeByDataTypeCode(DataTypeEntity dataTypeByDataTypeCode) {
        this.dataTypeByDataTypeCode = dataTypeByDataTypeCode;
    }
}
