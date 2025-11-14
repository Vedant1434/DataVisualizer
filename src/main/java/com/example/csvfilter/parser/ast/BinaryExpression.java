package com.example.csvfilter.parser.ast;
import com.example.csvfilter.parser.Token;
public record BinaryExpression(Expression left, Token operator, Expression right) implements Expression {
    @Override
    public <R> R accept(Visitor<R> visitor) {
        return visitor.visitBinaryExpr(this);
    }
}