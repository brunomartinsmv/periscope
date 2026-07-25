package br.ufmt.periscope.api.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Maps Jackson parse/mapping failures to HTTP 400 (never 500 / stacktrace).
 */
@Provider
@Priority(Priorities.USER)
public class JsonProcessingExceptionMapper implements ExceptionMapper<JsonProcessingException> {

    @Override
    public Response toResponse(JsonProcessingException exception) {
        String message = exception.getOriginalMessage();
        if (message == null || message.isBlank()) {
            message = "Malformed JSON";
        } else if (message.length() > 200) {
            message = message.substring(0, 200);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "Invalid JSON: " + message);
        body.put("status", 400);
        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }
}
