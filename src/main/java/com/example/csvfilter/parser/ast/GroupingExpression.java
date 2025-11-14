package com.example.csvfilter.parser.ast;
public record GroupingExpression(Expression expression) implements Expression {
    @Override
    public <R> R accept(Visitor<R> visitor) {
        return visitor.visitGroupingExpr(this);
    }
}