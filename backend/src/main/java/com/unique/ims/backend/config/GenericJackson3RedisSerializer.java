package com.unique.ims.backend.config;

import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;

/**
 * JSON cache-value serializer built directly on Spring Boot 4's Jackson 3
 * {@link ObjectMapper}.
 *
 * <p>Why not use a built-in generic Jackson serializer? Those rely on Jackson's
 * global default typing to embed {@code @class} markers. This implementation
 * instead records the element type in a tiny prefix of its own, which keeps the
 * cached payload clean, avoids any dependency on version-specific serializer
 * class names, and still round-trips both single DTOs and {@code List<DTO>}
 * correctly (the case that matters here, since every list endpoint caches a
 * list).</p>
 *
 * <p>Wire format — two metadata fields, then the raw JSON body:</p>
 * <pre>
 *   object|&lt;declared-class&gt;|{...}        a single object
 *   list|&lt;element-class&gt;|[{...},{...}]   a collection
 * </pre>
 * <p>Class names never contain {@code |}, and the body is split with a limit of
 * three, so a {@code |} appearing inside the JSON itself is harmless.</p>
 */
public class GenericJackson3RedisSerializer implements RedisSerializer<Object> {

    private static final String KIND_OBJECT = "object";
    private static final String KIND_LIST = "list";
    private static final String SEPARATOR = "|";
    private static final int PREFIX_LIMIT = 3;
    private static final byte[] EMPTY = new byte[0];

    private final ObjectMapper objectMapper;

    public GenericJackson3RedisSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public byte[] serialize(Object value) throws SerializationException {
        if (value == null) {
            return EMPTY;
        }
        try {
            if (value instanceof Collection<?> collection) {
                Class<?> elementType = collection.stream()
                        .filter(java.util.Objects::nonNull)
                        .map(Object::getClass)
                        .findFirst()
                        .orElse(null);
                String prefix = KIND_LIST + SEPARATOR + (elementType == null ? "" : elementType.getName()) + SEPARATOR;
                return (prefix + objectMapper.writeValueAsString(value)).getBytes(StandardCharsets.UTF_8);
            }
            String prefix = KIND_OBJECT + SEPARATOR + value.getClass().getName() + SEPARATOR;
            return (prefix + objectMapper.writeValueAsString(value)).getBytes(StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new SerializationException("Unable to serialize cache value of type "
                    + value.getClass().getName(), ex);
        }
    }

    @Override
    public Object deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        String payload = new String(bytes, StandardCharsets.UTF_8);
        String[] parts = payload.split("\\" + SEPARATOR, PREFIX_LIMIT);
        if (parts.length < PREFIX_LIMIT) {
            throw new SerializationException("Malformed cache entry: missing type prefix");
        }

        String kind = parts[0];
        String className = parts[1];
        String json = parts[2];

        try {
            if (KIND_LIST.equals(kind)) {
                if (className.isEmpty()) {
                    // The collection was empty when cached, so there is no element
                    // type to restore — an empty list is the faithful result.
                    return new ArrayList<>();
                }
                Class<?> elementType = Class.forName(className);
                // readValue(String, JavaType) already returns a fully built
                // ArrayList whose elements are typed by the JavaType, so it is
                // returned directly. Chaining on it would infer the generic as
                // Object and lose the element type.
                return objectMapper.readValue(json, objectMapper.getTypeFactory()
                        .constructCollectionType(ArrayList.class, elementType));
            }
            return objectMapper.readValue(json, Class.forName(className));
        } catch (Exception ex) {
            throw new SerializationException("Unable to deserialize cache entry of type " + className, ex);
        }
    }
}
