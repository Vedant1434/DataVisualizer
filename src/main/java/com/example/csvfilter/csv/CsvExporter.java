package com.example.csvfilter.csv;

import com.opencsv.CSVWriter;
import org.springframework.stereotype.Component;

import java.io.Writer;
import java.util.List;
import java.util.Map;

@Component
public class CsvExporter {
    public void export(List<String> headers, List<Map<String, Object>> rows, Writer writer) {
        try (CSVWriter csvWriter = new CSVWriter(writer)) {
            // Write header
            csvWriter.writeNext(headers.toArray(new String[0]));

            // Write rows
            for (Map<String, Object> row : rows) {
                String[] line = new String[headers.size()];
                for (int i = 0; i < headers.size(); i++) {
                    Object value = row.get(headers.get(i));
                    line[i] = (value != null) ? value.toString() : "";
                }
                csvWriter.writeNext(line);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error writing CSV export: " + e.getMessage(), e);
        }
    }
}