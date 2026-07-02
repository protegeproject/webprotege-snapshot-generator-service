package edu.stanford.protege.webprotege.snapshots;

import edu.stanford.protege.webprotege.ipc.CommandExecutor;
import edu.stanford.protege.webprotege.ipc.impl.CommandExecutorImpl;
import edu.stanford.protege.webprotege.project.GetProjectPrefixDeclarationsRequest;
import edu.stanford.protege.webprotege.project.GetProjectPrefixDeclarationsResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration that provides a CommandExecutor bean for getting project
 * prefix declarations from the backend service via RabbitMQ.
 */
@Configuration
public class GetProjectPrefixDeclarationsExecutor {

    @Bean
    public CommandExecutor<GetProjectPrefixDeclarationsRequest, GetProjectPrefixDeclarationsResponse> prefixDeclarationsExecutor() {
        return new CommandExecutorImpl<>(GetProjectPrefixDeclarationsResponse.class);
    }
}
