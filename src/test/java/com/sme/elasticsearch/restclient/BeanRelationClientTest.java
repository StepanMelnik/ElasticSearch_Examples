package com.sme.elasticsearch.restclient;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpHost;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.GetMappingsRequest;
import org.elasticsearch.client.indices.GetMappingsResponse;
import org.elasticsearch.client.indices.PutMappingRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.sme.elasticsearch.model.Order;
import com.sme.elasticsearch.td.OrderTD;

import util.ObjectMapperUtil;

/**
 * Unit tests to work with relation of beans.
 */
public class BeanRelationClientTest extends Assert
{
    protected static final String ORDER_INDEX = "orders";
    protected static final RestHighLevelClient CLIENT = new RestHighLevelClient(RestClient.builder(new HttpHost("localhost", 9200, "http")));
    protected static final Logger LOGGER = LogManager.getLogger(BeanRelationClientTest.class);

    @Before
    public void setUp() throws Exception
    {
        // http://localhost:9200/products/_mapping?pretty
        if (!CLIENT.indices().exists(new GetIndexRequest(ORDER_INDEX), RequestOptions.DEFAULT))
        {
            CreateIndexResponse createIndexResponse = CLIENT.indices().create(new CreateIndexRequest(ORDER_INDEX), RequestOptions.DEFAULT);
            assertTrue("Expects Acknowledged status", createIndexResponse.isAcknowledged());
        }

        // https://www.elastic.co/guide/en/elasticsearch/reference/current/mapping-params.html
        String fileName = ProductClientTest.class.getClassLoader().getResource("mappings/Order.json").getFile();
        String mapping = FileUtils.readFileToString(new File(fileName), "UTF-8");

        PutMappingRequest putMappingRequest = new PutMappingRequest(ORDER_INDEX)
                .source(mapping, XContentType.JSON);

        // PUT /products/_mapping?master_timeout=30s&timeout=30s
        // Body: mapping
        CLIENT.indices().putMapping(putMappingRequest, RequestOptions.DEFAULT);

        GetMappingsRequest getMappingsRequest = new GetMappingsRequest().indices(ORDER_INDEX);
        GetMappingsResponse getMappingsResponse = CLIENT.indices().getMapping(getMappingsRequest, RequestOptions.DEFAULT);

        assertTrue("Expects creeated mapping", getMappingsResponse.mappings().size() >= 1);

        // Create data
        String order1 = ObjectMapperUtil.serialize(OrderTD.ORDER1);
        String order2 = ObjectMapperUtil.serialize(OrderTD.ORDER2);
        String order3 = ObjectMapperUtil.serialize(OrderTD.ORDER3);

        final BulkRequest bulkRequest = new BulkRequest()
                .add(new IndexRequest().index(ORDER_INDEX).id("order_1").source(order1, XContentType.JSON))
                .add(new IndexRequest().index(ORDER_INDEX).id("order_2").source(order2, XContentType.JSON))
                .add(new IndexRequest().index(ORDER_INDEX).id("order_3").source(order3, XContentType.JSON))
                .setRefreshPolicy(RefreshPolicy.IMMEDIATE);

        BulkResponse response = CLIENT.bulk(bulkRequest, RequestOptions.DEFAULT);

        assertEquals(RestStatus.OK, response.status());
        for (BulkItemResponse r : response.getItems())
        {
            assertTrue(RestStatus.CREATED == r.status() || RestStatus.OK == r.status());
        }
    }

    @After
    public void tearDown() throws Exception
    {
        AcknowledgedResponse response = CLIENT.indices().delete(new DeleteIndexRequest(ORDER_INDEX), RequestOptions.DEFAULT);
        assertTrue("Expects Acknowledged status", response.isAcknowledged());
    }

    @AfterClass
    public static void afterClass() throws Exception
    {
        CLIENT.close();
    }

    @Test
    public void testMatchAll() throws Exception
    {
        final SearchRequest searchRequest = new SearchRequest()
                .indices(ORDER_INDEX)
                .requestCache(false)
                .source(new SearchSourceBuilder()
                        .query(QueryBuilders.matchAllQuery())
                        .sort(new FieldSortBuilder("id").order(SortOrder.ASC)));

        // http://localhost:9200/orders/_search?pre_filter_shard_size=128&typed_keys=true&max_concurrent_shard_requests=5&ignore_unavailable=false&expand_wildcards=open&allow_no_indices=true&ignore_throttled=true&request_cache=false&search_type=query_then_fetch&batched_reduce_size=512&ccs_minimize_roundtrips=true
        // POST: {"query":{"match_all":{"boost":1.0}},"sort":[{"id":{"order":"asc"}}]}
        SearchResponse searchResponse = CLIENT.search(searchRequest, RequestOptions.DEFAULT);

        assertEquals(RestStatus.OK, searchResponse.status());

        List<Order> orders = Arrays.stream(searchResponse.getHits().getHits())
                .map(searchHit -> ObjectMapperUtil.deserialize(Order.class, searchHit.getSourceAsString()))
                .collect(Collectors.toList());

        assertEquals(OrderTD.ALL_ORDERED, orders);
    }

    @Test
    public void testFilteredMatchAll() throws Exception
    {
        // Fetch all orders with Article#id = 1
        final SearchRequest searchRequest = new SearchRequest()
                .indices(ORDER_INDEX)
                .requestCache(false)
                .source(new SearchSourceBuilder()
                        .query(QueryBuilders.matchAllQuery())
                        .sort(new FieldSortBuilder("id").order(SortOrder.ASC))
                        .postFilter(QueryBuilders.termQuery("orderItems.article.id", 1)));

        // http://localhost:9200/orders/_search?pre_filter_shard_size=128&typed_keys=true&max_concurrent_shard_requests=5&ignore_unavailable=false&expand_wildcards=open&allow_no_indices=true&ignore_throttled=true&request_cache=false&search_type=query_then_fetch&batched_reduce_size=512&ccs_minimize_roundtrips=true
        // POST: {"query":{"match_all":{"boost":1.0}},"post_filter":{"term":{"orderItems.article.id":{"value":1,"boost":1.0}}},"sort":[{"id":{"order":"asc"}}]}
        SearchResponse searchResponse = CLIENT.search(searchRequest, RequestOptions.DEFAULT);

        assertEquals(RestStatus.OK, searchResponse.status());

        List<Order> orders = Arrays.stream(searchResponse.getHits().getHits())
                .map(searchHit -> ObjectMapperUtil.deserialize(Order.class, searchHit.getSourceAsString()))
                .collect(Collectors.toList());

        assertEquals(Arrays.asList(OrderTD.ORDER1, OrderTD.ORDER3), orders);
    }

    @Test
    public void testNestedTermQuery() throws Exception
    {
        // Fetch all orders with Article#id = 1
        final SearchRequest searchRequest = new SearchRequest()
                .indices(ORDER_INDEX)
                .requestCache(false)
                .source(new SearchSourceBuilder()
                        .query(QueryBuilders.termQuery("orderItems.article.id", 1)));

        // http://localhost:9200/orders/_search?pre_filter_shard_size=128&typed_keys=true&max_concurrent_shard_requests=5&ignore_unavailable=false&expand_wildcards=open&allow_no_indices=true&ignore_throttled=true&request_cache=false&search_type=query_then_fetch&batched_reduce_size=512&ccs_minimize_roundtrips=true
        // POST: {"query":{"term":{"orderItems.article.id":{"value":1,"boost":1.0}}}}
        SearchResponse searchResponse = CLIENT.search(searchRequest, RequestOptions.DEFAULT);

        assertEquals(RestStatus.OK, searchResponse.status());

        List<Order> orders = Arrays.stream(searchResponse.getHits().getHits())
                .map(searchHit -> ObjectMapperUtil.deserialize(Order.class, searchHit.getSourceAsString()))
                .collect(Collectors.toList());

        assertEquals(Arrays.asList(OrderTD.ORDER1, OrderTD.ORDER3), orders);
    }
}
