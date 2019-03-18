package si.fri.db;

import org.hibernate.metamodel.model.domain.internal.EntityTypeImpl;

import javax.persistence.*;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.Metamodel;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {

    private EntityManager em;

    public DatabaseManager() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("crawler JPA");
        em = emf.createEntityManager();
    }

    public void addPageToDB(String pageTypeCode, String url, String htmlContent, int httpStatusCode, Timestamp timeAccessed) {
        PageEntity pageEntity = new PageEntity();
        pageEntity.setUrl(url);
        pageEntity.setHtmlContent(htmlContent);
        pageEntity.setHttpStatusCode(httpStatusCode);
        pageEntity.setAccessedTime(timeAccessed);
        PageTypeEntity pageType = getPageTypeEntityByCode(pageTypeCode);
        if (pageType != null)
            pageEntity.setPageTypeByPageTypeCode(pageType);

        em.getTransaction().begin();
        em.persist(pageEntity);
        em.getTransaction().commit();

//        //FIXME
//        try {
////            beginTx();
//            em.persist(pageEntity);
////            commitTx();
//        } catch (Exception e) {
////            rollbackTx();
//            System.out.println("Can't save to db!");
//        }
    }

    public void truncateDatabase() {
        String schemaName = "crawldb";
        String[] tableNames = {"image", "link", "page", "page_data", "site"};

        em.getTransaction().begin();
        em.flush();
        for (String tableName : tableNames) {
            em.createNativeQuery("TRUNCATE TABLE " + schemaName + "." + tableName + " CASCADE").executeUpdate();
        }
        em.getTransaction().commit();
    }

    private PageTypeEntity getPageTypeEntityByCode(String code) {
        List<PageTypeEntity> reuslts = em.createQuery("SELECT p FROM PageTypeEntity p WHERE p.code = :code")
                .setParameter("code", code)
                .getResultList();
        if (!reuslts.isEmpty())
            return reuslts.get(0);
        return null;
    }

    private void beginTx() {
        if (!em.getTransaction().isActive())
            em.getTransaction().begin();
    }

    private void commitTx() {
        if (em.getTransaction().isActive())
            em.getTransaction().commit();
    }

    private void rollbackTx() {
        if (em.getTransaction().isActive())
            em.getTransaction().rollback();
    }
}