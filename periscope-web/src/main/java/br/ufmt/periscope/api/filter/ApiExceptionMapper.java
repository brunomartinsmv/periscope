package br.ufmt.periscope.api.filter;

import br.ufmt.periscope.api.ApiException;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Provider
public class ApiExceptionMapper implements ExceptionMapper<Exception> {

    private static final Logger LOG = Logger.getLogger(ApiExceptionMapper.class.getName());

    @Override
    public Response toResponse(Exception exception) {
        JsonProcessingException jsonEx = findCause(exception, JsonProcessingException.class);
        if (jsonEx != null) {
            String message = jsonEx.getOriginalMessage();
            if (message == null || message.isBlank()) {
                message = "Malformed JSON";
            } else if (message.length() > 200) {
                message = message.substring(0, 200);
            }
            return json(400, "Invalid JSON: " + message);
        }
        if (exception instanceof BadRequestException
                || exception instanceof IllegalArgumentException) {
            String message = exception.getMessage();
            return json(400, message != null && !message.isBlank() ? message : "Bad request");
        }
        if (exception instanceof ProcessingException && exception.getCause() != null) {
            Throwable cause = exception.getCause();
            if (cause instanceof IllegalArgumentException || cause instanceof BadRequestException) {
                String message = cause.getMessage();
                return json(400, message != null && !message.isBlank() ? message : "Bad request");
            }
        }
        if (exception instanceof WebApplicationException wae) {
            Response response = wae.getResponse();
            int status = response != null ? response.getStatus() : 500;
            String message = wae.getMessage();
            if (message == null || message.isBlank()) {
                message = Response.Status.fromStatusCode(status) != null
                        ? Response.Status.fromStatusCode(status).getReasonPhrase()
                        : "Error";
            }
            if (status >= 500) {
                LOG.log(Level.SEVERE, "API error", exception);
                message = "Internal server error";
            }
            return json(status, message);
        }
        if (exception instanceof ApiException api) {
            return json(api.getStatus(), api.getMessage());
        }
        if (exception instanceof NotFoundException) {
            return json(404, exception.getMessage() != null ? exception.getMessage() : "Not found");
        }
        if (exception instanceof NotAuthorizedException) {
            return json(401, "Unauthorized");
        }
        LOG.log(Level.SEVERE, "Unhandled API exception", exception);
        return json(500, "Internal server error");
    }

    private static <T extends Throwable> T findCause(Throwable ex, Class<T> type) {
        Throwable current = ex;
        int depth = 0;
        while (current != null && depth++ < 10) {
            if (type.isInstance(current)) {
                return type.cast(current);
            }
            current = current.getCause();
        }
        return null;
    }

    private static Response json(int status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", message);
        body.put("status", status);
        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }
}
