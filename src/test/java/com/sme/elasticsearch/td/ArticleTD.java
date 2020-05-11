package com.sme.elasticsearch.td;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import com.sme.elasticsearch.model.Article;

import util.PojoGenericBuilder;

/**
 * Article test data.
 */
public class ArticleTD
{
    public static Article ARTICLE1 = new PojoGenericBuilder<>(Article::new)
            .with(Article::setId, 1)
            .with(Article::setActive, true)
            .with(Article::setDescription, "description 1")
            .with(Article::setName, "name1")
            .with(Article::setPrice, BigDecimal.valueOf(1.01d))
            .with(Article::setActive, true)
            .build();

    public static Article ARTICLE2 = new PojoGenericBuilder<>(Article::new)
            .with(Article::setId, 2)
            .with(Article::setActive, true)
            .with(Article::setDescription, "description 2")
            .with(Article::setName, "name2")
            .with(Article::setPrice, BigDecimal.valueOf(2.02d))
            .with(Article::setActive, true)
            .build();

    public static Article ARTICLE3 = new PojoGenericBuilder<>(Article::new)
            .with(Article::setId, 3)
            .with(Article::setActive, true)
            .with(Article::setDescription, "description 3")
            .with(Article::setName, "name3")
            .with(Article::setPrice, BigDecimal.valueOf(3.03d))
            .with(Article::setActive, false)
            .build();

    public static List<Article> ALL_ORDERED = Arrays.asList(ARTICLE1, ARTICLE2, ARTICLE3);

    public static Article ARTICLE100 = new PojoGenericBuilder<>(Article::new)
            .with(Article::setId, 100)
            .with(Article::setActive, true)
            .with(Article::setDescription, "description 100")
            .with(Article::setName, "name3")
            .with(Article::setPrice, BigDecimal.valueOf(100.01d))
            .with(Article::setActive, true)
            .build();

    private ArticleTD()
    {
    }
}
