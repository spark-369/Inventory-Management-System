package com.unique.ims.backend.config;

import com.unique.ims.backend.dto.CategoryDto;
import com.unique.ims.backend.dto.ProductDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.SerializationException;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the Redis cache-value serializer.
 *
 * <p>These matter because the round trip is the whole point of the class: a
 * cached {@code List<Dto>} must come back as a {@code List<Dto>} with its
 * elements intact, not as a list of untyped maps. That is exactly the bug the
 * hand-rolled type prefix exists to prevent.</p>
 */
class GenericJackson3RedisSerializerTest {

    private GenericJackson3RedisSerializer serializer;

    @BeforeEach
    void setUp() {
        serializer = new GenericJackson3RedisSerializer(new ObjectMapper());
    }

    @Test
    @DisplayName("round-trips a single DTO preserving every field")
    void roundTripsSingleDto() {
        CategoryDto original = new CategoryDto();
        original.setId(7L);
        original.setName("Beverages");
        original.setDescription("Drinks and juices");

        Object restored = serializer.deserialize(serializer.serialize(original));

        assertThat(restored).isInstanceOf(CategoryDto.class);
        CategoryDto dto = (CategoryDto) restored;
        assertThat(dto.getId()).isEqualTo(7L);
        assertThat(dto.getName()).isEqualTo("Beverages");
        assertThat(dto.getDescription()).isEqualTo("Drinks and juices");
    }

    @Test
    @DisplayName("round-trips a list and restores the element type, not Maps")
    void roundTripsListWithElementType() {
        ProductDto first = new ProductDto();
        first.setId(1L);
        first.setSku("SKU-1");
        first.setName("Coffee");
        first.setSellingPrice(new BigDecimal("12.50"));

        ProductDto second = new ProductDto();
        second.setId(2L);
        second.setSku("SKU-2");
        second.setName("Tea");

        Object restored = serializer.deserialize(serializer.serialize(List.of(first, second)));

        assertThat(restored).isInstanceOf(List.class);
        List<?> list = (List<?>) restored;
        assertThat(list).hasSize(2);
        // The critical assertion: elements are DTOs, not LinkedHashMaps.
        assertThat(list).allSatisfy(element -> assertThat(element).isInstanceOf(ProductDto.class));
        assertThat(((ProductDto) list.get(0)).getSku()).isEqualTo("SKU-1");
        assertThat(((ProductDto) list.get(1)).getName()).isEqualTo("Tea");
    }

    @Test
    @DisplayName("round-trips an empty list")
    void roundTripsEmptyList() {
        Object restored = serializer.deserialize(serializer.serialize(List.of()));

        assertThat(restored).isInstanceOf(List.class);
        assertThat((List<?>) restored).isEmpty();
    }

    @Test
    @DisplayName("serializes null to an empty payload and reads it back as null")
    void handlesNull() {
        assertThat(serializer.serialize(null)).isEmpty();
        assertThat(serializer.deserialize(null)).isNull();
        assertThat(serializer.deserialize(new byte[0])).isNull();
    }

    @Test
    @DisplayName("tolerates a pipe character inside the JSON body")
    void toleratesSeparatorInsideJson() {
        CategoryDto original = new CategoryDto();
        original.setId(3L);
        original.setName("A|B");
        original.setDescription("pipes | everywhere |");

        Object restored = serializer.deserialize(serializer.serialize(original));

        assertThat(((CategoryDto) restored).getName()).isEqualTo("A|B");
        assertThat(((CategoryDto) restored).getDescription()).isEqualTo("pipes | everywhere |");
    }

    @Test
    @DisplayName("rejects a payload that has no type prefix instead of guessing")
    void rejectsMalformedPayload() {
        assertThatThrownBy(() -> serializer.deserialize("not-a-prefixed-value".getBytes()))
                .isInstanceOf(SerializationException.class)
                .hasMessageContaining("Malformed cache entry");
    }

    @Test
    @DisplayName("rejects an unknown class name rather than returning a partial object")
    void rejectsUnknownClass() {
        byte[] payload = "object|com.example.DoesNotExist|{}".getBytes();

        assertThatThrownBy(() -> serializer.deserialize(payload))
                .isInstanceOf(SerializationException.class);
    }
}
