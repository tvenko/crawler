import psycopg2 as db
import numpy as np

pages_names = {}
link_count = {}


def connect_to_db():
    return db.connect(host="localhost", database="crawldb", user="postgres", password="admin")


def get_pages_for_site(site_id):
    conn = connect_to_db()
    cur = conn.cursor()
    cur.execute("SELECT p.id, p.site_id, p.url FROM crawldb.page p WHERE p.site_id = " + str(site_id))
    row = cur.fetchone()

    pages = []
    while row is not None:
        pages.append(row)
        row = cur.fetchone()
    cur.close()
    return pages


def get_pages_count_for_site(site_id):
    conn = connect_to_db()
    cur = conn.cursor()
    cur.execute("SELECT COUNT(*) FROM crawldb.page p WHERE p.site_id = " + str(site_id))
    row = cur.fetchone()

    return row[0]


def get_linked_pages(pages):

    str_pages = ""
    for page in pages:
        str_pages += str(page) + ", "
    str_pages = str_pages[:-2]

    conn = connect_to_db()
    cur = conn.cursor()
    cur.execute("SELECT l.to_page FROM crawldb.link l WHERE l.from_page IN (" + str_pages + ")")
    row = cur.fetchone()

    linked_pages = []
    while row is not None:
        linked_pages.append(row)
        row = cur.fetchone()
    cur.close()

    cur = conn.cursor()
    cur.execute("SELECT l.from_page FROM crawldb.link l WHERE l.to_page IN (" + str_pages + ")")
    row = cur.fetchone()

    while row is not None:
        linked_pages.append(row)
        row = cur.fetchone()
    cur.close()

    return linked_pages


def get_linked_pages_for_page(page_id):
    conn = connect_to_db()
    cur = conn.cursor()
    cur.execute("SELECT l.to_page FROM crawldb.link l WHERE l.from_page = " + str(page_id))
    row = cur.fetchone()

    linked_pages = []
    while row is not None:
        linked_pages.append(row[0])
        row = cur.fetchone()
    cur.close()
    return linked_pages


def get_sites_for_pages(pages):
    print("getting sites for pages")
    str_pages = ""
    for page in pages:
        str_pages += str(page) + ", "
    str_pages = str_pages[:-2]

    conn = connect_to_db()
    cur = conn.cursor()
    cur.execute("SELECT p.site_id FROM crawldb.page p WHERE p.id IN (" + str_pages + ") GROUP BY p.site_id")
    row = cur.fetchone()

    linked_sites = []
    while row is not None:
        linked_sites.append(row[0])
        row = cur.fetchone()
    cur.close()

    return linked_sites


def get_sites():
    conn = connect_to_db()
    cur = conn.cursor()
    cur.execute("SELECT s.id, s.domain FROM crawldb.site s WHERE s.id IN "
                "(SELECT p.site_id FROM crawldb.page p GROUP BY p.site_id)")

    print("number of sites with at least one visited page: ", cur.rowcount)
    row = cur.fetchone()
    sites = []
    while row is not None:
        sites.append(row)
        row = cur.fetchone()

    cur.close()
    return sites


def get_site_by_id(id):
    conn = connect_to_db()
    cur = conn.cursor()
    cur.execute("SELECT * FROM crawldb.site s WHERE s.id = " + str(id))

    return cur.fetchone()


def get_connections_between_sites():
    sites = get_sites()
    site_map = {}
    for site in sites:
        pages_for_site = np.array(get_pages_for_site(site[0]))
        linked_pages = np.array(get_linked_pages(pages_for_site[:,0]))
        linked_sites = get_sites_for_pages(linked_pages[:,0])
        site_map[site[0]] = linked_sites
    return site_map

