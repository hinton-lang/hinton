package org.hinton_lang.Lexer;

import org.hinton_lang.Tokens.Token;
import org.hinton_lang.Tokens.TokenType;



public class LogicalOrBitwiseOperator {

    /**
     * Adds a logical or bitwise operator to the tokens list.
     * @param lexer The lexer.
     * @param op The current character (operator).
     */
    public static void add(Lexer lexer, char op) {
        if (op == '>') { foundGreaterThan(lexer); }
        else if (op == '<') { foundLessThan(lexer); }
        else if (op == '=') { foundEqualsSign(lexer); }
        else if (op == '!') { foundExclamation(lexer); }
        else if (op == '&') { foundAmpersand(lexer); }
        else if (op == '|') { foundOr(lexer); }
        else if (op == '~' || op == '^') { foundBitwiseNot_Xor(lexer, op); };
    }


    /**
     * Adds a logical greater-than-or-equals (>=), a bitwise shift-right (>>) or a logical greater-than operator.
     * @param lexer The lexer.
     */
    private static void foundGreaterThan(Lexer lexer) {
        int ln = lexer.currentLineNum;
        Token currentToken;

        if (lexer.hasNextChar() && lexer.nextChar() == '=') {
             currentToken = new Token(TokenType.LOGICAL_GREATER_THAN_EQUALS, ln, lexer.currentCharPos, ">=", null);
            lexer.currentCharPos++;
        } else if (lexer.hasNextChar() && lexer.nextChar() == '>') {
            currentToken = new Token(TokenType.BITWISE_SHIFT_RIGHT, ln, lexer.currentCharPos, ">>", null);
            lexer.currentCharPos++;
        } else {
            currentToken = new Token(TokenType.LOGICAL_GREATER_THAN, ln, lexer.currentCharPos, ">", null);
        }

        lexer.TokensList.add(currentToken);
    }


    /**
     * Adds a logical less-than-or-equals (<=), a bitwise shift-left (<<), or a logical less-than (<) operator.
     * @param lexer The lexer.
     */
    private static void foundLessThan(Lexer lexer) {
        int ln = lexer.currentLineNum;
        Token currentToken;

        if (lexer.hasNextChar() && lexer.nextChar() == '=') {
            currentToken = new Token(TokenType.LOGICAL_LESS_THAN_EQUALS, ln, lexer.currentCharPos, "<=", null);
            lexer.currentCharPos++;
        } else if (lexer.hasNextChar() && lexer.nextChar() == '<') {
            currentToken = new Token(TokenType.BITWISE_SHIFT_LEFT, ln, lexer.currentCharPos, "<<", null);
            lexer.currentCharPos++;
        } else {
            currentToken = new Token(TokenType.LOGICAL_LESS_THAN, ln, lexer.currentCharPos, "<", null);
        }

        lexer.TokensList.add(currentToken);
    }


    /**
     * Adds an equals sign (=) or a logical equals (==) operator.
     * @param lexer The lexer.
     */
    private static void foundEqualsSign(Lexer lexer) {
        int ln = lexer.currentLineNum;
        Token currentToken;

        if (lexer.hasNextChar() && lexer.nextChar() == '=') {
            currentToken = new Token(TokenType.LOGICAL_EQUALS, ln, lexer.currentCharPos, "==", null);
            lexer.currentCharPos++;
        } else {
            currentToken = new Token(TokenType.EQUALS_SIGN, ln, lexer.currentCharPos, "=", null);
        }

        lexer.TokensList.add(currentToken);
    }


    /**
     * Adds a logical not-equals (!=) or a logical not (!) operator.
     * @param lexer The lexer.
     */
    private static void foundExclamation(Lexer lexer) {
        int ln = lexer.currentLineNum;
        Token currentToken;

        if (lexer.hasNextChar() && lexer.nextChar() == '=') {
            currentToken = new Token(TokenType.LOGICAL_NOT_EQUALS, ln, lexer.currentCharPos, "!=", null);
            lexer.currentCharPos++;
        } else {
            currentToken = new Token(TokenType.LOGICAL_NOT, ln, lexer.currentCharPos, "!", null);
        }

        lexer.TokensList.add(currentToken);
    }


    /**
     * Adds a logical and (&&) or a bitwise and (&) operator.
     * @param lexer The lexer.
     */
    private static void foundAmpersand(Lexer lexer) {
        int ln = lexer.currentLineNum;
        Token currentToken;

        if (lexer.hasNextChar() && lexer.nextChar() == '&') {
            currentToken = new Token(TokenType.LOGICAL_AND, ln, lexer.currentCharPos, "&&", null);
            lexer.currentCharPos++;
        } else {
            currentToken = new Token(TokenType.BITWISE_AND, ln, lexer.currentCharPos, "&", null);
        }

        lexer.TokensList.add(currentToken);
    }


    /**
     * Adds a logical or (||) or a bitwise or (|) operator.
     * @param lexer The lexer.
     */
    private static void foundOr(Lexer lexer) {
        int ln = lexer.currentLineNum;
        Token currentToken;

        if (lexer.hasNextChar() && lexer.nextChar() == '|') {
            currentToken = new Token(TokenType.LOGICAL_OR, ln, lexer.currentCharPos, "||", null);
            lexer.currentCharPos++;
        } else {
            currentToken = new Token(TokenType.BITWISE_OR, ln, lexer.currentCharPos, "|", null);
        }

        lexer.TokensList.add(currentToken);
    }


    /**
     * Adds a bitwise not (~) or a bitwise xor (^) operator.
     * @param lexer The lexer.
     * @param op The current character (operator).
     */
    private static void foundBitwiseNot_Xor(Lexer lexer, char op) {
        int ln = lexer.currentLineNum;
        Token currentToken;

        if (op == '~') {
            currentToken = new Token(TokenType.BITWISE_NOT, ln, lexer.currentCharPos, "~", null);
        } else {
            currentToken = new Token(TokenType.BITWISE_XOR, ln, lexer.currentCharPos, "^", null);
        }

        lexer.TokensList.add(currentToken);
    }
}
