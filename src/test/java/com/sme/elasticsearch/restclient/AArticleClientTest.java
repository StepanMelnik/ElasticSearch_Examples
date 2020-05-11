package com.sme.elasticsearch.restclient;

import java.util.function.Supplier;

import org.apache.http.HttpHost;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.rest.RestStatus;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;

import com.sme.elasticsearch.td.ArticleTD;

import util.ObjectMapperUtil;

/**
 * Abstraction to work with "articles" index by high level rest client.
 */
public abstract class AArticleClientTest extends Assert
{
    protected static final String ARTICLE_INDEX = "articles";
    protected static final RestHighLevelClient CLIENT = new RestHighLevelClient(RestClient.builder(new HttpHost("localhost", 9200, "http")));
    protected static final Logger LOGGER = LogManager.getLogger(AArticleClientTest.class);

    @Before
    public void setUp() throws Exception
    {
        // http://localhost:9200/_mapping should be empty

        String article1 = ObjectMapperUtil.serialize(ArticleTD.ARTICLE1);
        String article2 = ObjectMapperUtil.serialize(ArticleTD.ARTICLE2);
        String article3 = ObjectMapperUtil.serialize(ArticleTD.ARTICLE3);

        final BulkRequest bulkRequest = new BulkRequest()
                .add(new IndexRequest().index(ARTICLE_INDEX).id("article1").source(article1, XContentType.JSON))
                .add(new IndexRequest().index(ARTICLE_INDEX).id("article2").source(article2, XContentType.JSON))
                .add(new IndexRequest().index(ARTICLE_INDEX).id("article3").source(article3, XContentType.JSON))
                .setRefreshPolicy(RefreshPolicy.IMMEDIATE);

        logAction("bulkRequest", () -> bulkRequest.requests().toString());

        // http://localhost:9200/articles/_mapping?pretty
        // http://localhost:9200/articles/_search?pretty
        BulkResponse response = CLIENT.bulk(bulkRequest, RequestOptions.DEFAULT);

        assertEquals(RestStatus.OK, response.status());
        for (BulkItemResponse r : response.getItems())
        {
            assertTrue(RestStatus.CREATED == r.status() || RestStatus.OK == r.status());
        }

        ClusterHealthRequest clusterHealthRequest = new ClusterHealthRequest(ARTICLE_INDEX);
        logAction("clusterHealthRequest", () -> clusterHealthRequest.toString());

        ClusterHealthResponse clusterHealthResponse = CLIENT.cluster().health(clusterHealthRequest, RequestOptions.DEFAULT);
        assertEquals(ClusterHealthStatus.YELLOW, clusterHealthResponse.getStatus());
    }

    @After
    public void tearDown() throws Exception
    {
        DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest(ARTICLE_INDEX);
        logAction("deleteIndexRequest", () -> deleteIndexRequest.toString());

        CLIENT.indices().delete(deleteIndexRequest, RequestOptions.DEFAULT);
    }

    @AfterClass
    public static void afterClass() throws Exception
    {
        CLIENT.close();
    }

    /**
     * Print info about a request.
     */
    protected static void logAction(String action, Supplier<String> message)
    {
        LOGGER.info("Perform {} action with {} json request", action, message.get());
    }
}
