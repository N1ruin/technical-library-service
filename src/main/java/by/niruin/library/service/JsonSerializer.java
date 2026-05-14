package by.niruin.library.service;

import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class JsonSerializer {
    private final ObjectMapper objectMapper;

    public JsonSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String serialize(Object object) {
        return objectMapper.writeValueAsString(object);
    }
}
