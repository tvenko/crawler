package si.fri.db;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.List;

public class DatabaseManager {

    private EntityManager em;

    public DatabaseManager() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("crawler JPA");
        em = emf.createEntityManager();
    }

    public void addPageToDB(String pageTypeCode, String baseUrl, String url, String htmlContent, int httpStatusCode, Timestamp timeAccessed) {
        // check if page already exists in db
        if (getPageByURL(url) == null) {
            PageEntity pageEntity = new PageEntity();
            pageEntity.setUrl(url);
            pageEntity.setHtmlContent(htmlContent);
            pageEntity.setHttpStatusCode(httpStatusCode);
            pageEntity.setAccessedTime(timeAccessed);
            PageTypeEntity pageType = getPageTypeEntityByCode(pageTypeCode);
            if (pageType != null)
                pageEntity.setPageTypeByPageTypeCode(pageType);
            SiteEntity site = getSiteByDomain(baseUrl);
            if (site != null)
                pageEntity.setSiteBySiteId(site);

            try {
                beginTx();
                em.persist(pageEntity);
                commitTx();
            } catch (Exception e) {
                rollbackTx();
                System.out.println("Can't save to page db!");
            }
        }
    }

    public void addSiteToDB(String domain, String robots, String siteMap) {
//        check if site already exits in db
        if (getSiteByDomain(domain) == null) {
            SiteEntity siteEntity = new SiteEntity();
            siteEntity.setDomain(domain);
            siteEntity.setRobotsContent(robots);
            siteEntity.setSitemapContent(siteMap);

            try {
                beginTx();
                em.persist(siteEntity);
                commitTx();
            } catch (Exception e) {
                rollbackTx();
                System.out.println("Can't save site to db!");
            }
        }
    }

    public void addLinkToDB(String fromUrl, String toUrl) {
        LinkEntity linkEntity = new LinkEntity();
        PageEntity fromPage = getPageByURL(fromUrl);
        PageEntity toPage = getPageByURL(toUrl);
        if (fromPage != null && toPage != null) {
            linkEntity.setToPage(toPage.getId());
            linkEntity.setFromPage(fromPage.getId());

            try {
                beginTx();
                em.persist(linkEntity);
                commitTx();
            } catch (Exception e) {
                rollbackTx();
                System.out.println("Can't save link to db!");
            }
        }
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
        List<PageTypeEntity> results = em.createQuery("SELECT p FROM PageTypeEntity p WHERE p.code = :code")
                .setParameter("code", code)
                .getResultList();
        if (!results.isEmpty())
            return results.get(0);
        return null;
    }

    private SiteEntity getSiteByDomain(String domain) {
        List<SiteEntity> results =  em.createQuery("SELECT s FROM SiteEntity s WHERE s.domain = :domain")
                .setParameter("domain", domain)
                .getResultList();
        if (!results.isEmpty())
            return results.get(0);
        return null;
    }

    private PageEntity getPageByURL(String url) {
        List<PageEntity> results = em.createQuery("SELECT p FROM PageEntity p WHERE p.url = :url")
                .setParameter("url", url)
                .getResultList();
        if (!results.isEmpty())
            return results.get(0);
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