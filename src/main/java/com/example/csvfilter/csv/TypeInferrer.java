package com.example.csvfilter.csv;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class TypeInferrer {

    private static final int ROWS_TO_SCAN = 100;

    public Map<String, Class<?>> inferSchema(List<Map<String, String>> rawRows, List<String> headers) {
        Map<String, Class<?>> schema = new HashMap<>();
        for (String header : headers) {
            schema.put(header, inferColumnType(rawRows, header));
        }
        return schema;
    }

    private Class<?> inferColumnType(List<Map<String, String>> rows, String header) {
        // Simple type inference. Scan N rows.
        // Start with most specific (Long) and widen to Double, then String.
        Class<?> currentBestType = Long.class;

        int rowsScanned = 0;
        for (Map<String, String> row : rows) {
            if (rowsScanned++ > ROWS_TO_SCAN) break;
            String value = row.get(header);
            if (value == null || value.isBlank() || value.equalsIgnoreCase("null")) {
                continue; // Nulls don't tell us anything
            }

            if (currentBestType == Long.class) {
                if (!canParseLong(value)) {
                    currentBestType = Double.class; // Try double
                }
            }

            if (currentBestType == Double.class) {
                if (!canParseDouble(value)) {
                    currentBestType = Boolean.class; // Try boolean
                }
            }

            if (currentBestType == Boolean.class) {
                if (!canParseBoolean(value)) {
                    currentBestType = String.class; // Give up, it's a string
                    break; // No wider type
                }
            }
        }
        // If all values were null, default to String
        if (rowsScanned == 0) return String.class;

        return currentBestType;
    }

    public Map<String, Object> coerceRow(Map<String, String> row, Map<String, Class<?>> schema) {
        Map<String, Object> typedRow = new LinkedHashMap<>();
        for (String header : schema.keySet()) {
            String value = row.get(header);
            Class<?> type = schema.get(header);
            typedRow.put(header, coerceValue(value, type));
        }
        return typedRow;
    }

    private Object coerceValue(String value, Class<?> type) {
        if (value == null || value.isBlank() || value.equalsIgnoreCase("null")) {
            return null;
        }
        try {
            if (type == Long.class) {
                return Long.parseLong(value);
            }
            if (type == Double.class) {
                return Double.parseDouble(value);
            }
            if (type == Boolean.class) {
                return parseBoolean(value);
            }
        } catch (NumberFormatException e) {
            // This can happen if inference was wrong (e.g. zip codes)
            return null; // Treat un-parseable data as null
        }
        return value; // Default to string
    }

    // Helper methods for inference
    private boolean canParseLong(String s) { try { Long.parseLong(s); return true; } catch (NumberFormatException e) { return false; } }
    private boolean canParseDouble(String s) { try { Double.parseDouble(s); return true; } catch (NumberFormatException e) { return false; } }
    private boolean canParseBoolean(String s) { return s.equalsIgnoreCase("true") || s.equalsIgnoreCase("false"); }
    private boolean parseBoolean(String s) { return s.equalsIgnoreCase("true"); }
}