package com.example.csvfilter.parser;

public enum TokenType {
    // Single-character tokens
    LPAREN, RPAREN,

    // Operators
    EQUAL_EQUAL, BANG_EQUAL,
    GREATER, GREATER_EQUAL,
    LESS, LESS_EQUAL,

    // Literals
    IDENTIFIER, STRING, NUMBER,

    // Keywords
    AND, OR, TRUE, FALSE, NULL,
    CONTAINS, STARTS_WITH, ENDS_WITH,

    EOF
}