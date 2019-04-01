# IEPS assignment 1: web crawler
- Project for WIER, masters course at FRI Faculty for computer and information science, University of Ljubljana in 2019.
- Authors: Julija Petrič (@JulijaPet), Ladislav Škufca (@ladislavskufca) and Tilen Venko (@tvenko).
- rules: http://zitnik.si/teaching/wier/PA1.html

# Basic description
Crawler is used to crawl .gov.si sites (bfs algorithm) and in written in java. All the libraries that were used are listed in main pom.xml.

# Build && install

\#1 Create empty database named: "crawldb" locally or use docker: 
```
docker run -d --name pg-database-crawler -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=admin -e POSTGRES_DB=crawldb -p 5432:5432 postgres:11.2
```

\#2 In newly created database run [script](https://github.com/ladislavskufca/IEPS_assignment_1_web_crawler/blob/master/crawldb.sql) - use IntelliJ SQL console, cp content of .sql and run query

\#3: 
```
mvn clean install
```

(\#4 For viewing db content, connect to database with IDE (in IntelliJ: View->Tools view->Database, add database; in tab schemas choose only schema crawldb))
