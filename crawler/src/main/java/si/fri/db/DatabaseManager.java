package si.fri.db;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.sql.Timestamp;

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

        try {
            beginTx();
            em.persist(pageEntity);
            commitTx();
        } catch (Exception e) {
            rollbackTx();
        }
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