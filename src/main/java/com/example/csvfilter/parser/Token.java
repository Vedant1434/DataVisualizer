package com.example.csvfilter.parser;

public record Token(TokenType type, String lexeme, Object literal, int position) {
    @Override
    public String toString() {
        return type + " " + lexeme + " " + literal;
    }
}