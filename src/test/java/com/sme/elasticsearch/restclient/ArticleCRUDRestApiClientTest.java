package com.sme.elasticsearch.restclient;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.MultiGetRequest;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.Test;

import com.sme.elasticsearch.model.Article;
import com.sme.elasticsearch.td.ArticleTD;

import util.ObjectMapperUtil;
import util.PojoGenericBuilder;

/**
 * Unit tests to work with "articles" index by rest api client.
 */
public class ArticleCRUDRestApiClientTest extends AArticleClientTest
{
    @Test
    public void testIndex() throws Exception
    {
        String article100 = ObjectMapperUtil.serialize(ArticleTD.ARTICLE100);

        final IndexRequest indexRequest = new IndexRequest(ARTICLE_INDEX).index("article100").source(article100, XContentType.JSON);
        logAction("indexRequest", () -> indexRequest.source().utf8ToString());

        // http://localhost:9200/articles/_search?pretty
        IndexResponse indexResponse = CLIENT.index(indexRequest, RequestOptions.DEFAULT);

        assertEquals(RestStatus.CREATED, indexResponse.status());
    }

    @Test
    public void testDelete() throws Exception
    {
        List<Article> articles = fetchAllArticles();
        assertEquals(ArticleTD.ALL_ORDERED, articles);

        // Remove first article
        final DeleteRequest deleteRequest = new DeleteRequest(ARTICLE_INDEX, "article1")
                .setRefreshPolicy(RefreshPolicy.IMMEDIATE);
        logAction("deleteRequest", () -> deleteRequest.toString());

        // DELETE http://localhost:9200/articles/_doc/article1?refresh=true&timeout=1m
        DeleteResponse deleteResponse = CLIENT.delete(deleteRequest, RequestOptions.DEFAULT);

        assertEquals(RestStatus.OK, deleteResponse.status());

        List<Article> articles0 = fetchAllArticles();
        assertEquals(Arrays.asList(ArticleTD.ARTICLE2, ArticleTD.ARTICLE3), articles0);

        final DeleteByQueryRequest deleteByQueryRequest = new DeleteByQueryRequest(ARTICLE_INDEX)
                .setQuery(new TermQueryBuilder("name", "name3"))
                .setRefresh(true);
        logAction("deleteByQueryRequest", () -> deleteByQueryRequest.toString());

        // POST http://localhost:9200/articles/_delete_by_query?slices=1&requests_per_second=-1&ignore_unavailable=false&expand_wildcards=open&allow_no_indices=true&ignore_throttled=true&wait_for_completion=true&timeout=1m
        // Body: {"size":1000,"query":{"term":{"name":{"value":"name3","boost":1.0}}},"_source":false}
        BulkByScrollResponse bulkByScrollResponse = CLIENT.deleteByQuery(deleteByQueryRequest, RequestOptions.DEFAULT);
        assertEquals(1, bulkByScrollResponse.getStatus().getDeleted());

        articles = fetchAllArticles();
        assertEquals(Arrays.asList(ArticleTD.ARTICLE2), articles);
    }

    @Test
    public void testExists() throws Exception
    {
        final GetRequest request = new GetRequest(ARTICLE_INDEX, "article1")
                .fetchSourceContext(new FetchSourceContext(false))
                .storedFields("_none_");

        // HEAD http://localhost:9200/articles/_doc/article1?stored_fields=_none_&_source=false
        assertTrue("Expects article with 'article1' id", CLIENT.exists(request, RequestOptions.DEFAULT));
    }

    @Test
    public void testMultiGet() throws Exception
    {
        final MultiGetRequest request = new MultiGetRequest();
        request.add(new MultiGetRequest.Item(ARTICLE_INDEX, "article1"));
        request.add(new MultiGetRequest.Item(ARTICLE_INDEX, "article2"));

        // POST http://localhost:9200/_mget
        // Body: {"docs":[{"_index":"articles","_type":null,"_id":"article1","routing":null,"stored_fields":null,"version":-3,"version_type":"internal","_source":null},{"_index":"articles","_type":null,"_id":"article2","routing":null,"stored_fields":null,"version":-3,"version_type":"internal","_source":null}]}
        MultiGetResponse multiGetResponse = CLIENT.mget(request, RequestOptions.DEFAULT);
        List<Article> articles = Arrays.stream(multiGetResponse.getResponses())
                .map(article -> ObjectMapperUtil.deserialize(Article.class, article.getResponse().getSourceAsString()))
                .collect(Collectors.toList());

        assertEquals(Arrays.asList(ArticleTD.ARTICLE1, ArticleTD.ARTICLE2), articles);
    }

    @Test
    public void testUpdate() throws Exception
    {

        final UpdateRequest request = new UpdateRequest(ARTICLE_INDEX, "article1")
                .doc("{\"name\" : \"name11\"}", XContentType.JSON)
                .fetchSource(true)
                .setRefreshPolicy(RefreshPolicy.IMMEDIATE);

        // POST /articles/_update/article1?refresh=true&timeout=1m
        // Body: {"doc":{"name":"name11"},"_source":{"includes":[],"excludes":[]}}
        UpdateResponse updateResponse = CLIENT.update(request, RequestOptions.DEFAULT);

        Article aticle = new PojoGenericBuilder<>(Article::new)
                .with(Article::setId, 1)
                .with(Article::setActive, true)
                .with(Article::setDescription, "description 1")
                .with(Article::setName, "name11")
                .with(Article::setPrice, BigDecimal.valueOf(1.01d))
                .with(Article::setActive, true)
                .build();
        assertEquals(aticle, ObjectMapperUtil.deserialize(Article.class, updateResponse.getGetResult().sourceAsString()));
    }

    private List<Article> fetchAllArticles() throws IOException
    {
        final SearchRequest searchRequest = new SearchRequest()
                .indices(ARTICLE_INDEX)
                .requestCache(false)
                .source(new SearchSourceBuilder()
                        .query(QueryBuilders.matchAllQuery())
                        .sort(new FieldSortBuilder("id").order(SortOrder.ASC)));

        logAction("searchRequest: all articles", () -> searchRequest.source().toString());

        // POST http://localhost:9200/articles/_search?pre_filter_shard_size=128&typed_keys=true&max_concurrent_shard_requests=5&ignore_unavailable=false&expand_wildcards=open&allow_no_indices=true&ignore_throttled=true&request_cache=false&search_type=query_then_fetch&batched_reduce_size=512&ccs_minimize_roundtrips=true"
        // Body: {"query":{"match_all":{"boost":1.0}},"sort":[{"id":{"order":"asc"}}]}
        SearchResponse searchResponse = CLIENT.search(searchRequest, RequestOptions.DEFAULT);

        assertEquals(RestStatus.OK, searchResponse.status());

        return Arrays.stream(searchResponse.getHits().getHits())
                .map(searchHit -> ObjectMapperUtil.deserialize(Article.class, searchHit.getSourceAsString()))
                .collect(Collectors.toList());
    }
}
