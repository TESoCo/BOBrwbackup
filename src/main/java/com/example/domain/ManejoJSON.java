package com.example.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

    @Converter
    public class ManejoJSON implements AttributeConverter<Map<Integer, Double>, String> {
        private final ObjectMapper objectMapper = new ObjectMapper();

        @Override
        public String convertToDatabaseColumn(Map<Integer, Double> attribute) {
            try {

                if (attribute == null) return "{}";

                // Create a new map filtering out null keys
                Map<Integer, Double> cleanMap = new HashMap<>();
                attribute.forEach((k, v) -> {
                    if (k != null) cleanMap.put(k, v);
                });

                return objectMapper.writeValueAsString(attribute);
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Error de map", e);
            }
        }

        @Override
        public Map<Integer, Double> convertToEntityAttribute(String dbData) {
            try {
                if (dbData == null || dbData.isEmpty()) {
                    return new HashMap<>();
                }
                return objectMapper.readValue(dbData, new TypeReference<Map<Integer, Double>>() {});
            } catch (IOException e) {
                throw new IllegalArgumentException("Error de JSON", e);
            }
        }
    }
