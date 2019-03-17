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

    public void truncateDatabase() throws Exception {
        List<String> tableNames = new ArrayList<>();
        Metamodel model = em.getMetamodel();

        for (EntityType entity : model.getEntities()) {
            Table classAnnotations = Class.forName(((EntityTypeImpl) entity).getTypeName()).getAnnotation(Table.class);
            String tableName = classAnnotations.name();
            String schemaName = classAnnotations.schema();
            tableNames.add(schemaName + "." + tableName);
        }

        em.getTransaction().begin();
        em.flush();
        for (String tableName : tableNames) {
            em.createNativeQuery("TRUNCATE TABLE " + tableName + " CASCADE").executeUpdate();
        }
        em.getTransaction().commit();
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