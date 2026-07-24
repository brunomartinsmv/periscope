package br.ufmt.periscope.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.List;

/**
 * Carrega listas YAML do classpath (substitui fixjures).
 */
public final class YamlLoader {

    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private YamlLoader() {
    }

    public static <T> List<T> loadList(String resourcePath, Class<T> type) {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
        if (is == null) {
            is = YamlLoader.class.getClassLoader().getResourceAsStream(resourcePath);
        }
        if (is == null) {
            throw new IllegalArgumentException("YAML resource not found: " + resourcePath);
        }
        try {
            return loadList(is, type);
        } finally {
            try {
                is.close();
            } catch (IOException ignored) {
                // ignore
            }
        }
    }

    public static <T> List<T> loadList(InputStream is, Class<T> type) {
        try {
            CollectionType listType = MAPPER.getTypeFactory().constructCollectionType(List.class, type);
            List<T> result = MAPPER.readValue(is, listType);
            return result == null ? Collections.<T>emptyList() : result;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to parse YAML for " + type.getName(), e);
        }
    }
}
