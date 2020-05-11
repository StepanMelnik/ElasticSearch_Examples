package com.sme.elasticsearch.node;

import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.rest.RestStatus;
import org.junit.Before;

import com.sme.elasticsearch.model.Article;
import com.sme.elasticsearch.td.ArticleTD;

import util.ObjectMapperUtil;

/**
 * Abstraction to work with {@link Article} bean based on Node implementation in ElasticSearch.
 */
public abstract class ANodeArticleTest extends AElasticSearchBaseTest
{
    protected static final String ARTICLE_INDEX = "articles";

    @Override
    @Before
    public void setUp() throws Exception
    {
        super.setUp();

        String article1 = ObjectMapperUtil.serialize(ArticleTD.ARTICLE1);
        String article2 = ObjectMapperUtil.serialize(ArticleTD.ARTICLE2);
        String article3 = ObjectMapperUtil.serialize(ArticleTD.ARTICLE3);

        BulkResponse response = client()
                .prepareBulk()
                .add(new IndexRequest().index(ARTICLE_INDEX).id("article1").source(article1, XContentType.JSON))
                .add(new IndexRequest().index(ARTICLE_INDEX).id("article2").source(article2, XContentType.JSON))
                .add(new IndexRequest().index(ARTICLE_INDEX).id("article3").source(article3, XContentType.JSON))
                .setRefreshPolicy(RefreshPolicy.IMMEDIATE)
                .execute()
                .actionGet();

        assertEquals(RestStatus.OK, response.status());
        for (BulkItemResponse r : response.getItems())
        {
            assertTrue(RestStatus.CREATED == r.status() || RestStatus.OK == r.status());
        }
    }
}
