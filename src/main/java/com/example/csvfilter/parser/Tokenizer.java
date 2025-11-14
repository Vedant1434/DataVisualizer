package com.example.csvfilter.parser;

import com.example.csvfilter.parser.exception.ParsingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Tokenizer {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int start = 0;
    private int current = 0;

    private static final Map<String, TokenType> keywords;
    static {
        keywords = new HashMap<>();
        keywords.put("and", TokenType.AND);
        keywords.put("or", TokenType.OR);
        keywords.put("true", TokenType.TRUE);
        keywords.put("false", TokenType.FALSE);
        keywords.put("null", TokenType.NULL);
        keywords.put("contains", TokenType.CONTAINS);
        keywords.put("startsWith", TokenType.STARTS_WITH);
        keywords.put("endsWith", TokenType.ENDS_WITH);
    }

    public Tokenizer(String source) {
        this.source = source;
    }

    public List<Token> tokenize() {
        while (!isAtEnd()) {
            start = current;
            scanToken();
        }
        tokens.add(new Token(TokenType.EOF, "", null, current));
        return tokens;
    }

    private void scanToken() {
        char c = advance();
        switch (c) {
            case '(': addToken(TokenType.LPAREN); break;
            case ')': addToken(TokenType.RPAREN); break;
            case '=': addToken(match('=') ? TokenType.EQUAL_EQUAL : TokenType.EQUAL_EQUAL); break; // Tolerate single =
            case '!': addToken(match('=') ? TokenType.BANG_EQUAL : TokenType.BANG_EQUAL); break; // Tolerate !
            case '<': addToken(match('=') ? TokenType.LESS_EQUAL : TokenType.LESS); break;
            case '>': addToken(match('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER); break;
            case '"': string(); break;
            case ' ', '\r', '\t', '\n': break; // Ignore whitespace
            default:
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                    identifier();
                } else {
                    throw new ParsingException("Unexpected character '" + c + "' at position " + current);
                }
        }
    }

    private void identifier() {
        while (isAlphaNumeric(peek())) advance();
        String text = source.substring(start, current);
        TokenType type = keywords.get(text.toLowerCase()); // Case-insensitive keywords
        if (type == null) type = TokenType.IDENTIFIER;
        addToken(type);
    }

    private void number() {
        while (isDigit(peek())) advance();
        if (peek() == '.' && isDigit(peekNext())) {
            advance(); // Consume the "."
            while (isDigit(peek())) advance();
        }
        addToken(TokenType.NUMBER, Double.parseDouble(source.substring(start, current)));
    }

    private void string() {
        while (peek() != '"' && !isAtEnd()) {
            advance();
        }
        if (isAtEnd()) {
            throw new ParsingException("Unterminated string at position " + start);
        }
        advance(); // The closing "
        String value = source.substring(start + 1, current - 1);
        addToken(TokenType.STRING, value);
    }

    private boolean match(char expected) {
        if (isAtEnd() || source.charAt(current) != expected) return false;
        current++;
        return true;
    }

    private char advance() { return source.charAt(current++); }
    private char peek() { return isAtEnd() ? '\0' : source.charAt(current); }
    private char peekNext() { return (current + 1 >= source.length()) ? '\0' : source.charAt(current + 1); }
    private boolean isAtEnd() { return current >= source.length(); }
    private boolean isDigit(char c) { return c >= '0' && c <= '9'; }
    private boolean isAlpha(char c) { return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_'; }
    private boolean isAlphaNumeric(char c) { return isAlpha(c) || isDigit(c); }
    private void addToken(TokenType type) { addToken(type, null); }
    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, start));
    }
}