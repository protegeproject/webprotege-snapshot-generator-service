package edu.stanford.protege.webprotege.snapshots;



import edu.stanford.protege.webprotege.common.DocumentFormat;
import edu.stanford.protege.webprotege.ipc.CommandExecutor;
import edu.stanford.protege.webprotege.ipc.ExecutionContext;
import edu.stanford.protege.webprotege.project.GetProjectPrefixDeclarationsRequest;
import edu.stanford.protege.webprotege.project.GetProjectPrefixDeclarationsResponse;
import edu.stanford.protege.webprotege.common.ProjectId;
import edu.stanford.protege.webprotege.revision.RevisionManager;
import edu.stanford.protege.webprotege.revision.RevisionNumber;
import org.semanticweb.owlapi.formats.PrefixDocumentFormat;
import org.semanticweb.owlapi.io.OWLOntologyStorageIOException;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.util.OntologyIRIShortFormProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.concurrent.ExecutionException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Author: Matthew Horridge<br>
 * Stanford University<br>
 * Bio-Medical Informatics Research Group<br>
 * Date: 06/06/2012
 *
 * Stores a snapshot (revision) of the ontologies in a project as a zip file.
 */
public class SnapshotSerializer {

    private static final Logger logger = LoggerFactory.getLogger(SnapshotSerializer.class);

    @Nonnull
    private final RevisionNumber revision;

    @Nonnull
    private final DocumentFormat format;

    @Nonnull
    private final String fileName;

    @Nonnull
    private final RevisionManager revisionManager;

    @Nonnull
    private final ProjectId projectId;

    @Nonnull
    private final CommandExecutor<GetProjectPrefixDeclarationsRequest, GetProjectPrefixDeclarationsResponse> prefixDeclarations;

    /**
     * Creates a project downloader that downloads the specified revision of the specified project.
     *  @param revision                The revision of the project to be downloaded.
     * @param format                  The format which the project should be downloaded in.
     * @param revisionManager         The revision manager of project to be downloaded.  Not <code>null</code>.
     * @param prefixDeclarationsExecutor An executor for getting project prefix declarations
     */

    @Inject
    public SnapshotSerializer(@Nonnull ProjectId projectId,
                              @Nonnull String fileName,
                              @Nonnull RevisionNumber revision,
                              @Nonnull DocumentFormat format,
                              @Nonnull RevisionManager revisionManager,
                              @Nonnull GetProjectPrefixDeclarationsExecutor prefixDeclarationsExecutor) {
        this.projectId = checkNotNull(projectId);
        this.revision = checkNotNull(revision);
        this.revisionManager = checkNotNull(revisionManager);
        this.format = checkNotNull(format);
        this.fileName = checkNotNull(fileName);
        this.prefixDeclarations = prefixDeclarationsExecutor;
    }

    public void writeProject(OutputStream outputStream,
                             ExecutionContext executionContext) throws UncheckedIOException {
            exportProjectRevision(executionContext, fileName, revision, outputStream, format);
    }

    private void exportProjectRevision(@Nonnull ExecutionContext executionContext,
                                       @Nonnull String projectDisplayName,
                                       @Nonnull RevisionNumber revisionNumber,
                                       @Nonnull OutputStream outputStream,
                                       @Nonnull DocumentFormat format) throws UncheckedIOException {
        OWLOntologyManager manager = revisionManager.getOntologyManagerForRevision(revisionNumber);
        saveOntologiesToStream(executionContext, projectDisplayName, manager, format, outputStream, revisionNumber);
    }

    private void saveOntologiesToStream(@Nonnull ExecutionContext executionContext,
                                        @Nonnull String projectDisplayName,
                                        @Nonnull OWLOntologyManager manager,
                                        @Nonnull DocumentFormat format,
                                        @Nonnull OutputStream outputStream,
                                        @Nonnull RevisionNumber revisionNumber) throws UncheckedIOException {


        // TODO: Separate object
        try(ZipOutputStream zipOutputStream = new ZipOutputStream(new BufferedOutputStream(outputStream))) {
            String baseFolder = projectDisplayName.replace(" ", "-") + "-ontologies-" + format.getExtension();
            baseFolder = baseFolder.toLowerCase();
            baseFolder = baseFolder + "-REVISION-" + (revisionNumber.isHead() ? "HEAD" : revisionNumber.getValue());
            for(var ontology : manager.getOntologies()) {
                var documentFormat = format.getDocumentFormat();
                if(documentFormat.isPrefixOWLOntologyFormat()) {
                    var prefixDocumentFormat = documentFormat.asPrefixOWLOntologyFormat();
                    copyPrefixes(executionContext, prefixDocumentFormat);
                }
                var ontologyShortForm = getOntologyShortForm(ontology);
                var ontologyDocumentFileName = ontologyShortForm.replace(":", "_");
                ZipEntry zipEntry = new ZipEntry(baseFolder + "/" + ontologyDocumentFileName + "." + format.getExtension());
                zipOutputStream.putNextEntry(zipEntry);
                ontology.getOWLOntologyManager().saveOntology(ontology, documentFormat, zipOutputStream);
                zipOutputStream.closeEntry();
            }
            zipOutputStream.finish();
            zipOutputStream.flush();
        }
        catch (IOException ioException) {
            throw new UncheckedIOException(ioException);
        }
        catch (OWLOntologyStorageIOException e) {
            throw new UncheckedIOException(e.getIOException());
        }
        catch (OWLOntologyStorageException e) {
            throw new RuntimeException(e);
        }
    }

    private void copyPrefixes(ExecutionContext executionContext,
                           PrefixDocumentFormat prefixDocumentFormat) {
        try {
            // If there's an error then it's not the end of the world.  The resulting document may
            // not look pretty but it will still be valid
            var prefixes = prefixDeclarations.execute(new GetProjectPrefixDeclarationsRequest(projectId), executionContext).get();
            prefixes.prefixDeclarations()
                    .forEach(prefix -> prefixDocumentFormat.setPrefix(prefix.prefixName(), prefix.prefix()));
        } catch (InterruptedException e) {
            logger.error("An interruption occurred while waiting for the prefixes for {}", projectId);
        } catch (ExecutionException e) {
            logger.error("An error occurred while retrieving the prefixes for {} ({})", projectId, e.getCause().getMessage());
        }
    }

    private String getOntologyShortForm(OWLOntology ontology) {
        return new OntologyIRIShortFormProvider().getShortForm(ontology);
    }
}
