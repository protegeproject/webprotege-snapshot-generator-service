package edu.stanford.protege.webprotege.snapshots;

import edu.stanford.protege.webprotege.ipc.CommandExecutor;
import edu.stanford.protege.webprotege.project.GetProjectPrefixDeclarationsRequest;
import edu.stanford.protege.webprotege.project.GetProjectPrefixDeclarationsResponse;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2021-09-24
 */
public class GetProjectPrefixDeclarationsExecutor extends CommandExecutor<GetProjectPrefixDeclarationsRequest, GetProjectPrefixDeclarationsResponse> {

    public GetProjectPrefixDeclarationsExecutor() {
        super(GetProjectPrefixDeclarationsResponse.class);
    }
}
