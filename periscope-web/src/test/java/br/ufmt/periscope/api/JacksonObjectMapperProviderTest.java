package br.ufmt.periscope.api;

import br.ufmt.periscope.api.dto.ProjectRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JacksonObjectMapperProviderTest {

    private final ObjectMapper mapper = JacksonObjectMapperProvider.createMapper();

    @Test
    void ignoresUnknownProperties() throws Exception {
        ProjectRequest req = mapper.readValue(
                "{\"title\":\"X\",\"foo\":\"bar\",\"description\":\"d\"}",
                ProjectRequest.class);
        assertThat(req.title()).isEqualTo("X");
        assertThat(req.description()).isEqualTo("d");
    }

    @Test
    void defaultJacksonWouldFailOnUnknownProperty() {
        ObjectMapper strict = new ObjectMapper();
        assertThatThrownBy(() -> strict.readValue(
                "{\"title\":\"X\",\"foo\":\"bar\"}", ProjectRequest.class))
                .isInstanceOf(UnrecognizedPropertyException.class);
    }

    @Test
    void rejectsMalformedJson() {
        assertThatCode(() -> mapper.readValue("{", ProjectRequest.class))
                .isInstanceOf(com.fasterxml.jackson.core.JsonProcessingException.class);
    }
}
