package edu.stanford.protege.webprotege.snapshots;



import edu.stanford.protege.webprotege.common.DocumentFormat;
import edu.stanford.protege.webprotege.common.ProjectId;
import edu.stanford.protege.webprotege.ipc.ExecutionContext;
import edu.stanford.protege.webprotege.revision.RevisionManager;
import edu.stanford.protege.webprotege.revision.RevisionManagerFactory;
import edu.stanford.protege.webprotege.revision.RevisionNumber;
import edu.stanford.protege.webprotege.common.UserId;
import io.minio.*;
import io.minio.errors.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 14 Apr 2017
 */
class CreateSnapshotTask implements Supplier<SnapshotStorageCoordinates> {

    private static final Logger logger = LoggerFactory.getLogger(CreateSnapshotTask.class);

    public static final String BUCKET_NAME = "webprotege-snapshots";

    @Nonnull
    private final RevisionManagerFactory revisionManagerFactory;

    @Nonnull
    private final ProjectId projectId;

    @Nonnull
    private final UserId userId;

    @Nonnull
    private final String projectDisplayName;

    @Nonnull
    private final RevisionNumber revisionNumber;

    @Nonnull
    private final DocumentFormat format;

    @Nonnull
    private final SnapshotSerializerFactory snapshotSerializerFactory;

    private final ExecutionContext executionContext;

    private MinioClient minioClient;


    public CreateSnapshotTask(@Nonnull RevisionManagerFactory revisionManagerFactory,
                              @Nonnull ProjectId projectId,
                              @Nonnull UserId userId,
                              @Nonnull String projectDisplayName,
                              @Nonnull RevisionNumber revisionNumber,
                              @Nonnull DocumentFormat format,
                              @Nonnull SnapshotSerializerFactory snapshotSerializerFactory,
                              @Nonnull ExecutionContext executionContext,
                              @Nonnull MinioClient minioClient) {
        this.revisionManagerFactory = revisionManagerFactory;
        this.projectId = projectId;
        this.userId = userId;
        this.projectDisplayName = projectDisplayName;
        this.revisionNumber = revisionNumber;
        this.format = format;
        this.snapshotSerializerFactory = snapshotSerializerFactory;
        this.executionContext = executionContext;
        this.minioClient = minioClient;
    }

    @Override
    public SnapshotStorageCoordinates get() {
        try {
            logger.info("{} {} Processing snapshot request", projectId, userId);
            var existingCoordinates = getSnapshotCoordinates();
            if(existingCoordinates.isPresent()) {
                logger.info("{} {} Requested snapshot already exists.  Not recreating it.",
                            projectId,
                            userId);
                return existingCoordinates.get();
            }
            logger.info("{} {} Creating project snapshot", projectId, userId);
            var revisionManager = revisionManagerFactory.createRevisionManager(projectId);
            var resolvedRevisionNumber = resolveRevisionNumber(revisionManager);
            var minio = new SnapshotLocation(projectId, resolvedRevisionNumber, format);
            var downloader = snapshotSerializerFactory.create(projectId,
                                                              projectDisplayName,
                                                              resolvedRevisionNumber,
                                                              format,
                                                              revisionManager);

            var tmpFile = getTempFile();
            logger.info("{} {} Writing snapshot to temp file ({})", projectId, userId, tmpFile);
            try (BufferedOutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(tmpFile))) {
                downloader.writeProject(outputStream, executionContext);
            }
            double sizeInMB = Files.size(tmpFile) / (1024.0 * 1024);
            logger.info("{} {} Finished creating snapshot ({} MB)", projectId, userId, String.format("%.4f", sizeInMB));
            logger.info("{} {} Storing snapshot at {}", projectId, userId, minio.getLocation());


            createBucketIfNotExists();

            var uploadObjectArgs = UploadObjectArgs.builder()
                                                   .bucket(BUCKET_NAME)
                                                   .object(minio.getLocation())
                                                   .filename(tmpFile.toString())
                                                   .contentType("application/zip")
                                                   .build();
            var minioResponse = minioClient.uploadObject(uploadObjectArgs);
            logger.info("{} {} Stored snapshot (Bucket: {}, Name: {}, Region: {})", projectId, userId, minioResponse.bucket(), minio.getLocation(), minioResponse.region());
            logger.info("{} {} Removing temp file {}", projectId, userId, tmpFile);
            return new SnapshotStorageCoordinates(BUCKET_NAME,
                                          minio.getLocation());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (MinioException | NoSuchAlgorithmException | InvalidKeyException e) {
            logger.error("MinIO Error", e);
            throw new RuntimeException(e);
        }
    }

    private void createBucketIfNotExists() throws ErrorResponseException, InsufficientDataException, InternalException, InvalidKeyException, InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException, XmlParserException {
        if(!minioClient.bucketExists(BucketExistsArgs.builder()
                                         .bucket(BUCKET_NAME)
                                                 .build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(BUCKET_NAME).build());
        }
    }

    private Optional<SnapshotStorageCoordinates> getSnapshotCoordinates() {
        try {
            if (!revisionNumber.isHead()) {
                var minio = new SnapshotLocation(projectId, revisionNumber, format);
                var name = minio.getLocation();

                var stats = minioClient.statObject(StatObjectArgs.builder().bucket(BUCKET_NAME).object(name).build());
                if(stats.size() != 0) {
                    return Optional.of(new SnapshotStorageCoordinates(BUCKET_NAME, name));
                }
            }
            return Optional.empty();
        } catch (Exception e) {
            logger.error("An error occurred", e);
            return Optional.empty();
        }
    }

    @Nonnull
    private RevisionNumber resolveRevisionNumber(RevisionManager revisionManager) {
        if(revisionNumber.isHead()) {
            return revisionManager.getCurrentRevision();
        }
        else {
            return revisionNumber;
        }
    }

    @Nonnull
    private Path getTempFile() throws UncheckedIOException {
        try {
            return Files.createTempFile(BUCKET_NAME, null);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
