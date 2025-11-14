package com.example.csvfilter.model;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@SessionScope
public class UserSessionData implements Serializable {
    private List<Map<String, Object>> allRows;
    private List<String> headers;
    private Map<String, Class<?>> schema;

    public void setData(List<Map<String, Object>> allRows, List<String> headers, Map<String, Class<?>> schema) {
        this.allRows = allRows;
        this.headers = headers;
        this.schema = schema;
    }

    public List<Map<String, Object>> getAllRows() {
        return allRows;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public Map<String, Class<?>> getSchema() {
        return schema;
    }

    public Set<String> getColumnNames() {
        return schema != null ? schema.keySet() : Set.of();
    }

    public boolean hasData() {
        return allRows != null && !allRows.isEmpty();
    }
}