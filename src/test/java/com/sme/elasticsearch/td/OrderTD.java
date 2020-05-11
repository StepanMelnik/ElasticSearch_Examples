package com.sme.elasticsearch.td;

import java.util.Arrays;
import java.util.List;

import com.sme.elasticsearch.model.Order;
import com.sme.elasticsearch.model.OrderItem;

import util.PojoGenericBuilder;

/**
 * Order test data.
 */
public class OrderTD
{
    public static Order ORDER1 = new PojoGenericBuilder<>(Order::new)
            .with(Order::setId, 1)
            .with(Order::setOrderNo, "order1")
            .with(Order::setOrderItems, Arrays.asList(
                    new PojoGenericBuilder<>(OrderItem::new)
                            .with(OrderItem::setQuantity, 1)
                            .with(OrderItem::setArticle, ArticleTD.ARTICLE1)
                            .build(),
                    new PojoGenericBuilder<>(OrderItem::new)
                            .with(OrderItem::setQuantity, 1)
                            .with(OrderItem::setArticle, ArticleTD.ARTICLE2)
                            .build()))
            .build();

    public static Order ORDER2 = new PojoGenericBuilder<>(Order::new)
            .with(Order::setId, 2)
            .with(Order::setOrderNo, "order2")
            .with(Order::setOrderItems, Arrays.asList(
                    new PojoGenericBuilder<>(OrderItem::new)
                            .with(OrderItem::setQuantity, 2)
                            .with(OrderItem::setArticle, ArticleTD.ARTICLE3)
                            .build()))
            .build();

    public static Order ORDER3 = new PojoGenericBuilder<>(Order::new)
            .with(Order::setId, 3)
            .with(Order::setOrderNo, "order3")
            .with(Order::setOrderItems, Arrays.asList(
                    new PojoGenericBuilder<>(OrderItem::new)
                            .with(OrderItem::setQuantity, 5)
                            .with(OrderItem::setArticle, ArticleTD.ARTICLE1)
                            .build()))
            .build();

    public static List<Order> ALL_ORDERED = Arrays.asList(ORDER1, ORDER2, ORDER3);

    private OrderTD()
    {
    }
}
