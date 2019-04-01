package si.fri.db;

import javax.persistence.*;
import javax.transaction.Transactional;
import java.sql.Timestamp;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.*;


@Transactional(Transactional.TxType.REQUIRES_NEW)
public class DatabaseManager {

    private EntityManagerFactory emf;
    private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    public DatabaseManager(EntityManagerFactory emf) {
        this.emf = emf;
    }

    public void addPageToDB(String pageTypeCode, String baseUrl, String url, String htmlContent, int httpStatusCode, Timestamp timeAccessed, String hash) {

        // check if page already exists in db
        if (getPageByURL(url) == null) {

            EntityManager em = emf.createEntityManager();

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
                beginTx(em);
                em.persist(pageEntity);
                commitTx(em);
            } catch (Exception e) {
                rollbackTx(em);
                LOGGER.severe("Can't save to page db!");
                LOGGER.log(Level.SEVERE,e.getMessage(),e);
            } finally {
                em.close();
            }
        }

    }

    public void addPageDataToDB(String url, byte[] data, String code) {

        PageDataEntity pageDataEntity = new PageDataEntity();
        pageDataEntity.setData(data);
        PageEntity page = getPageByURL(url);
        DataTypeEntity dataType = getDataType(code);
        if (page != null && dataType != null) {

            EntityManager em = emf.createEntityManager();

            pageDataEntity.setPageByPageId(page);
            pageDataEntity.setDataTypeByDataTypeCode(dataType);

            try {
                beginTx(em);
                em.persist(pageDataEntity);
                commitTx(em);
                LOGGER.info("Document from page " + url + " saved to DB; size: " + data.length/8 + "B");
            } catch (Exception e) {
                rollbackTx(em);
                LOGGER.severe("Can't save to page_data db!");
                LOGGER.log(Level.SEVERE,e.getMessage(),e);
            } finally {
                em.close();
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

            EntityManager em = emf.createEntityManager();

            try {
                beginTx(em);
                em.persist(siteEntity);
                commitTx(em);
            } catch (Exception e) {
                rollbackTx(em);
                LOGGER.severe("Can't save to site db!");
                LOGGER.log(Level.SEVERE,e.getMessage(),e);
            } finally {
                em.close();
            }
        }
    }

    public void addLinkToDB(String fromUrl, String toUrl) {
        if (!hasLinksThisRecord(fromUrl, toUrl)) {

            EntityManager em = emf.createEntityManager();

            LinkEntity linkEntity = new LinkEntity();
            PageEntity fromPage = getPageByURL(fromUrl);
            PageEntity toPage = getPageByURL(toUrl);
            if (fromPage != null && toPage != null) {
                linkEntity.setToPage(toPage.getId());
                linkEntity.setFromPage(fromPage.getId());

                try {
                    beginTx(em);
                    em.persist(linkEntity);
                    commitTx(em);
                } catch (Exception e) {
                    rollbackTx(em);
                    LOGGER.severe("Can't save to link db!");
                    LOGGER.log(Level.SEVERE,e.getMessage(),e);
                } finally {
                    em.close();
                }
            }
        }
    }

    public void addImageToDB(String url, String fileName, String contentType, byte[] data, Timestamp timeAccessed) {
        ImageEntity imageEntity = new ImageEntity();
        PageEntity page = getPageByURL(url);
        if (page != null && !imageExistsInDB(page.getId(), fileName)) {

            EntityManager em = emf.createEntityManager();

            imageEntity.setPageByPageId(page);
            imageEntity.setContentType(contentType);
            imageEntity.setData(data);
            imageEntity.setAccessedTime(timeAccessed);
            imageEntity.setFilename(fileName);

            try {
                beginTx(em);
                em.persist(imageEntity);
                commitTx(em);
                LOGGER.info("Image from " + url + " saved to DB; size: " + data.length/8 + "B");
            } catch (Exception e) {
                rollbackTx(em);
                LOGGER.severe("Can't save to image db!");
                LOGGER.log(Level.SEVERE,e.getMessage(),e);
            } finally {
                em.close();
            }
        } else {
            LOGGER.info("IMAGE NOT ADDED TO DB: " + page.getId() + " : " + fileName);
        }
    }

    public void truncateDatabase() {
        LOGGER.info("truncating database");
        String schemaName = "crawldb";
        String[] tableNames = {"image", "link", "page", "page_data", "site"};

        EntityManager em = emf.createEntityManager();

        em.getTransaction().begin();
//        em.flush();
        for (String tableName : tableNames) {
            em.createNativeQuery("TRUNCATE TABLE " + schemaName + "." + tableName + " CASCADE").executeUpdate();
        }
        em.getTransaction().commit();

        em.close();
    }

    private PageTypeEntity getPageTypeEntityByCode(String code) {

        EntityManager em = emf.createEntityManager();

        try {
            return (PageTypeEntity)em.createQuery("SELECT p FROM PageTypeEntity p WHERE p.code = :code")
                    .setParameter("code", code)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        } finally {
            em.close();
        }
    }

    private SiteEntity getSiteByDomain(String domain) {

        EntityManager em = emf.createEntityManager();

        try {
            return (SiteEntity)em.createQuery("SELECT s FROM SiteEntity s WHERE s.domain = :domain")
                    .setParameter("domain", domain)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        } finally {
            em.close();
        }
    }

    private PageEntity getPageByURL(String url) {

        EntityManager em = emf.createEntityManager();

        try {
            return (PageEntity) em.createQuery("SELECT p FROM PageEntity p WHERE p.url = :url")
                    .setParameter("url", url)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        } finally {
            em.close();
        }
    }

    private DataTypeEntity getDataType(String code) {

        EntityManager em = emf.createEntityManager();

        try {
            return (DataTypeEntity)em.createQuery("SELECT dt FROM DataTypeEntity dt WHERE dt.code = :code")
                    .setParameter("code", code)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        } finally {
            em.close();
        }
    }

    public PageEntity duplicateExistsInDB(String hash) {

        EntityManager em = emf.createEntityManager();

        try {
            return (PageEntity)em.createQuery("SELECT p FROM PageEntity p WHERE p.hash = :hash")
                    .setParameter("hash", hash)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        } finally {
            em.close();
        }
    }

    private boolean hasLinksThisRecord(String fromUrl, String toUrl) {

        EntityManager em = emf.createEntityManager();

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
        } finally {
            em.close();
        }
    }

    private boolean imageExistsInDB(int pageId, String imageName) {

        EntityManager em = emf.createEntityManager();

        try {
            List results = em.createQuery("SELECT i FROM ImageEntity i WHERE i.pageId = :pageId AND i.filename = :imageName")
                    .setParameter("pageId", pageId)
                    .setParameter("imageName", imageName)
                    .getResultList();
            return !results.isEmpty();
        } catch (NoResultException e) {
            return false;
        } finally {
            em.close();
        }
    }

    private void beginTx(EntityManager em) {
        if (!em.getTransaction().isActive())
            em.getTransaction().begin();
    }

    private void commitTx(EntityManager em) {
        if (em.getTransaction().isActive())
            em.getTransaction().commit();
    }

    private void rollbackTx(EntityManager em) {
        if (em.getTransaction().isActive())
            em.getTransaction().rollback();
    }
}