package com.unique.ims.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Verifies the whole Spring context can be built.
 *
 * <p>Runs against the in-memory H2 database and a disabled JobRunr worker pool
 * configured in {@code src/test/resources/application.properties}, so it needs
 * no external PostgreSQL or Redis. This is the safety net that catches bean
 * wiring mistakes (a missing dependency, a conflicting cache manager, a broken
 * JobRunr configuration) that pure unit tests cannot see.</p>
 */
@SpringBootTest
class BackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
