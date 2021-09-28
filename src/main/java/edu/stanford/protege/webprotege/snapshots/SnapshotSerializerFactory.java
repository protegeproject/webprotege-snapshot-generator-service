package edu.stanford.protege.webprotege.snapshots;

import edu.stanford.protege.webprotege.common.DocumentFormat;
import edu.stanford.protege.webprotege.common.ProjectId;
import edu.stanford.protege.webprotege.revision.RevisionManager;
import edu.stanford.protege.webprotege.revision.RevisionNumber;

import static java.util.Objects.requireNonNull;

public final class SnapshotSerializerFactory {

    private final GetProjectPrefixDeclarationsExecutor prefixDeclarationExecutor;

    public SnapshotSerializerFactory(GetProjectPrefixDeclarationsExecutor executor) {
        this.prefixDeclarationExecutor = requireNonNull(executor);
    }


    public SnapshotSerializer create(ProjectId projectId,
                                     String fileName,
                                     RevisionNumber revision,
                                     DocumentFormat format,
                                     RevisionManager revisionManager) {
        return new SnapshotSerializer(requireNonNull(projectId),
                                      requireNonNull(fileName),
                                      requireNonNull(revision),
                                      requireNonNull(format),
                                      requireNonNull(revisionManager),
                                      prefixDeclarationExecutor);
    }
}
