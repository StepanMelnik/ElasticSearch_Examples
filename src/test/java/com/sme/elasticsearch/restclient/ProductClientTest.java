package com.sme.elasticsearch.restclient;

import java.io.File;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpHost;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;
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
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.sme.elasticsearch.model.Product;
import com.sme.elasticsearch.td.ProductTD;

import util.ObjectMapperUtil;

/**
 * <p>
 * Unit tests to work with {@link Product} bean.
 * </p>
 * The test includes mapping creating specified in /mappings/Product.json.
 */
public class ProductClientTest extends Assert
{
    protected static final String PRODUCT_INDEX = "products";
    protected static final RestHighLevelClient CLIENT = new RestHighLevelClient(RestClient.builder(new HttpHost("localhost", 9200, "http")));
    protected static final Logger LOGGER = LogManager.getLogger(ProductClientTest.class);

    @Before
    public void setUp() throws Exception
    {
        // http://localhost:9200/products/_mapping?pretty
        if (!CLIENT.indices().exists(new GetIndexRequest(PRODUCT_INDEX), RequestOptions.DEFAULT))
        {
            CreateIndexResponse createIndexResponse = CLIENT.indices().create(new CreateIndexRequest(PRODUCT_INDEX), RequestOptions.DEFAULT);
            assertTrue("Expects Acknowledged status", createIndexResponse.isAcknowledged());
        }

        // https://www.elastic.co/guide/en/elasticsearch/reference/current/mapping-params.html
        String fileName = ProductClientTest.class.getClassLoader().getResource("mappings/Product.json").getFile();
        String mapping = FileUtils.readFileToString(new File(fileName), "UTF-8");

        PutMappingRequest putMappingRequest = new PutMappingRequest(PRODUCT_INDEX)
                .source(mapping, XContentType.JSON);

        // PUT /products/_mapping?master_timeout=30s&timeout=30s
        // Body: mapping
        CLIENT.indices().putMapping(putMappingRequest, RequestOptions.DEFAULT);

        GetMappingsRequest getMappingsRequest = new GetMappingsRequest().indices(PRODUCT_INDEX);
        GetMappingsResponse getMappingsResponse = CLIENT.indices().getMapping(getMappingsRequest, RequestOptions.DEFAULT);

        assertTrue("Expects creeated mapping", getMappingsResponse.mappings().size() >= 1);

        // Create data
        String product1 = ObjectMapperUtil.serialize(ProductTD.PRODUCT1);
        String product2 = ObjectMapperUtil.serialize(ProductTD.PRODUCT2);
        String product3 = ObjectMapperUtil.serialize(ProductTD.PRODUCT3);

        final BulkRequest bulkRequest = new BulkRequest()
                .add(new IndexRequest().index(PRODUCT_INDEX).id("product1").source(product1, XContentType.JSON))
                .add(new IndexRequest().index(PRODUCT_INDEX).id("product2").source(product2, XContentType.JSON))
                .add(new IndexRequest().index(PRODUCT_INDEX).id("product3").source(product3, XContentType.JSON))
                .setRefreshPolicy(RefreshPolicy.IMMEDIATE);

        BulkResponse response = CLIENT.bulk(bulkRequest, RequestOptions.DEFAULT);

        assertEquals(RestStatus.OK, response.status());
        for (BulkItemResponse r : response.getItems())
        {
            assertTrue(RestStatus.CREATED == r.status() || RestStatus.OK == r.status());
        }
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
                .indices(PRODUCT_INDEX)
                .requestCache(false)
                .source(new SearchSourceBuilder()
                        .query(QueryBuilders.matchAllQuery())
                        .sort(new FieldSortBuilder("id").order(SortOrder.ASC)));

        SearchResponse searchResponse = CLIENT.search(searchRequest, RequestOptions.DEFAULT);

        assertEquals(RestStatus.OK, searchResponse.status());

        List<Product> products = Arrays.stream(searchResponse.getHits().getHits())
                .map(searchHit -> ObjectMapperUtil.deserialize(Product.class, searchHit.getSourceAsString()))
                .collect(Collectors.toList());

        assertEquals(ProductTD.ALL_ORDERED, products);
    }

    @Test
    public void testRange() throws Exception
    {
        final SearchRequest searchRequest = new SearchRequest()
                .indices(PRODUCT_INDEX)
                .requestCache(false)
                .source(new SearchSourceBuilder()
                        .query(new RangeQueryBuilder("createdDate")
                                .gt(Timestamp.valueOf(LocalDateTime.now().minusDays(2)).getTime())
                                .lt(Timestamp.valueOf(LocalDateTime.now()).getTime()))
                        .sort(new FieldSortBuilder("id").order(SortOrder.ASC)));

        SearchResponse searchResponse = CLIENT.search(searchRequest, RequestOptions.DEFAULT);

        assertEquals(RestStatus.OK, searchResponse.status());

        List<Product> products = Arrays.stream(searchResponse.getHits().getHits())
                .map(searchHit -> ObjectMapperUtil.deserialize(Product.class, searchHit.getSourceAsString()))
                .collect(Collectors.toList());

        assertEquals(Arrays.asList(ProductTD.PRODUCT1), products);
    }

    @Test
    public void testMultiMatch() throws Exception
    {
        final SearchRequest searchRequest = new SearchRequest()
                .indices(PRODUCT_INDEX)
                .requestCache(false)
                .source(new SearchSourceBuilder()
                        .query(new MultiMatchQueryBuilder("computure")
                                .field("name")
                                .field("description"))
                        .sort(new FieldSortBuilder("id").order(SortOrder.ASC)));

        SearchResponse searchResponse = CLIENT.search(searchRequest, RequestOptions.DEFAULT);

        assertEquals(RestStatus.OK, searchResponse.status());

        List<Product> products = Arrays.stream(searchResponse.getHits().getHits())
                .map(searchHit -> ObjectMapperUtil.deserialize(Product.class, searchHit.getSourceAsString()))
                .collect(Collectors.toList());

        assertEquals(Arrays.asList(ProductTD.PRODUCT1), products);
    }

    @Test
    public void testBoolMatch() throws Exception
    {
        final SearchRequest searchRequest = new SearchRequest()
                .indices(PRODUCT_INDEX)
                .requestCache(false)
                .source(new SearchSourceBuilder()
                        .query(new BoolQueryBuilder()
                                .must(QueryBuilders.rangeQuery("price").gt("10.00").lt("15.00"))
                                .must(QueryBuilders.termsQuery("name", "computure", "motherboard"))
                                .should(QueryBuilders.wildcardQuery("name", "Samsung")))
                        .sort(new FieldSortBuilder("id").order(SortOrder.ASC)));

        SearchResponse searchResponse = CLIENT.search(searchRequest, RequestOptions.DEFAULT);

        assertEquals(RestStatus.OK, searchResponse.status());

        List<Product> products = Arrays.stream(searchResponse.getHits().getHits())
                .map(searchHit -> ObjectMapperUtil.deserialize(Product.class, searchHit.getSourceAsString()))
                .collect(Collectors.toList());

        assertEquals(Arrays.asList(ProductTD.PRODUCT1, ProductTD.PRODUCT2), products);
    }
}
