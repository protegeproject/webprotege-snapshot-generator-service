package edu.stanford.protege.webprotege.snapshots;

import edu.stanford.protege.webprotege.ipc.CommandHandler;
import edu.stanford.protege.webprotege.ipc.ExecutionContext;
import edu.stanford.protege.webprotege.ipc.WebProtegeHandler;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2021-09-23
 */
@WebProtegeHandler
public class CreateSnapshotCommandHandler implements CommandHandler<CreateSnapshotRequest, CreateSnapshotResponse> {


    @Nonnull
    private final CreateSnapshotTaskFactory createSnapshotTaskFactory;

    @Nonnull
    private final ExecutorService executorService;

    public CreateSnapshotCommandHandler(@Nonnull CreateSnapshotTaskFactory createSnapshotTaskFactory,
                                        @Nonnull ExecutorService executorService) {
        this.createSnapshotTaskFactory = createSnapshotTaskFactory;
        this.executorService = executorService;
    }

    @Nonnull
    @Override
    public String getChannelName() {
        return CreateSnapshotRequest.CHANNEL;
    }

    @Override
    public Class<CreateSnapshotRequest> getRequestClass() {
        return CreateSnapshotRequest.class;
    }

    @Override
    public Mono<CreateSnapshotResponse> handleRequest(CreateSnapshotRequest request,
                                                      ExecutionContext executionContext) {
        var future = new CompletableFuture<SnapshotStorageCoordinates>();
        var task = createSnapshotTaskFactory.create(executionContext, request.projectId(),
                                                                  executionContext.userId(),
                                                                  request.fileName(),
                                                                  request.revisionNumber(),
                                                                  request.documentFormat());
        var response = future.completeAsync(task, executorService)
                .thenApply(CreateSnapshotResponse::new);
        return Mono.fromFuture(response);
    }
}
