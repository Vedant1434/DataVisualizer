package com.example.csvfilter.parser.ast;

// Base interface for all AST nodes
public interface Expression {
    <R> R accept(Visitor<R> visitor);

    interface Visitor<R> {
        R visitBinaryExpr(BinaryExpression expr);
        R visitGroupingExpr(GroupingExpression expr);
        R visitLiteralExpr(LiteralExpression expr);
        R visitVariableExpr(VariableExpression expr);
    }
}