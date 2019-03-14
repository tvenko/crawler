package src.main.java.db;

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

        em.getTransaction().begin();
        em.persist(pageEntity);
        em.getTransaction().commit();
    }
}