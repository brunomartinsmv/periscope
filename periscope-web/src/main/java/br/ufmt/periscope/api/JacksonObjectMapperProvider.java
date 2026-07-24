package br.ufmt.periscope.api;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;

@Provider
public class JacksonObjectMapperProvider implements ContextResolver<ObjectMapper> {

    private final ObjectMapper mapper;

    public JacksonObjectMapperProvider() {
        mapper = createMapper();
    }

    /** Shared factory for production CDI and unit tests. */
    public static ObjectMapper createMapper() {
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new JavaTimeModule());
        om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // SPA clients may send forward-compatible extra fields
        om.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        om.disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);
        om.disable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
        return om;
    }

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return mapper;
    }
}
