package com.sme.elasticsearch.node;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetRequest;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Requests;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.Priority;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.rest.RestStatus;
import org.junit.Test;

import com.sme.elasticsearch.model.Article;
import com.sme.elasticsearch.td.ArticleTD;

import util.ObjectMapperUtil;
import util.PojoGenericBuilder;

/**
 * Unit tests to perform ElasticSearch operations with {@link Article} bean.
 */
public class ArticleCRUDNodeTest extends ANodeArticleTest
{
    @Test
    public void testGetArticles()
    {
        GetResponse response = client()
                .prepareGet()
                .setIndex(ARTICLE_INDEX)
                .setId("article")   // wrong id
                .execute()
                .actionGet();

        assertEquals(ARTICLE_INDEX, response.getIndex());
        assertEquals("article", response.getId());
        assertFalse("Does not expect response by 'article' id", response.isExists());
        assertNull("Expects null source", response.getSourceAsMap());

        response = client()
                .prepareGet()
                .setIndex(ARTICLE_INDEX)
                .setId("article1")
                .execute()
                .actionGet();

        assertEquals(ARTICLE_INDEX, response.getIndex());
        assertEquals("article1", response.getId());
        assertTrue("Expects response by 'article1' id", response.isExists());
        assertNotNull("Expects null source", response.getSourceAsMap());
        assertEquals(ArticleTD.ARTICLE1, ObjectMapperUtil.deserialize(Article.class, response.getSourceAsString()));

        response = client()
                .prepareGet()
                .setIndex(ARTICLE_INDEX)
                .setId("article2")
                .execute()
                .actionGet();
        assertEquals(ArticleTD.ARTICLE2, ObjectMapperUtil.deserialize(Article.class, response.getSourceAsString()));

        ClusterHealthResponse cluserHealthAction = client()
                .admin()
                .cluster()
                .health(Requests.clusterHealthRequest(ARTICLE_INDEX)
                        .timeout(TimeValue.timeValueSeconds(30))
                        .waitForGreenStatus()
                        .waitForEvents(Priority.LANGUID)
                        .waitForNoRelocatingShards(true))
                .actionGet();

        Optional.of(cluserHealthAction.isTimedOut()).map(s -> s).ifPresent(c -> new RuntimeException("Timeout occurred while checking cluster state"));
        ClusterHealthStatus status = cluserHealthAction.getStatus();
        assertEquals(ClusterHealthStatus.GREEN, status);
    }

    @Test
    public void testMultiGetArticles()
    {
        MultiGetRequest request = new MultiGetRequest()
                .add(ARTICLE_INDEX, "article1")
                .add(ARTICLE_INDEX, "article3");

        MultiGetResponse response = client().multiGet(request).actionGet();

        List<Article> articles = Arrays.stream(response.getResponses())
                .map(m -> ObjectMapperUtil.deserialize(Article.class, m.getResponse().getSourceAsString()))
                .collect(Collectors.toList());
        assertEquals(Arrays.asList(ArticleTD.ARTICLE1, ArticleTD.ARTICLE3), articles);
    }

    @Test
    public void testUpdateArticle() throws Exception
    {
        GetResponse getResponse = client()
                .prepareGet()
                .setIndex(ARTICLE_INDEX)
                .setId("article1")
                .execute()
                .actionGet();
        assertTrue("Expects updated versions >= 1", getResponse.getVersion() >= 1L);

        UpdateResponse updateResponse = client()
                .prepareUpdate()
                .setIndex(ARTICLE_INDEX)
                .setId("article1")
                .setDoc("{\"name\" : \"name11\"}", XContentType.JSON)
                .setFetchSource(true)   // fetch source for testing purpose
                .setRefreshPolicy(RefreshPolicy.IMMEDIATE)
                .execute()
                .actionGet();

        assertEquals(RestStatus.OK, updateResponse.status());
        assertEquals(ARTICLE_INDEX, updateResponse.getIndex());
        assertEquals("article1", updateResponse.getId());

        Article article = ObjectMapperUtil.deserialize(Article.class, updateResponse.getGetResult().sourceAsString());
        Article expected = new PojoGenericBuilder<>(Article::new)
                .with(Article::setId, 1)
                .with(Article::setActive, true)
                .with(Article::setDescription, "description 1")
                .with(Article::setName, "name11")
                .with(Article::setPrice, BigDecimal.valueOf(1.01d))
                .with(Article::setActive, true)
                .build();

        assertEquals(expected, article);

        Map<String, Object> articleToMap = ObjectMapperUtil.convert(ArticleTD.ARTICLE3, Map.class);
        updateResponse = client()
                .prepareUpdate()
                .setIndex(ARTICLE_INDEX)
                .setId("article1")
                .setDoc(articleToMap)
                .setFetchSource(true) // fetch source for testing purpose
                .setRefreshPolicy(RefreshPolicy.IMMEDIATE)
                .execute()
                .actionGet();

        assertEquals(ArticleTD.ARTICLE3, ObjectMapperUtil.deserialize(Article.class, updateResponse.getGetResult().sourceAsString()));
    }
}
