package com.example.csvfilter.parser;

import com.example.csvfilter.parser.ast.*;
import com.example.csvfilter.parser.exception.ParsingException;

import java.util.List;
import java.util.Set;

import static com.example.csvfilter.parser.TokenType.*;

// Implements the grammar from Phase 4
public class Parser {
    private final List<Token> tokens;
    private final Set<String> columnNames;
    private int current = 0;

    public Parser(List<Token> tokens, Set<String> columnNames) {
        this.tokens = tokens;
        this.columnNames = columnNames;
    }

    public Expression parse() {
        try {
            return expression();
        } catch (ParsingException e) {
            throw e;
        } catch (Exception e) {
            throw new ParsingException("Invalid filter syntax. " + e.getMessage());
        }
    }

    // expression -> logic_or
    private Expression expression() { return logicOr(); }

    // logic_or   -> logic_and ( "OR" logic_and )*
    private Expression logicOr() {
        Expression expr = logicAnd();
        while (match(OR)) {
            Token operator = previous();
            Expression right = logicAnd();
            expr = new BinaryExpression(expr, operator, right);
        }
        return expr;
    }

    // logic_and  -> comparison ( "AND" comparison )*
    private Expression logicAnd() {
        Expression expr = comparison();
        while (match(AND)) {
            Token operator = previous();
            Expression right = comparison();
            expr = new BinaryExpression(expr, operator, right);
        }
        return expr;
    }

    // comparison -> primary ( ( "==" | "!=" | ... ) primary )*
    private Expression comparison() {
        Expression expr = primary();
        while (match(EQUAL_EQUAL, BANG_EQUAL, GREATER, GREATER_EQUAL, LESS, LESS_EQUAL, CONTAINS, STARTS_WITH, ENDS_WITH)) {
            Token operator = previous();
            Expression right = primary();
            expr = new BinaryExpression(expr, operator, right);
        }
        return expr;
    }

    // primary    -> NUMBER | STRING | "true" | "false" | "null" | IDENTIFIER | "(" expression ")"
    private Expression primary() {
        if (match(FALSE)) return new LiteralExpression(false);
        if (match(TRUE)) return new LiteralExpression(true);
        if (match(NULL)) return new LiteralExpression(null);

        if (match(NUMBER, STRING)) {
            return new LiteralExpression(previous().literal());
        }

        if (match(IDENTIFIER)) {
            Token name = previous();
            // Validate column name *during parsing*
            if (!columnNames.contains(name.lexeme())) {
                throw new ParsingException("Unknown column '" + name.lexeme() + "' at position " + name.position() + ". " +
                        "Available columns: " + columnNames);
            }
            return new VariableExpression(name);
        }

        if (match(LPAREN)) {
            Expression expr = expression();
            consume(RPAREN, "Expected ')' after expression.");
            return new GroupingExpression(expr);
        }

        throw new ParsingException("Expected expression at position " + peek().position() + ", but found '" + peek().lexeme() + "'");
    }

    // Parser helper methods
    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw new ParsingException(message + " at position " + peek().position());
    }

    private boolean check(TokenType type) { return !isAtEnd() && peek().type() == type; }
    private Token advance() { if (!isAtEnd()) current++; return previous(); }
    private boolean isAtEnd() { return peek().type() == EOF; }
    private Token peek() { return tokens.get(current); }
    private Token previous() { return tokens.get(current - 1); }
}