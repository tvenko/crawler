# IEPS_assignment_1_web_crawler
TODO


# LINK DO NAVODIL:
http://zitnik.si/teaching/wier/PA1.html

# What to submit

Only one of the group members should make a submission of the assignment to moodle. The submission should contain only a link to the repository that contains the following:

a file si.fri;.si.fri.db.gz - Crawldb database dump pg_dump (dbname) | gzip > si.fri;.si.fri.db.gz (The dump must contain only data from the sites given as a seed URL list above!).
a file report.pdf - PDF report.
a file README.md - Short description of the project and instructions to install, set up and run the crawler.
a folder crawler - Implementation of the crawler.

# Build && install

\#1 Create empty database named: "crawldb" locally or use docker: 
```
docker run -d --name pg-database-crawler -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=admin -e POSTGRES_DB=crawldb -p 5432:5432 postgres:11.2
```

\#2 In newly created database run [script](http://zitnik.si/teaching/wier/data/pa1/crawldb.sql) - use IntelliJ SQL console, cp content of .sql and run query

```
\#3 mvn clean install
```

(\#4 For viewing db content, connect to database with IDE (in IntelliJ: View->Tools view->Database, add database; in tab schemas choose only schema crawldb))
