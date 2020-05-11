package com.sme.elasticsearch.restclient;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.Test;

import com.sme.elasticsearch.model.Article;
import com.sme.elasticsearch.td.ArticleTD;

import util.ObjectMapperUtil;

/**
 * Unit tests to search "articles" index by rest api client.
 */
public class ArticleSearchRestApiClientTest extends AArticleClientTest
{
    @Test
    public void testSearchAll() throws Exception
    {
        List<Article> articles = fetchAllArticles();
        assertEquals(ArticleTD.ALL_ORDERED, articles);
    }

    @Test
    public void testTerm() throws Exception
    {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder()
                .explain(true)
                .fetchSource(true)
                .query(QueryBuilders.termQuery("name", "name3"))
                .timeout(TimeValue.timeValueSeconds(5));

        // http://localhost:9200//_search?pre_filter_shard_size=128&typed_keys=true&max_concurrent_shard_requests=5&ignore_unavailable=false&expand_wildcards=open&allow_no_indices=true&ignore_throttled=true&search_type=query_then_fetch&batched_reduce_size=512&ccs_minimize_roundtrips=true
        // Body: {"from":1,"size":2,"timeout":"5s","query":{"term":{"name":{"value":"name*","boost":1.0}}},"explain":true,"_source":{"includes":[],"excludes":[]}}
        SearchResponse searchResponse = CLIENT.search(new SearchRequest()
                .indices(ARTICLE_INDEX)
                .source(sourceBuilder), RequestOptions.DEFAULT);

        List<Article> articles = Arrays.stream(searchResponse.getHits().getHits())
                .map(searchHit -> ObjectMapperUtil.deserialize(Article.class, searchHit.getSourceAsString()))
                .collect(Collectors.toList());
        assertEquals(Arrays.asList(ArticleTD.ARTICLE3), articles);
    }

    @Test
    public void testQuery() throws Exception
    {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder()
                .explain(true)
                .fetchSource(true)
                .query(QueryBuilders.queryStringQuery("name*"))
                .from(1)
                .size(2)
                .sort(new FieldSortBuilder("id").order(SortOrder.ASC))
                .timeout(TimeValue.timeValueSeconds(5));

        // http://localhost:9200/articles/_search?pre_filter_shard_size=128&typed_keys=true&max_concurrent_shard_requests=5&ignore_unavailable=false&expand_wildcards=open&allow_no_indices=true&ignore_throttled=true&search_type=query_then_fetch&batched_reduce_size=512&ccs_minimize_roundtrips=true
        // Body: {"timeout":"5s","query":{"query_string":{"query":"name*","fields":[],"type":"best_fields","default_operator":"or","max_determinized_states":10000,"enable_position_increments":true,"fuzziness":"AUTO","fuzzy_prefix_length":0,"fuzzy_max_expansions":50,"phrase_slop":0,"escape":false,"auto_generate_synonyms_phrase_query":true,"fuzzy_transpositions":true,"boost":1.0}},"explain":true,"_source":{"includes":[],"excludes":[]}}
        SearchResponse searchResponse = CLIENT.search(new SearchRequest()
                .indices(ARTICLE_INDEX)
                .source(sourceBuilder), RequestOptions.DEFAULT);

        List<Article> articles = Arrays.stream(searchResponse.getHits().getHits())
                .map(searchHit -> ObjectMapperUtil.deserialize(Article.class, searchHit.getSourceAsString()))
                .collect(Collectors.toList());
        assertEquals(Arrays.asList(ArticleTD.ARTICLE2, ArticleTD.ARTICLE3), articles);
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
