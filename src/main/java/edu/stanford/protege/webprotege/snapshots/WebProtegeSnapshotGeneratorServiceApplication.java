package edu.stanford.protege.webprotege.snapshots;

import edu.stanford.protege.webprotege.WebprotegeBackendApi;
import edu.stanford.protege.webprotege.common.WebProtegeCommonConfiguration;
import edu.stanford.protege.webprotege.ipc.WebProtegeIpcApplication;
import edu.stanford.protege.webprotege.jackson.WebProtegeJacksonApplication;
import edu.stanford.protege.webprotege.revision.RevisionManagerFactory;
import edu.stanford.protege.webprotege.revision.WebProtegeRevisionManagerApplication;
import io.minio.MinioClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootApplication
@EnableConfigurationProperties
@Import({WebProtegeRevisionManagerApplication.class, WebProtegeJacksonApplication.class, WebprotegeBackendApi.class, WebProtegeIpcApplication.class, WebProtegeCommonConfiguration.class})
public class WebProtegeSnapshotGeneratorServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebProtegeSnapshotGeneratorServiceApplication.class, args);
    }

    @Bean
    SnapshotSerializerFactory projectDownloaderFactory(GetProjectPrefixDeclarationsExecutor p1) {
        return new SnapshotSerializerFactory(p1);
    }

    @Bean
    GetProjectPrefixDeclarationsExecutor getProjectPrefixes() {
        return new GetProjectPrefixDeclarationsExecutor();
    }

    @Bean
    MinioClient minioClient(MinioProperties minioProperties) {
        return MinioClient.builder().credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey()).endpoint(minioProperties.getEndPoint()).build();
    }

    @Bean
    ExecutorService downloadExecutorService() {
        return Executors.newSingleThreadExecutor();
    }

    @Bean
    CreateSnapshotTaskFactory createDownloadTaskFactory(RevisionManagerFactory revisionManagerFactory,
                                                        SnapshotSerializerFactory snapshotSerializerFactory,
                                                        MinioClient minioClient) {
        return new CreateSnapshotTaskFactory(revisionManagerFactory, snapshotSerializerFactory, minioClient);
    }
}
