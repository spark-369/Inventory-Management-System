package com.unique.ims.backend.config;

/**
 * Single source of truth for the Redis cache names used by {@code @Cacheable},
 * {@code @CacheEvict} and the TTL table in {@link CacheConfig}.
 *
 * <p>Keeping the names in one place means a typo can never silently create a
 * second, unbounded cache region.</p>
 */
public final class CacheNames {

    /** Reference data — small, rarely written, read on nearly every screen. */
    public static final String CATEGORIES = "categories";
    public static final String BRANDS = "brands";
    public static final String WAREHOUSES = "warehouses";
    public static final String CUSTOMERS = "customers";
    public static final String SUPPLIERS = "suppliers";

    /** Catalogue — larger and written more often, so it gets a shorter TTL. */
    public static final String PRODUCTS = "products";

    /** Expensive derived list (full table scan + filter). */
    public static final String LOW_STOCK_PRODUCTS = "lowStockProducts";

    private CacheNames() {
    }
}
