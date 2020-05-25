package util;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Generic builder of pojo.
 * 
 * @param <T> is a POJO type.
 */
public class PojoGenericBuilder<T>
{
    private final T pojo;

    public PojoGenericBuilder(Supplier<T> supplier)
    {
        this.pojo = supplier.get();
    }

    /**
     * Create builder from the given supplier.
     * 
     * @param <T> is a POJO type;
     * @param supplier The supplier of POJO;
     * @return Returns POJO builder.
     */
    public static <T> PojoGenericBuilder<T> of(Supplier<T> supplier)
    {
        return new PojoGenericBuilder<>(supplier);
    }

    /**
     * Set value in POJO instance.
     * 
     * @param <V> The value of setter;
     * @param consumer Setter of POJO;
     * @param value The value for the given Setter;
     * @return Returns POJO builder.
     */
    public <V> PojoGenericBuilder<T> with(BiConsumer<T, V> consumer, V value)
    {
        consumer.accept(pojo, value);
        return this;
    }

    /**
     * Return the created instance.
     * 
     * @return Return given instance.
     */
    public T build()
    {
        return pojo;
    }
}
