package com.example.csvfilter.service;

import com.example.csvfilter.csv.CsvExporter;
import com.example.csvfilter.csv.CsvParser;
import com.example.csvfilter.csv.TypeInferrer;
import com.example.csvfilter.model.UserSessionData;
import com.example.csvfilter.parser.Evaluator;
import com.example.csvfilter.parser.Parser;
import com.example.csvfilter.parser.Token;
import com.example.csvfilter.parser.Tokenizer;
import com.example.csvfilter.parser.ast.Expression;
import com.example.csvfilter.parser.exception.FilterException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort; // <-- IMPORT
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.Writer;
import java.util.Collections;
import java.util.Comparator; // <-- IMPORT
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DataService {

    private final UserSessionData userSessionData;
    private final CsvParser csvParser;
    private final TypeInferrer typeInferrer;
    private final CsvExporter csvExporter;

    public DataService(UserSessionData userSessionData, CsvParser csvParser, TypeInferrer typeInferrer, CsvExporter csvExporter) {
        this.userSessionData = userSessionData;
        this.csvParser = csvParser;
        this.typeInferrer = typeInferrer;
        this.csvExporter = csvExporter;
    }

    public void loadAndStoreCsv(InputStream inputStream) {
        List<Map<String, String>> rawRows = csvParser.parse(inputStream);
        if (rawRows.isEmpty()) {
            throw new IllegalArgumentException("CSV file is empty or invalid.");
        }
        List<String> headers = List.copyOf(rawRows.get(0).keySet());
        Map<String, Class<?>> schema = typeInferrer.inferSchema(rawRows, headers);

        // Coerce all string values to their inferred types
        List<Map<String, Object>> typedRows = rawRows.stream()
                .map(row -> typeInferrer.coerceRow(row, schema))
                .collect(Collectors.toList());

        userSessionData.setData(typedRows, headers, schema);
    }

    // --- METHOD MODIFIED to handle Pageable (which includes Sort) ---
    public Page<Map<String, Object>> getFilteredPaginatedData(String filter, Pageable pageable) {
        // 1. Get filtered data
        List<Map<String, Object>> filteredRows = getFilteredData(filter);

        // 2. Apply sorting (NEW)
        List<Map<String, Object>> sortedRows = sortData(filteredRows, pageable.getSort());

        // 3. Manual pagination (applied to sorted rows)
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), sortedRows.size());

        List<Map<String, Object>> pageContent = (start <= end) ? sortedRows.subList(start, end) : Collections.emptyList();

        return new PageImpl<>(pageContent, pageable, sortedRows.size());
    }

    // --- NEW METHOD for Sorting ---
    private List<Map<String, Object>> sortData(List<Map<String, Object>> data, Sort sort) {
        if (sort.isUnsorted()) {
            return data;
        }

        // We only support sorting by one column for this MVP
        Sort.Order order = sort.iterator().next();
        String property = order.getProperty();
        Comparator<Map<String, Object>> comparator = new RowComparator(property, order.getDirection());

        // We need to stream and collect to a new list to perform the sort
        return data.stream().sorted(comparator).collect(Collectors.toList());
    }

    // --- NEW Inner Class for Comparison ---
    private static class RowComparator implements Comparator<Map<String, Object>> {
        private final String sortKey;
        private final Sort.Direction direction;

        public RowComparator(String sortKey, Sort.Direction direction) {
            this.sortKey = sortKey;
            this.direction = direction;
        }

        @Override
        public int compare(Map<String, Object> row1, Map<String, Object> row2) {
            Object val1 = row1.get(sortKey);
            Object val2 = row2.get(sortKey);

            // Treat nulls as "less" than non-nulls
            if (val1 == null && val2 == null) return 0;
            if (val1 == null) return (direction == Sort.Direction.ASC) ? -1 : 1;
            if (val2 == null) return (direction == Sort.Direction.ASC) ? 1 : -1;

            int cmp;
            if (val1 instanceof Comparable && val2 instanceof Comparable) {
                // This works for String, Long, Double
                // We rely on our TypeInferrer to have made them comparable
                try {
                    @SuppressWarnings({"unchecked", "rawtypes"})
                    Comparable c1 = (Comparable) val1;
                    @SuppressWarnings({"unchecked", "rawtypes"})
                    Comparable c2 = (Comparable) val2;
                    cmp = c1.compareTo(c2);
                } catch (ClassCastException e) {
                    // Fallback to string comparison if types are mixed (e.g., Long vs Double)
                    cmp = val1.toString().compareTo(val2.toString());
                }
            } else {
                // Fallback for non-comparable types
                cmp = val1.toString().compareTo(val2.toString());
            }

            return (direction == Sort.Direction.ASC) ? cmp : -cmp;
        }
    }


    public void exportFilteredData(String filter, Writer writer) {
        // Note: Export does NOT use sorting from the UI. This is usually desired.
        List<Map<String, Object>> filteredRows = getFilteredData(filter);
        csvExporter.export(userSessionData.getHeaders(), filteredRows, writer);
    }

    private List<Map<String, Object>> getFilteredData(String filter) {
        if (filter == null || filter.isBlank()) {
            return userSessionData.getAllRows();
        }

        // 1. Tokenize
        Tokenizer tokenizer = new Tokenizer(filter);
        List<Token> tokens = tokenizer.tokenize();

        // 2. Parse (Build AST)
        Parser parser = new Parser(tokens, userSessionData.getColumnNames());
        Expression ast = parser.parse();

        // 3. Evaluate
        Evaluator evaluator = new Evaluator(userSessionData.getSchema());
        return userSessionData.getAllRows().stream()
                .filter(row -> {
                    try {
                        return evaluator.evaluate(ast, row);
                    } catch (FilterException e) {
                        return false;
                    }
                })
                .collect(Collectors.toList());
    }
}
