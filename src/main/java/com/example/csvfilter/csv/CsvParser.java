package com.example.csvfilter.csv;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class CsvParser {
    public List<Map<String, String>> parse(InputStream inputStream) {
        List<Map<String, String>> result = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new InputStreamReader(inputStream))) {
            String[] headers = reader.readNext();
            if (headers == null) {
                return result; // Empty file
            }

            // Check for duplicate headers
            checkDuplicateHeaders(headers);

            String[] line;
            while ((line = reader.readNext()) != null) {
                Map<String, String> row = new LinkedHashMap<>();
                for (int i = 0; i < headers.length; i++) {
                    row.put(headers[i], (i < line.length) ? line[i] : null);
                }
                result.add(row);
            }
        } catch (IOException | CsvValidationException e) {
            throw new RuntimeException("Error reading CSV file: " + e.getMessage(), e);
        }
        return result;
    }

    private void checkDuplicateHeaders(String[] headers) {
        List<String> seen = new ArrayList<>();
        for (String header : headers) {
            if (seen.contains(header)) {
                throw new IllegalArgumentException("Invalid CSV: Duplicate column name found: '" + header + "'");
            }
            seen.add(header);
        }
    }
}