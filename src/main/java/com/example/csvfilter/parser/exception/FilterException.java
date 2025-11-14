package com.example.csvfilter.parser.exception;
// Base exception
public class FilterException extends RuntimeException {
    public FilterException(String message) {
        super(message);
    }
}