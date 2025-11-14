package com.example.csvfilter.parser.ast;
import com.example.csvfilter.parser.Token;
public record VariableExpression(Token name) implements Expression {
    @Override
    public <R> R accept(Visitor<R> visitor) {
        return visitor.visitVariableExpr(this);
    }
}