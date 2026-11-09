package com.example.catalog;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Full context on the "test" profile: H2 instead of PostgreSQL, placeholder
 * API keys, no seeding, CloudWatch export off (the default outside "aws"), and
 * no Secrets Manager import. The AWS SDK clients are built but never called,
 * and JwtDecoder is mocked so Cognito is never contacted.
 */
@SpringBootTest
@ActiveProfiles("test")
class ProductCatalogMvcApplicationTests {

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@Test
	void contextLoads() {
	}

}
