package edu.stanford.protege.webprotege.snapshots;

import edu.stanford.protege.webprotege.common.DocumentFormat;
import edu.stanford.protege.webprotege.common.ProjectId;
import edu.stanford.protege.webprotege.ipc.ExecutionContext;
import edu.stanford.protege.webprotege.revision.RevisionManagerFactory;
import edu.stanford.protege.webprotege.revision.RevisionNumber;
import edu.stanford.protege.webprotege.common.UserId;
import io.minio.MinioClient;

import javax.annotation.Nonnull;
import java.util.Objects;

final class CreateSnapshotTaskFactory {

  private final RevisionManagerFactory revisionManagerFactory;

  private final SnapshotSerializerFactory snapshotSerializerFactory;

  private final MinioClient minioClient;

  public CreateSnapshotTaskFactory(RevisionManagerFactory revisionManagerFactory,
                                   SnapshotSerializerFactory snapshotSerializerFactory,
                                   MinioClient minioClient) {
    this.revisionManagerFactory = revisionManagerFactory;
    this.snapshotSerializerFactory = snapshotSerializerFactory;
    this.minioClient = minioClient;
  }

  @Nonnull
  CreateSnapshotTask create(ExecutionContext executionContext,
                            ProjectId projectId,
                            UserId userId,
                            String projectDisplayName,
                            RevisionNumber revisionNumber,
                            DocumentFormat format) {
    return new CreateSnapshotTask(
            revisionManagerFactory,
            Objects.requireNonNull(projectId),
            Objects.requireNonNull(userId),
            Objects.requireNonNull(projectDisplayName),
            Objects.requireNonNull(revisionNumber),
            Objects.requireNonNull(format), snapshotSerializerFactory,
            executionContext,
            minioClient);
  }
}
