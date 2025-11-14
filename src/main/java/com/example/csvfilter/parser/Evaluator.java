package com.example.csvfilter.parser;

import com.example.csvfilter.parser.ast.*;
import com.example.csvfilter.parser.exception.EvaluationException;

import java.util.Map;

public class Evaluator implements Expression.Visitor<Object> {

    @SuppressWarnings("unused")
    private final Map<String, Class<?>> schema; // Reserved for future type validation
    private Map<String, Object> currentRow;

    public Evaluator(Map<String, Class<?>> schema) {
        this.schema = schema;
    }

    public boolean evaluate(Expression expr, Map<String, Object> row) {
        this.currentRow = row;
        try {
            Object result = evaluateExpression(expr);
            return isTruthy(result);
        } catch (EvaluationException e) {
            // Rethrow exceptions from the evaluator
            throw e;
        } catch (Exception e) {
            // Catch any other runtime errors (e.g. ClassCastException)
            throw new EvaluationException("Error during evaluation: " + e.getMessage());
        }
    }

    private Object evaluateExpression(Expression expr) {
        return expr.accept(this);
    }

    @Override
    public Object visitBinaryExpr(BinaryExpression expr) {
        Object left = evaluateExpression(expr.left());
        Object right = evaluateExpression(expr.right());

        switch (expr.operator().type()) {
            case AND: return isTruthy(left) && isTruthy(right);
            case OR: return isTruthy(left) || isTruthy(right);
            case EQUAL_EQUAL: return isEqual(left, right);
            case BANG_EQUAL: return !isEqual(left, right);
            case GREATER: return compare(left, right) > 0;
            case GREATER_EQUAL: return compare(left, right) >= 0;
            case LESS: return compare(left, right) < 0;
            case LESS_EQUAL: return compare(left, right) <= 0;
            case CONTAINS: return checkStringOp(left, right, String::contains);
            case STARTS_WITH: return checkStringOp(left, right, String::startsWith);
            case ENDS_WITH: return checkStringOp(left, right, String::endsWith);
            default:
                throw new EvaluationException("Unexpected operator: " + expr.operator().type());
        }
    }

    @Override
    public Object visitGroupingExpr(GroupingExpression expr) {
        return evaluateExpression(expr.expression());
    }

    @Override
    public Object visitLiteralExpr(LiteralExpression expr) {
        return expr.value();
    }

    @Override
    public Object visitVariableExpr(VariableExpression expr) {
        return currentRow.get(expr.name().lexeme());
    }

    // --- Evaluation Helpers ---

    private boolean isTruthy(Object object) {
        if (object == null) return false;
        if (object instanceof Boolean) return (boolean) object;
        return true;
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;

        // Coerce types for comparison
        if (a instanceof Number && b instanceof Number) {
            return ((Number) a).doubleValue() == ((Number) b).doubleValue();
        }

        // --- MODIFIED: Case-insensitive string comparison ---
        if (a instanceof String && b instanceof String) {
            return ((String) a).equalsIgnoreCase((String) b);
        }

        return a.equals(b);
    }

    private int compare(Object left, Object right) {
        // Handle nulls: nulls are always "less"
        if (left == null && right == null) return 0;
        if (left == null) return -1;
        if (right == null) return 1;

        if (left instanceof Number && right instanceof Number) {
            return Double.compare(((Number) left).doubleValue(), ((Number) right).doubleValue());
        }

        // --- MODIFIED: Case-insensitive string comparison ---
        if (left instanceof String && right instanceof String) {
            return ((String) left).compareToIgnoreCase((String) right);
        }

        throw new EvaluationException("Cannot compare " + left.getClass().getSimpleName() + " with " + right.getClass().getSimpleName());
    }

    private boolean checkStringOp(Object left, Object right, java.util.function.BiPredicate<String, String> op) {
        if (left == null) return false; // "null" cannot contain anything
        if (!(left instanceof String) || !(right instanceof String)) {
            throw new EvaluationException("String operation (contains, startsWith, endsWith) can only be used on strings.");
        }

        // --- MODIFIED: Case-insensitive operations ---
        String leftStr = ((String) left).toLowerCase();
        String rightStr = ((String) right).toLowerCase();

        return op.test(leftStr, rightStr);
    }
}