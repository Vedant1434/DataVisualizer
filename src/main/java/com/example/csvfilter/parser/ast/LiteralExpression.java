package com.example.csvfilter.parser.ast;
public record LiteralExpression(Object value) implements Expression {
    @Override
    public <R> R accept(Visitor<R> visitor) {
        return visitor.visitLiteralExpr(this);
    }
}