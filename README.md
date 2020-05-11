# ElasticSearch Examples 
A simple project to work with ElasticSearch by different test containers, ES Node and RestHighLevelClient implementation.

# Description
The project works with that latest ElasticSearch 7.6.2 Version.

DockerTestContainer and EmbeddedContainerTest containers disabled, because ElasticSearch test framework analyzes duplicated classes in ClassLoader and does not allow to start Node implementation.

Unit tests based on ElasticSearch Node implementation.
Another bunch of unit tests works with RestHighLevelClient client.

### Node unit tests

Node unit tests based on ElasticSearch test framework.
ESSingleNodeTestCase uses extra Lucene configuration and has a lot of problems with JarHell.

AElasticSearchBaseTest is based on Node implementation that wraps MockNode and runs unit tests in Rundomized container.

Unit tests work with CRUD and Search operations.

### RestHighLevelClient unit tests

The tests work with a real ElasticSearch 7.6.2 server started in Docker by Jenkins.

CRUD, Search, BeanRelation, Nested, etc operations tested by unit tests. 

### Build

> mvn clean install

