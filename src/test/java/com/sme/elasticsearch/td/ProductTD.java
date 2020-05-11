package com.sme.elasticsearch.td;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import com.sme.elasticsearch.model.Product;

import util.PojoGenericBuilder;

/**
 * Product test data.
 */
public class ProductTD
{
    public static Product PRODUCT1 = new PojoGenericBuilder<>(Product::new)
            .with(Product::setId, 1)
            .with(Product::setActive, true)
            .with(Product::setCreatedDate, Date.valueOf(LocalDate.now().minusDays(1)))
            .with(Product::setDescription, "Dell computure (32GB), Black)")
            .with(Product::setImage, "http://localhost:8080/resources/image1.gif")
            .with(Product::setName, "Dell 1111 computure")
            .with(Product::setPrice, new BigDecimal(10.01d).setScale(2, RoundingMode.HALF_UP))
            .with(Product::setProductType, "Type1")
            .with(Product::setTotal, new BigDecimal(12.01d).setScale(2, RoundingMode.HALF_UP))
            .build();

    public static Product PRODUCT2 = new PojoGenericBuilder<>(Product::new)
            .with(Product::setId, 2)
            .with(Product::setActive, true)
            .with(Product::setCreatedDate, Date.valueOf(LocalDate.now().minusDays(2)))
            .with(Product::setDescription, "Dell motherboard, 16Gb)")
            .with(Product::setImage, "http://localhost:8080/resources/image2.gif")
            .with(Product::setName, "Dell 2222 motherboard")
            .with(Product::setPrice, new BigDecimal(11.01d).setScale(2, RoundingMode.HALF_UP))
            .with(Product::setProductType, "Type2")
            .with(Product::setTotal, new BigDecimal(13.01d).setScale(2, RoundingMode.HALF_UP))
            .build();

    public static Product PRODUCT3 = new PojoGenericBuilder<>(Product::new)
            .with(Product::setId, 3)
            .with(Product::setActive, true)
            .with(Product::setCreatedDate, Date.valueOf(LocalDate.now().minusDays(3)))
            .with(Product::setDescription, "Dell keyboard, Black")
            .with(Product::setImage, "http://localhost:8080/resources/image3.gif")
            .with(Product::setName, "Dell 2222 keyboard")
            .with(Product::setPrice, new BigDecimal(12.01d).setScale(2, RoundingMode.HALF_UP))
            .with(Product::setProductType, "Type3")
            .with(Product::setTotal, new BigDecimal(14.01d).setScale(2, RoundingMode.HALF_UP))
            .build();

    public static Product PRODUCT4 = new PojoGenericBuilder<>(Product::new)
            .with(Product::setId, 4)
            .with(Product::setActive, false)
            .with(Product::setCreatedDate, Date.valueOf(LocalDate.now().minusDays(3)))
            .with(Product::setDescription, "Dell bag, Red")
            .with(Product::setImage, "http://localhost:8080/resources/image4.gif")
            .with(Product::setName, "Dell bag")
            .with(Product::setPrice, new BigDecimal(12.01d).setScale(2, RoundingMode.HALF_UP))
            .with(Product::setProductType, "Type4")
            .with(Product::setTotal, new BigDecimal(14.01d).setScale(2, RoundingMode.HALF_UP))
            .build();

    public static List<Product> ALL_ORDERED = Arrays.asList(PRODUCT1, PRODUCT2, PRODUCT3);

    private ProductTD()
    {
    }
}
