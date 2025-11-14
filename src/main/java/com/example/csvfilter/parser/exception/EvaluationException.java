package com.example.csvfilter.parser.exception;
// For runtime/type errors
public class EvaluationException extends FilterException {
    public EvaluationException(String message) {
        super(message);
    }
}