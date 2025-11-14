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
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.Writer;
import java.util.Collections;
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

    public Page<Map<String, Object>> getFilteredPaginatedData(String filter, Pageable pageable) {
        List<Map<String, Object>> filteredRows = getFilteredData(filter);

        // Manual pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredRows.size());

        List<Map<String, Object>> pageContent = (start <= end) ? filteredRows.subList(start, end) : Collections.emptyList();

        return new PageImpl<>(pageContent, pageable, filteredRows.size());
    }

    public void exportFilteredData(String filter, Writer writer) {
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
                        // This catches row-level evaluation errors
                        // For this app, we'll treat row-level errors as "false"
                        return false;
                    }
                })
                .collect(Collectors.toList());
    }
}