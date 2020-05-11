package com.sme.elasticsearch.node;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.Test;

import com.sme.elasticsearch.model.Article;
import com.sme.elasticsearch.td.ArticleTD;

import util.ObjectMapperUtil;

/**
 *
 */
public class ArticleSearchNodeTest extends ANodeArticleTest
{
    @Test
    public void testSearchAll() throws Exception
    {
        SearchResponse response = client()
                .prepareSearch(ARTICLE_INDEX)
                .setQuery(QueryBuilders.matchAllQuery())
                .addSort(new FieldSortBuilder("id").order(SortOrder.ASC))
                .setExplain(true)
                .setSize(100)
                .get();

        assertEquals(RestStatus.OK, response.status());
        assertEquals(3L, response.getHits().getTotalHits().value);

        List<Article> articles = Arrays.stream(response.getHits().getHits())
                .map(searchHit -> ObjectMapperUtil.deserialize(Article.class, searchHit.getSourceAsString()))
                .collect(Collectors.toList());

        assertEquals(ArticleTD.ALL_ORDERED, articles);

    }

    @Test
    public void testSearchByTermQuery() throws Exception
    {
        SearchResponse response = client()
                .prepareSearch(ARTICLE_INDEX)
                .setQuery(QueryBuilders.termQuery("name", "name1"))
                .setExplain(true)
                .get();

        assertEquals(RestStatus.OK, response.status());
        assertEquals(1L, response.getHits().getTotalHits().value);

        assertEquals(ArticleTD.ARTICLE1, ObjectMapperUtil.deserialize(Article.class, response.getHits().getAt(0).getSourceAsString()));
    }

    @Test
    public void testByBoolQuery() throws Exception
    {
        QueryBuilder boolQuery = QueryBuilders.boolQuery()
                .must(QueryBuilders.rangeQuery("price").gt("1.00").lt("5.00"))
                .must(QueryBuilders.wildcardQuery("name", "name*"));

        SearchResponse response = client()
                .prepareSearch(ARTICLE_INDEX)
                .setQuery(boolQuery)
                .setExplain(true)
                .addSort(new FieldSortBuilder("id").order(SortOrder.ASC))
                .get();

        assertEquals(RestStatus.OK, response.status());
        assertEquals(3L, response.getHits().getTotalHits().value);
    }
}
