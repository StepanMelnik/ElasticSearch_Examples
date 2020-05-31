# ElasticSearch 7.6.2 version should be started before runnig unit test.
# sudo docker pull docker.elastic.co/elasticsearch/elasticsearch:7.6.2
# sudo docker run -p 9200:9200 -p 9300:9300 -e "discovery.type=single-node" docker.elastic.co/elasticsearch/elasticsearch:7.6.2
# wget http://localhost:9200/ or open http://localhost:9200/ in browser