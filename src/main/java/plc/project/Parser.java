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
                throw new ParseException("Unexpected Token at: ", getIndex());
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
            throw new ParseException("Expected Identifier at: ", getIndex());
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
        if (!match(";")) {
            throw new ParseException("Expected Semicolon at: ", getIndex());
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
            throw new ParseException("Expected Identifier at: ", getIndex());
        }
        name = tokens.get(0).getLiteral();
        tokens.advance();

        // check for left parenthesis
        if (!match("(")) {
            throw new ParseException("Expected ( at: ", getIndex());
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
        if (!match(")")) {
            throw new ParseException("Expected ) at: ", getIndex());
        }

        // check for DO
        if (!match("DO")) {
            throw new ParseException("Expected DO at: ", getIndex());
        }

        // check for 1 or more statements
        while(!peek("END")) {
            statements.add(parseStatement());
        }

        // check for END at end of tokens
        if (!match("END")) {
                throw new ParseException("Expected END at: ", getIndex());
        }

        return new Ast.Method(name, params, statements);
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, for, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Statement parseStatement() throws ParseException {
        // check all statements and parse correctly
        if (tokens.has(0)) {
            if (match("LET")) {
                return parseDeclarationStatement();
            } else if (match("IF")) {
                return parseIfStatement();
            } else if (match("FOR")) {
                return parseForStatement();
            } else if (match("WHILE")) {
                return parseWhileStatement();
            } else if (match("RETURN")) {
                return parseReturnStatement();
            } else {
                // handles expression and/or assignment
                Ast.Expression expression = null;
                expression = parseExpression();

                // checks for optional assignment
                if (match("=")) {
                    Ast.Expression value = parseExpression();
                    if (!match(";")) {
                        throw new ParseException("Expected Semicolon at: ", getIndex());
                    }
                    return new Ast.Statement.Assignment(expression, value);
                }
                if (!match(";")) {
                    throw new ParseException("Expected Semicolon at: ", getIndex());
                }
                return new Ast.Statement.Expression(expression);
            }
        }
        throw new ParseException("Expected Statement at: ", getIndex());
    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Statement.Declaration parseDeclarationStatement() throws ParseException {
        // Declaration requires name of identifier and optional expression
        String name = "";
        Ast.Expression expression = null;
        // check for identifier
        if (!peek(Token.Type.IDENTIFIER)) {
            throw new ParseException("Expected Identifier at: ", getIndex());
        } else {
            name = tokens.get(0).getLiteral();
            tokens.advance();
        }

        // check for optional '=' and expression
        if (tokens.has(1) && peek("=")) {
            tokens.advance();
            expression = parseExpression();
        }

        // check for semicolon at end of tokens
        if (!match(";")) {
            throw new ParseException("Expected Semicolon at: ", getIndex());
        }

        return new Ast.Statement.Declaration(name, Optional.ofNullable(expression));
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Statement.If parseIfStatement() throws ParseException {
        // If requires condition, thenStatements, and elseStatements
        Ast.Expression condition = null;
        List<Ast.Statement> thenStatement = new ArrayList<>();
        List<Ast.Statement> elseStatement = new ArrayList<>();

        // check for expression
        if(tokens.has(0)) {
            condition = parseExpression();
        }

        // check for "DO"
        if (!match("DO")) {
            throw new ParseException("Expected DO at: ", getIndex());
        }

        // check for 1 or more statements
        while (!peek("ELSE") && !peek("END")) {
            thenStatement.add(parseStatement());
        }

        // check for optional "ELSE" and statements
        if (tokens.has(1) && match("ELSE")) {
            while (!peek("END")) {
                elseStatement.add(parseStatement());
            }
        }

        // check for END at end of tokens
        if (!match("END")) {
            throw new ParseException("Expected END at: ", getIndex());
        }

        return new Ast.Statement.If(condition, thenStatement, elseStatement);
    }

    /**
     * Parses a for statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a for statement, aka
     * {@code FOR}.
     */
    public Ast.Statement.For parseForStatement() throws ParseException {
        // FOR requires initialization, condition, increment, and statements
        Ast.Statement initial = null;
        Ast.Expression condition = null;
        Ast.Statement increment = null;
        List<Ast.Statement> statements = new ArrayList<>();

        // check for left parenthesis
        if (!match("(")) {
            throw new ParseException("Expected ( at: ", getIndex());
        }

        // check for optional initialization statement
        if (peek(Token.Type.IDENTIFIER, "=")) {
            String name = tokens.get(0).getLiteral();
            tokens.advance();
            tokens.advance();
            Ast.Expression value = parseExpression();
            initial = new Ast.Statement.Declaration(name, Optional.ofNullable(value));
        }

        // check for semicolon
        if (!match(";")) {
            throw new ParseException("Expected Semicolon at: ", getIndex());
        }
        condition = parseExpression();

        // check for semicolon
        if (!match(";")) {
            throw new ParseException("Expected Semicolon at: ", getIndex());
        }

        // check for optional increment statement
        if (peek(Token.Type.IDENTIFIER, "=")) {
            Ast.Expression rec = parseExpression();
            tokens.advance();
            Ast.Expression value = parseExpression();
            increment = new Ast.Statement.Assignment(rec, value);
        }

        // check for right parenthesis
        if (!match(")")) {
            throw new ParseException("Expected ) at: ", getIndex());
        }

        // check for 1 or more statements
        while (!peek("END")) {
            statements.add(parseStatement());
        }

        // check for END at end of tokens
        if (!match("END")) {
            throw new ParseException("Expected END at: ", getIndex());
        }

        return new Ast.Statement.For(initial, condition, increment, statements);
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Statement.While parseWhileStatement() throws ParseException {
        // WHILE requires condition and statements
        Ast.Expression condition = null;
        List<Ast.Statement> statements = new ArrayList<>();

        // check for expression
        if (tokens.has(0)) {
            condition = parseExpression();
        }

        // check for DO
        if (!match("DO")) {
            throw new ParseException("Expected DO at: ", getIndex());
        }

        // check for 1 or more statements
        while (!peek("END")) {
            statements.add(parseStatement());
        }

        // check for END at end of tokens
        if (!match("END")) {
            throw new ParseException("Expected END at: ", getIndex());
        }

        return new Ast.Statement.While(condition, statements);
    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Statement.Return parseReturnStatement() throws ParseException {
        // RETURN requires expression value
        Ast.Expression value = null;

        // check for expression
        if (tokens.has(0)) {
            value = parseExpression();
        }

        // check for semicolon at end of tokens
        if (!match(";")) {
            throw new ParseException("Expected Semicolon at: ", getIndex());
        }

        return new Ast.Statement.Return(value);
    }

    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expression parseExpression() throws ParseException {
        return parseLogicalExpression();
    }

    /**
     * Parses the {@code logical-expression} rule.
     */
    public Ast.Expression parseLogicalExpression() throws ParseException {
        Ast.Expression log = parseEqualityExpression();
        while(peek("||") || peek("&&")){
            String op = tokens.get(0).getLiteral();
            tokens.advance();
            Ast.Expression.Binary temp = new Ast.Expression.Binary(op, log, parseEqualityExpression());
            log = temp;
        }
        return log;
    }

    /**
     * Parses the {@code equality-expression} rule.
     */
    public Ast.Expression parseEqualityExpression() throws ParseException {
        Ast.Expression eq = parseAdditiveExpression();
        while(peek("!=") || peek("==") || peek(">=")||peek(">")||peek("<=")||peek("<")){
            String op = tokens.get(0).getLiteral();
            tokens.advance();
            Ast.Expression.Binary temp = new Ast.Expression.Binary(op, eq, parseAdditiveExpression());
            eq = temp;
        }
        return eq;
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expression parseAdditiveExpression() throws ParseException {
        Ast.Expression add = parseMultiplicativeExpression();
        while(peek("+") || peek("-")){
            String op = tokens.get(0).getLiteral();
            tokens.advance();
            Ast.Expression.Binary temp = new Ast.Expression.Binary(op, add, parseMultiplicativeExpression());
            add = temp;
        }
        return add;
    }

    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expression parseMultiplicativeExpression() throws ParseException {
        Ast.Expression Mul = parseSecondaryExpression();
        while(peek("*") || peek("/")){
            String op = tokens.get(0).getLiteral();
            tokens.advance();
            Ast.Expression.Binary temp = new Ast.Expression.Binary(op, Mul, parseSecondaryExpression());
            Mul = temp;
        }
        return Mul;
    }

    /**
     * Parses the {@code secondary-expression} rule.
     */
    public Ast.Expression parseSecondaryExpression() throws ParseException {
        Ast.Expression first = parsePrimaryExpression();
        Token right;
        while(Boolean.TRUE){
            if(!peek(".")){
                //must start with a period to extend expression
                return first;
            }
            tokens.advance();
            if(!peek(Token.Type.IDENTIFIER)){
                //must be identifier
                throw new ParseException("Expected identifier at:", getIndex());
            }
            right = tokens.get(0);
            tokens.advance();
            if(match("(")){
                //Function call inside
                List<Ast.Expression> params = new ArrayList<>();
                while (tokens.has(0) && !peek(")")){
                    params.add(parseExpression());
                    if(!peek(")") && !peek(",")){
                        throw new ParseException("Expected comma or end of function at: ", getIndex());
                    }
                    if(match(",")){
                        if(peek(")")){
                            throw new ParseException("Expected Expression After Comma at: ", getIndex());
                        }
                    }
                }
                if(peek(")")){
                    tokens.advance();
                    Ast.Expression temp = new Ast.Expression.Function(Optional.of(first), right.getLiteral(), params);
                    first = temp;
                }

            }
            else{
                //variable call inside
                Ast.Expression temp = new Ast.Expression.Access(Optional.of(first), right.getLiteral());
                first = temp;
            }
        }
        throw new ParseException("Expected end of expression at:", getIndex());
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
            quoted = quoted.substring(1, quoted.length()-1);
            quoted = quoted.replace("\\b", "\b").replace("\\n", "\n").replace("\\r", "\r").replace("\\t", "\t");
            quoted = quoted.replace("\\'", "'").replace("\\\\", "\\").replace("\\\"", "\"");
            Character val = quoted.charAt(0);
            tokens.advance();
            return new Ast.Expression.Literal(val);
        }
        else if (peek(Token.Type.STRING)) {
            String quoted = new String(tokens.get(0).getLiteral());
            quoted = quoted.substring(1, quoted.length()-1);
            quoted = quoted.replace("\\b", "\b").replace("\\n", "\n").replace("\\r", "\r").replace("\\t", "\t");
            quoted = quoted.replace("\\'", "'").replace("\\\\", "\\").replace("\\\"", "\"");
            tokens.advance();
            return new Ast.Expression.Literal(quoted);
        }
        else if (match("(")) {
            Ast.Expression group = parseExpression();
            if(!match(")")){
                throw new ParseException("Expected ) at: ", getIndex());
            }
            return new Ast.Expression.Group(group);
        }
        else if (peek(Token.Type.IDENTIFIER)) {
            String literal = tokens.get(0).getLiteral();
            tokens.advance();
            if(!match("(")){
                return new Ast.Expression.Access(Optional.empty(), literal);
            }
            List<Ast.Expression> params = new ArrayList<>();
            while (tokens.has(0) && !peek(")")){
                params.add(parseExpression());
                if(!peek(")") && !peek(",")){
                    throw new ParseException("Expected comma or end of function at: ", getIndex());
                }
                if(match(",")){
                    if(peek(")")){
                        throw new ParseException("Expected Expression After comma at: ", getIndex());
                    }
                }
            }
            if(peek(")")){
                tokens.advance();
                return new Ast.Expression.Function(Optional.empty(), literal, params);
            }

            throw new ParseException("Expected end of function ) at: ", getIndex());

        }

        throw new ParseException("Invalid/Missing Expression at: ", getIndex());
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

    // helper function for computing index of expected token in case of ParseException
    private int getIndex() {
        if (!tokens.has(0)) {
            return tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length();
        } else {
            return tokens.get(0).getIndex();
        }
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
