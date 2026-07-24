package br.ufmt.periscope.rest;

import br.ufmt.periscope.bean.SeedBean;
import com.mongodb.client.MongoClient;
import dev.morphia.Datastore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.File;
import java.nio.file.Files;
import org.bson.Document;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * Liveness/readiness probe: MongoDB ping + Lucene index directory under {@code PERISCOPE_DIR}.
 * <p>
 * URL: {@code /periscope/rest/health} (see {@link JaxRsActivator}).
 * Response body is built as a JSON string (JSON-P not required) so WildFly can serve it
 * even with server Jackson JAX-RS modules excluded from the deployment.
 */
@Path("/health")
@ApplicationScoped
@Tag(name = "Health")
public class HealthResource {

    @Inject
    private Datastore datastore;

    @Inject
    private MongoClient mongoClient;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Health check", description = "MongoDB ping + diretório Lucene")
    public Response health() {
        boolean mongoUp = isMongoUp();
        boolean luceneUp = isLuceneIndexReadable();
        boolean up = mongoUp && luceneUp;

        String json = "{"
                + "\"status\":\"" + (up ? "UP" : "DOWN") + "\","
                + "\"mongodb\":\"" + (mongoUp ? "UP" : "DOWN") + "\","
                + "\"luceneIndex\":\"" + (luceneUp ? "UP" : "DOWN") + "\""
                + "}";

        return Response.status(up ? Response.Status.OK : Response.Status.SERVICE_UNAVAILABLE)
                .type(MediaType.APPLICATION_JSON)
                .entity(json)
                .build();
    }

    private boolean isMongoUp() {
        try {
            if (datastore != null) {
                Document pong = datastore.getDatabase().runCommand(new Document("ping", 1));
                return pong != null && Double.valueOf(1.0).equals(pong.getDouble("ok"));
            }
            if (mongoClient != null) {
                Document pong = mongoClient.getDatabase("admin").runCommand(new Document("ping", 1));
                return pong != null && Double.valueOf(1.0).equals(pong.getDouble("ok"));
            }
            return false;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private boolean isLuceneIndexReadable() {
        try {
            String dirPath = SeedBean.PERISCOPE_DIR;
            if (dirPath == null || dirPath.isBlank()) {
                dirPath = System.getenv().getOrDefault("PERISCOPE_DIR", "/opt/periscope");
            }
            File dir = new File(dirPath);
            if (!dir.isDirectory() || !dir.canRead() || !dir.canWrite()) {
                return false;
            }
            File[] children = dir.listFiles();
            if (children == null) {
                return false;
            }
            for (File child : children) {
                if (child.isFile() && child.getName().startsWith("segments") && Files.isReadable(child.toPath())) {
                    return true;
                }
            }
            // Empty but writable index dir is acceptable before first import/reindex
            return dir.canWrite();
        } catch (RuntimeException ex) {
            return false;
        }
    }
}
