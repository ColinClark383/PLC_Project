package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The parser takes the sequence of tokens emitted by the lexer and turns that
 * into a structured representation of the program, called the Abstract Syntax
 * Tree (AST).
 *
 * The parser has a similar architecture to the lexer, just with {@link Token}s
 * instead of characters. As before, {@link #peek(Object...)} and {@link
 * #match(Object...)} are helpers to make the implementation easier.
 *
 * This type of parser is called <em>recursive descent</em>. Each rule in our
 * grammar will have it's own function, and reference to other rules correspond
 * to calling those functions.
 */
public final class Parser {

    private final TokenStream tokens;

    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    /**
     * Parses the {@code source} rule.
     */
    public Ast.Source parseSource() throws ParseException {
        // initialize lists to hold 0 or more fields/methods
        List<Ast.Field> fields = new ArrayList<>();
        List<Ast.Method> methods = new ArrayList<>();

        while (tokens.has(0)) {
            if (peek("LET")) {
                fields.add(parseField());
            } else if (peek("DEF")) {
                methods.add(parseMethod());
            } else {
                throw new ParseException("Unexpected Token at: ", tokens.index);
            }
        }
        return new Ast.Source(fields, methods);
    }

    /**
     * Parses the {@code field} rule. This method should only be called if the
     * next tokens start a field, aka {@code LET}.
     */
    public Ast.Field parseField() throws ParseException {
        // field requires name of identifier, bool for optional const, and optional expression variables
        String name = "";
        boolean isConst = false;
        Ast.Expression expression = null;

        tokens.advance();

        // check for optional const and change bool
        if (peek("CONST")) {
            isConst = true;
            tokens.advance();
        }

        // check for identifier and get its name
        if (!peek(Token.Type.IDENTIFIER)) {
            throw new ParseException("Expected Identifier at: ", tokens.index);
        } else {
            name = tokens.get(0).getLiteral();
            tokens.advance();
        }

        // check for optional '=' and call method to parse expression
        if (peek("=")) {
            tokens.advance();
            expression = parseExpression();
        }

        // check for semicolon at end of tokens
        if (!peek(";")) {
            throw new ParseException("Missing Semicolon at: ", tokens.index);
        } else {
            tokens.advance();
        }

        return new Ast.Field(name, isConst, Optional.ofNullable(expression));
    }

    /**
     * Parses the {@code method} rule. This method should only be called if the
     * next tokens start a method, aka {@code DEF}.
     */
    public Ast.Method parseMethod() throws ParseException {
        // methods requires name of identifier, list of parameters, and list of statements
        String name = "";
        List<String> params = new ArrayList<>();
        List<Ast.Statement> statements = new ArrayList<>();

        tokens.advance();

        // check for identifier
        if (!peek(Token.Type.IDENTIFIER)) {
            throw new ParseException("Expected Identifier at: ", tokens.index);
        } else {
            tokens.advance();
        }

        // check for left parenthesis
        if (!peek("(")) {
            throw new ParseException("Expected Left Parenthesis at: ", tokens.index);
        } else {
            tokens.advance();
        }

        // check for optional function arguments and add to parameters
        if (peek(Token.Type.IDENTIFIER)) {
                params.add(tokens.get(0).getLiteral());
                tokens.advance();
            while (peek(",", Token.Type.IDENTIFIER)) {
                params.add(tokens.get(1).getLiteral());
                tokens.advance();
                tokens.advance();
            }
        }

        // check for right parenthesis
        if (!peek(")")) {
            throw new ParseException("Expected Right Parenthesis at: ", tokens.index);
        } else {
            tokens.advance();
        }

        // check for DO
        if (!peek("DO")) {
            throw new ParseException("Expected DO at: ", tokens.index);
        } else {
            tokens.advance();
        }

        // check for 1 or more statements
        if (tokens.has(1) && (!peek("END"))) {
            statements.add(parseStatement());
            } else {
            throw new ParseException("Expected Statement at: ", tokens.index);
        }

        // check for END at end of tokens
        if (!peek("END")) {
                throw new ParseException("Expected END at: ", tokens.index);
        } else {
            tokens.advance();
        }

        return new Ast.Method(name, params, statements);
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, for, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Statement parseStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Statement.Declaration parseDeclarationStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Statement.If parseIfStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a for statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a for statement, aka
     * {@code FOR}.
     */
    public Ast.Statement.For parseForStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Statement.While parseWhileStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Statement.Return parseReturnStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expression parseExpression() throws ParseException {
        return parseLogicalExpression(); //TODO
    }

    /**
     * Parses the {@code logical-expression} rule.
     */
    public Ast.Expression parseLogicalExpression() throws ParseException {
        return parseEqualityExpression(); //TODO
    }

    /**
     * Parses the {@code equality-expression} rule.
     */
    public Ast.Expression parseEqualityExpression() throws ParseException {
        return parseAdditiveExpression(); //TODO
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expression parseAdditiveExpression() throws ParseException {
        return parseMultiplicativeExpression(); //TODO
    }

    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expression parseMultiplicativeExpression() throws ParseException {
        return parseSecondaryExpression(); //TODO
    }

    /**
     * Parses the {@code secondary-expression} rule.
     */
    public Ast.Expression parseSecondaryExpression() throws ParseException {
        return parsePrimaryExpression(); //TODO
    }

    /**
     * Parses the {@code primary-expression} rule. This is the top-level rule
     * for expressions and includes literal values, grouping, variables, and
     * functions. It may be helpful to break these up into other methods but is
     * not strictly necessary.
     */
    public Ast.Expression parsePrimaryExpression() throws ParseException {
        if (match("NIL")){
            return new Ast.Expression.Literal(null);
        }
        else if (match("TRUE")) {
            return new Ast.Expression.Literal(Boolean.TRUE);
        }
        else if(match("FALSE")){
            return new Ast.Expression.Literal(Boolean.FALSE);
        }
        else if(peek(Token.Type.INTEGER)){
            BigInteger val = new BigInteger(tokens.get(0).getLiteral());
            tokens.advance();
            return new Ast.Expression.Literal(val);
        }
        else if(peek(Token.Type.DECIMAL)){
            BigDecimal val = new BigDecimal(tokens.get(0).getLiteral());
            tokens.advance();
            return new Ast.Expression.Literal(val);
        }
        else if(peek(Token.Type.CHARACTER)){
            String quoted = new String(tokens.get(0).getLiteral());
            quoted = quoted.replace("'", "");
            Character val = quoted.charAt(0);
            tokens.advance();
            return new Ast.Expression.Literal(val);
        }
        else if (peek(Token.Type.STRING)) {
            String quoted = new String(tokens.get(0).getLiteral());
            quoted = quoted.replace("\"", "");
            tokens.advance();
            return new Ast.Expression.Literal(quoted);
        }
        else if (match("(")) {
            Ast.Expression group = parseExpression();
            if(!match(")")){
                throw new ParseException("Expected ) at:", tokens.index);
            }
            return new Ast.Expression.Group(group);
        }
        else if (peek(Token.Type.IDENTIFIER)) {
            Ast.Expression.Literal identifier = new Ast.Expression.Literal(tokens.get(0));
            tokens.advance();
            if(!match("(")){
                return identifier;
            }
            List<Ast.Expression> params = new ArrayList<>();
            while (tokens.has(0) && !peek(")")){
                params.add(parseExpression());
                if(!peek(")") && !match(",")){
                    throw new ParseException("Expected comma or end of function at: ", tokens.index);
                }
            }
            if(peek(")")){
                tokens.advance();
                return new Ast.Expression.Function(Optional.empty(), identifier.getLiteral().toString(), params);
            }

            throw new ParseException("Expected end of function ) at:", tokens.index);

        }

        throw new ParseException("Invalid Expression at:", tokens.index); //TODO
    }

    /**
     * As in the lexer, returns {@code true} if the current sequence of tokens
     * matches the given patterns. Unlike the lexer, the pattern is not a regex;
     * instead it is either a {@link Token.Type}, which matches if the token's
     * type is the same, or a {@link String}, which matches if the token's
     * literal is the same.
     *
     * In other words, {@code Token(IDENTIFIER, "literal")} is matched by both
     * {@code peek(Token.Type.IDENTIFIER)} and {@code peek("literal")}.
     */
    private boolean peek(Object... patterns) {
        for (int i = 0; i < patterns.length; i++) {
            if (!tokens.has(i)) {
                return false;
            } else if (patterns[i] instanceof Token.Type) {
                if (patterns[i] != tokens.get(i).getType()) {
                    return false;
                }
            } else if (patterns[i] instanceof String) {
                if (!patterns[i].equals(tokens.get(i).getLiteral())) {
                    return false;
                }
            } else {
                throw new AssertionError("Invalid pattern object: " + patterns[i].getClass());
            }
        }
        return true;
    }

    /**
     * As in the lexer, returns {@code true} if {@link #peek(Object...)} is true
     * and advances the token stream.
     */
    private boolean match(Object... patterns) {
        boolean peek = peek(patterns);
        if (peek) {
            for (int i = 0; i < patterns.length; i++) {
                tokens.advance();
            }
        }
        return peek;
    }

    private static final class TokenStream {

        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * Returns true if there is a token at index + offset.
         */
        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        /**
         * Gets the token at index + offset.
         */
        public Token get(int offset) {
            return tokens.get(index + offset);
        }

        /**
         * Advances to the next token, incrementing the index.
         */
        public void advance() {
            index++;
        }

    }

}
