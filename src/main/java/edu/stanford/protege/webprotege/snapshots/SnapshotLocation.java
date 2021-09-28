package edu.stanford.protege.webprotege.snapshots;

import edu.stanford.protege.webprotege.common.DocumentFormat;
import edu.stanford.protege.webprotege.common.ProjectId;
import edu.stanford.protege.webprotege.revision.RevisionNumber;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2021-09-27
 */
public class SnapshotLocation {

    private final ProjectId projectId;

    private final RevisionNumber revisionNumber;

    private final DocumentFormat format;

    public SnapshotLocation(ProjectId projectId, RevisionNumber revisionNumber, DocumentFormat format) {
        this.projectId = projectId;
        this.revisionNumber = revisionNumber;
        this.format = format;
    }

    /**
     * Gets the location/name for the stored snapshot
     * @return A location/name for the stored snapshot that will be used in a (e.g. S3) storage service
     */
    public String getLocation() {
        return "/projects/%s/revisions/%d/%s".formatted(projectId.id(),
                                                        revisionNumber.getValue(),
                                                        format.getExtension());
    }
}
