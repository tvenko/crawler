package si.fri.db;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.*;

public class DatabaseManager {

    private EntityManager em;
    private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    public DatabaseManager() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("crawler JPA");
        em = emf.createEntityManager();
    }

    public void addPageToDB(String pageTypeCode, String baseUrl, String url, String htmlContent, int httpStatusCode, Timestamp timeAccessed, String hash) {
        // check if page already exists in db
        if (getPageByURL(url) == null) {
            PageEntity pageEntity = new PageEntity();
            pageEntity.setUrl(url);
            pageEntity.setHtmlContent(htmlContent);
            pageEntity.setHttpStatusCode(httpStatusCode);
            pageEntity.setAccessedTime(timeAccessed);

            //hash
            pageEntity.setHash(hash);

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
                LOGGER.severe("Can't save to page db!");
                LOGGER.log(Level.SEVERE,e.getMessage(),e);
            }
        }
    }

    public void addPageDataToDB(String url, byte[] data, String code) {
        PageDataEntity pageDataEntity = new PageDataEntity();
        pageDataEntity.setData(data);
        PageEntity page = getPageByURL(url);
        DataTypeEntity dataType = getDataType(code);
        if (page != null && dataType != null) {
            pageDataEntity.setPageByPageId(page);
            pageDataEntity.setDataTypeByDataTypeCode(dataType);

            try {
                beginTx();
                em.persist(pageDataEntity);
                commitTx();
                LOGGER.info("Document from page " + url + " saved to DB; size: " + data.length/8 + "B");
            } catch (Exception e) {
                rollbackTx();
                LOGGER.severe("Can't save to page_data db!");
                LOGGER.log(Level.SEVERE,e.getMessage(),e);
            }
        } else {
            LOGGER.info("Page " + url + " is already in DB!");
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
                LOGGER.severe("Can't save to site db!");
                LOGGER.log(Level.SEVERE,e.getMessage(),e);
            }
        }
    }

    public void addLinkToDB(String fromUrl, String toUrl) {
        if (!hasLinksThisRecord(fromUrl, toUrl)) {
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
                    LOGGER.severe("Can't save to link db!");
                    LOGGER.log(Level.SEVERE,e.getMessage(),e);
                }
            }
        }
    }

    public void addImageToDB(String url, String fileName, String contentType, byte[] data, Timestamp timeAccessed) {
        ImageEntity imageEntity = new ImageEntity();
        PageEntity page = getPageByURL(url);
        if (page != null && !imageExistsInDB(page.getId(), fileName)) {
            imageEntity.setPageByPageId(page);
            imageEntity.setContentType(contentType);
            imageEntity.setData(data);
            imageEntity.setAccessedTime(timeAccessed);
            imageEntity.setFilename(fileName);

            try {
                beginTx();
                em.persist(imageEntity);
                commitTx();
                LOGGER.info("Image from " + url + " saved to DB; size: " + data.length/8 + "B");
            } catch (Exception e) {
                rollbackTx();
                LOGGER.severe("Can't save to image db!");
                LOGGER.log(Level.SEVERE,e.getMessage(),e);
            }
        } else {
            LOGGER.info("IMAGE NOT ADDED TO DB: " + page.getId() + " : " + fileName);
        }
    }

    public void truncateDatabase() {
        LOGGER.info("truncating database");
        String schemaName = "crawldb";
        String[] tableNames = {"image", "link", "page", "page_data", "site"};

        em.getTransaction().begin();
//        em.flush();
        for (String tableName : tableNames) {
            em.createNativeQuery("TRUNCATE TABLE " + schemaName + "." + tableName + " CASCADE").executeUpdate();
        }
        em.getTransaction().commit();
    }

    private PageTypeEntity getPageTypeEntityByCode(String code) {
        try {
            return (PageTypeEntity)em.createQuery("SELECT p FROM PageTypeEntity p WHERE p.code = :code")
                    .setParameter("code", code)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    private SiteEntity getSiteByDomain(String domain) {
        try {
            return (SiteEntity)em.createQuery("SELECT s FROM SiteEntity s WHERE s.domain = :domain")
                    .setParameter("domain", domain)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    private PageEntity getPageByURL(String url) {
        try {
            return (PageEntity) em.createQuery("SELECT p FROM PageEntity p WHERE p.url = :url")
                    .setParameter("url", url)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    private DataTypeEntity getDataType(String code) {
        try {
            return (DataTypeEntity)em.createQuery("SELECT dt FROM DataTypeEntity dt WHERE dt.code = :code")
                    .setParameter("code", code)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public PageEntity duplicateExistsInDBHash(String hash) {
        try {
            return (PageEntity)em.createQuery("SELECT p FROM PageEntity p WHERE p.hash = :hash")
                    .setParameter("hash", hash)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public boolean duplicateExistsInDBUrl(String url) {
        try {
            List results = em.createQuery("SELECT p FROM PageEntity p WHERE p.url = :url")
                    .setParameter("url", url)
                    .getResultList();
            return results.isEmpty();
        }
        catch (Exception e) {
            return true;
        }

    }

    private boolean hasLinksThisRecord(String fromUrl, String toUrl) {
        try {
            PageEntity fromPageId = (PageEntity) em.createQuery("SELECT p FROM PageEntity p WHERE p.url = :url")
                    .setParameter("url", fromUrl).getSingleResult();
            PageEntity toPageId = (PageEntity) em.createQuery("SELECT p FROM PageEntity p WHERE p.url = :url")
                    .setParameter("url", toUrl).getSingleResult();
            if (fromPageId != null && toPageId != null) {
                LinkEntity result = (LinkEntity) em.createQuery("SELECT l FROM LinkEntity l WHERE l.fromPage = :fromUrl AND l.toPage = :toUrl")
                        .setParameter("fromUrl", fromPageId.getId())
                        .setParameter("toUrl", toPageId.getId())
                        .getSingleResult();
                return result != null;
            }
            return false;
        } catch (NoResultException e) {
            return false;
        }
    }

    private boolean imageExistsInDB(int pageId, String imageName) {
        List results = em.createQuery("SELECT i FROM ImageEntity i WHERE i.pageId = :pageId AND i.filename = :imageName")
                .setParameter("pageId", pageId)
                .setParameter("imageName", imageName)
                .getResultList();
        return !results.isEmpty();
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