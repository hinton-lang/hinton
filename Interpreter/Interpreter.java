package org.hinton_lang.Interpreter;

import java.util.List;
import java.util.ArrayList;

import org.hinton_lang.Parser.AST.*;
import org.hinton_lang.Hinton;
import org.hinton_lang.Errors.RuntimeError;
import org.hinton_lang.Envornment.DecType;
import org.hinton_lang.Envornment.Environment;
import org.hinton_lang.Parser.AST.Stmt;
import org.hinton_lang.RuntimeLib.RuntimeLib;
import org.hinton_lang.Tokens.TokenType;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
    // Holds global functions and variables native to Hinton.
    public final Environment globals = new Environment();
    // Used to store variables
    public Environment environment = globals;

    public Interpreter() {
        // Attaches the native functions to the global scope
        RuntimeLib.nativeFunctions.forEach((fn) -> {
            globals.defineBuiltIn(fn.getFuncName(), fn.getFunc(), DecType.HINTON_FUNCTION);
        });
    }

    /**
     * Executes the given list of statements (program).
     * 
     * @param statements The list of statements that make up the program.
     */
    public void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            Hinton.runtimeError(error);
        }
    }

    /**
     * Computes the boolean value of the provided object.
     * 
     * @param object The object whose boolean value will be computed.
     * @return The boolean value of the provided object.
     */
    public static boolean isTruthy(Object object) {
        if (object == null)
            return false;
        if (object instanceof Integer && (int) object == 0)
            return false;
        if (object instanceof Double && (int) object == 0.0)
            return false;
        if (object instanceof Boolean)
            return (boolean) object;
        return true;
    }

    /**
     * Visits a literal expression.
     */
    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    /**
     * Visits a logical expression.
     */
    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.left);

        if (expr.operator.type == TokenType.LOGICAL_OR) {
            if (isTruthy(left))
                return left;
        } else {
            if (!isTruthy(left))
                return left;
        }

        return evaluate(expr.right);
    }

    /**
     * Visits a parenthesized expression.
     */
    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    /**
     * Evaluates the given expression.
     * 
     * @param expr The expression to be evaluated.
     * @return The literal value obtained from the expression.
     */
    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    /**
     * Executes the given statement.
     * 
     * @param stmt The statement to execute.
     */
    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    /**
     * Visits a block statement.
     */
    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    /**
     * Executes the contents of a block statement.
     * 
     * @param statements  The statements contained within the block.
     * @param environment The new environment for this block.
     */
    public void executeBlock(List<Stmt> statements, Environment environment) {
        Environment previous = this.environment;
        try {
            this.environment = environment;

            for (Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            this.environment = previous;
        }
    }

    /**
     * Visits an expression statement.
     * 
     * @param stmt The statement to visit.
     * @return VOID.
     */
    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    /**
     * Visits a function declaration statement.
     */
    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        HintonFunction function = new HintonFunction(stmt, environment);
        environment.define(stmt.name, function, DecType.FUNCTION);
        return null;
    }

    /**
     * Visits a break statement.
     */
    @Override
    public Void visitBreakStmt(Stmt.Break stmt) throws Break {
        // We use a throw-statement to trace back all the
        // way to where the loop's body was executed.
        throw new Break();
    }

    /**
     * Visits a continue statement.
     */
    @Override
    public Void visitContinueStmt(Stmt.Continue stmt) throws Continue {
        // We use a throw-statement to trace back all the
        // way to where the loop's body was executed.
        throw new Continue();
    }

    /**
     * Visits a function declaration.
     */
    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        Object value = null;
        if (stmt.value != null)
            value = evaluate(stmt.value);

        // We use a throw-statement to trace back all the
        // way to where the function's body was executed.
        throw new Return(value);
    }

    /**
     * Visits an if statement.
     */
    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch);
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch);
        }
        return null;
    }

    /**
     * Visits a variable statement.
     */
    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        Object value = null;
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        }

        environment.define(stmt.name, value, DecType.VARIABLE);
        return null;
    }

    /**
     * Visits a while statement.
     */
    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        while (isTruthy(evaluate(stmt.condition))) {
            try {
                execute(stmt.body);
            } catch (Continue c) {
                continue;
            } catch (Break b) {
                break;
            }
        }
        return null;
    }

    /**
     * Visits a constant statement.
     */
    @Override
    public Void visitConstStmt(Stmt.Const stmt) {
        Object value = evaluate(stmt.initializer);

        environment.define(stmt.name, value, DecType.CONSTANT);
        return null;
    }

    /**
     * Visits an assignment expression.
     */
    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);
        environment.assign(expr.name, value);
        return value;
    }

    /**
     * Visits a lambda function expression.
     */
    @Override
    public Object visitLambdaExpr(Expr.Lambda expr) {
        return new HintonLambda(expr, environment);
    }

    /**
     * Evaluates a unary expression.
     */
    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case LOGICAL_NOT:
                return EvalUnaryExpr.evalLogicNegation(right);
            case MINUS:
                return EvalUnaryExpr.evalNumericNegation(expr.operator, right);
            default:
                break;
        }

        // Unreachable.
        return null;
    }

    /**
     * Visits a variable expression.
     */
    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return environment.get(expr.name);
    }

    /**
     * Visits a function call expression.
     */
    @Override
    public Object visitCallExpr(Expr.Call expr) {
        Object callee = evaluate(expr.callee);

        List<Object> arguments = new ArrayList<>();
        for (Expr argument : expr.arguments) {
            arguments.add(evaluate(argument));
        }

        if (!(callee instanceof HintonCallable)) {
            throw new RuntimeError(expr.paren, "Can only call functions and classes.");
        }

        HintonCallable function = (HintonCallable) callee;

        if (arguments.size() != function.arity()) {
            throw new RuntimeError(expr.paren,
                    "Expected " + function.arity() + " arguments but got " + arguments.size() + ".");
        }

        return function.call(this, arguments);
    }

    /**
     * Visits an array expression.
     */
    @Override
    public ArrayList<Expr> visitArrayExpr(Expr.Array expr) {
        ArrayList<Expr> ar = new ArrayList<>();

        for (int i = 0; i < expr.expressions.size(); i++) {
            ar.add(expr.expressions.get(i));
        }

        return ar;
    }

    /**
     * Visits an array indexing expression.
     */
    @Override
    public Object visitArrayIndexingExpr(Expr.ArrayIndexing expr) {
        Object arr = evaluate(expr.arr);
        Object index = evaluate(expr.index);

        if (arr instanceof ArrayList) {
            ArrayList<Expr> arr1 = (ArrayList<Expr>) arr;

            int idx;
            if (index instanceof Integer) {
                idx = (int) index;
            } else {
                // TODO: Throw RuntimeError instead
                throw new Error("CAN ONLY USE INTEGER FOR ARRAY INDEX.");
            }

            // Support for negative indexing
            if (idx < 0)
                idx = arr1.size() + idx;

            // If even after adjusting for negative index the provided
            // index is out of range, we throw an error.
            if (idx < 0 || idx > (arr1.size() - 1)) {
                // TODO: Throw RuntimeError instead
                throw new Error("LIST INDEX OUT OF RANGE.");
            }

            return evaluate(arr1.get(idx));
        } else {
            // TODO: Throw RuntimeError instead
            throw new Error("INDEX ARRAY ONLY!");
        }
    }

    /**
     * Evaluates a binary expression.
     */
    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case MINUS:
                return EvalBinaryExpr.evalSubtraction(expr.operator, left, right);
            case DIV:
                return EvalBinaryExpr.evalDivision(expr.operator, left, right);
            case MULT:
                return EvalBinaryExpr.evalMultiplication(expr.operator, left, right);
            case PLUS:
                return EvalBinaryExpr.evalAddition(expr.operator, left, right);
            case MOD:
                return EvalBinaryExpr.evalModulus(expr.operator, left, right);
            case EXPO:
                return EvalBinaryExpr.evalExponent(expr.operator, left, right);
            case LOGICAL_OR:
                return (boolean) left || (boolean) right;
            case LOGICAL_AND:
                return (boolean) left && (boolean) right;
            case GREATER_THAN:
                return EvalBinaryExpr.evalGreaterThan(expr.operator, left, right);
            case GREATER_THAN_EQ:
                return EvalBinaryExpr.evalGreaterThanEqual(expr.operator, left, right);
            case LESS_THAN:
                return EvalBinaryExpr.evalLessThan(expr.operator, left, right);
            case LESS_THAN_EQ:
                return EvalBinaryExpr.evalLessThanEqual(expr.operator, left, right);
            case LOGICAL_EQ:
                return EvalBinaryExpr.evalEquals(expr.operator, left, right);
            case LOGICAL_NOT_EQ:
                return EvalBinaryExpr.evalNotEquals(expr.operator, left, right);
            default:
                break;
        }

        // Unreachable.
        return null;
    }
}
