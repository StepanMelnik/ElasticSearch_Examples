package util;

import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The Utility to work with json object.
 */
public final class ObjectMapperUtil
{
    private static final Logger LOGGER = LogManager.getLogger(ObjectMapperUtil.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static
    {
        OBJECT_MAPPER.setVisibility(OBJECT_MAPPER
                .getSerializationConfig()
                .getDefaultVisibilityChecker()
                .with(Visibility.NONE)
                .withFieldVisibility(Visibility.ANY));
        OBJECT_MAPPER.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
    }

    // private class
    private ObjectMapperUtil()
    {
    }

    /**
     * @param <T>
     * @param data
     * @return
     */
    public static <T> String serialize(T data)
    {
        Objects.requireNonNull(data);
        try
        {
            return OBJECT_MAPPER.writeValueAsString(data);
        }
        catch (JsonProcessingException e)
        {
            LOGGER.error("Cannot serialize data: " + data, e);  // never must happen
            return "";
        }
    }

    /**
     * @param <T>
     * @param resultClass
     * @param data
     * @return
     */
    public static <T> T deserialize(Class<T> resultClass, String data)
    {
        try
        {
            return OBJECT_MAPPER.readValue(data, resultClass);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Cannot deserizlize data: " + data);
        }
    }

    /**
     * @param <T>
     * @param fromValue
     * @param toType
     * @return
     */
    public static <T> T convert(Object fromValue, Class<T> toType)
    {
        return OBJECT_MAPPER.convertValue(fromValue, toType);
    }
}
