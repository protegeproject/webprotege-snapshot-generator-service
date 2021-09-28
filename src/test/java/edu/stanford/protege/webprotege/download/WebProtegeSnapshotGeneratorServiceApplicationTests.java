package edu.stanford.protege.webprotege.download;

import io.minio.MinioClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;

import static org.mockito.Mockito.mock;

@SpringBootTest
@ActiveProfiles("test")
class WebProtegeSnapshotGeneratorServiceApplicationTests {

	@Test
	void contextLoads() {
	}

	@Bean
	MinioClient minioClient() {
		return mock(MinioClient.class);
	}
}
