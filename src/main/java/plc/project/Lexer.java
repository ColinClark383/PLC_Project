package plc.project;

import java.util.ArrayList;
import java.util.List;

/**
 * The lexer works through three main functions:
 *
 *  - {@link #lex()}, which repeatedly calls lexToken() and skips whitespace
 *  - {@link #lexToken()}, which lexes the next token
 *  - {@link CharStream}, which manages the state of the lexer and literals
 *
 * If the lexer fails to parse something (such as an unterminated string) you
 * should throw a {@link ParseException} with an index at the invalid character.
 *
 * The {@link #peek(String...)} and {@link #match(String...)} functions are
 * helpers you need to use, they will make the implementation easier.
 */
public final class Lexer {

    private final CharStream chars;

    public Lexer(String input) {
        chars = new CharStream(input);
    }

    /**
     * Repeatedly lexes the input using {@link #lexToken()}, also skipping over
     * whitespace where appropriate.
     */
    public List<Token> lex() {

        //initialize return variable
        List<Token> tokens = new ArrayList<>();

        while(chars.has(0)){
            if(!(peek(" ") || peek("\\t") || peek("\\n") || peek("\\r") || peek("\\b"))){
                //not a whitespace!
                tokens.add(lexToken());
            }
            else {
                //white space, skip
                chars.advance();
                chars.skip();
            }
        }

        return tokens; //TODO: Ngl i think this function works but it wouldn't hurt you checking
    }

    /**
     * This method determines the type of the next token, delegating to the
     * appropriate lex method. As such, it is best for this method to not change
     * the state of the char stream (thus, use peek not match).
     *
     * The next character should start a valid token since whitespace is handled
     * by {@link #lex()}
     */
    public Token lexToken() {
        //think this all fits the provided definitions

        //check for identifier
        if ((peek("\\w") && !peek("\\d")) ) {
            return lexIdentifier();
        }

        //add check for number
        //I think + needs to be \\ because it is a regex metacharacter
        else if(peek("\\d") || peek("\\+") || peek("-")){
            return lexNumber();
        }

        //check for char
        else if(peek("'")){
            return lexCharacter();
        }

        //check for string
        else if(peek("\"")){
            return lexString();
        }

        //I guess we doing operators now
        else if(!(peek(" ") || peek("\\n") || peek("\\b") || peek("\\r") || peek("\\t"))){
            return lexOperator();
        }
        else {
            throw new ParseException("Whitespace in the lexToken", chars.index);
        }
        //TODO: make sure logic checks out (Logic the Rapper reference?!?!?!?)
    }

    public Token lexIdentifier() {
        while((match("\\w") || match("-")) && chars.has(0)){
            //auto advances
        }
        return chars.emit(Token.Type.IDENTIFIER); //TODO: Testing
    }

    public Token lexNumber() {
        // tracking variable
        boolean hasSign = false;

        // allow optional + or -
        if (chars.has(0) && (peek("\\+") || peek("-"))) {
            hasSign = true;
            chars.advance();
            if(!peek("\\d") || (peek("0") && !peek("0", "."))){
                //+ or - sign is not attached to a valid digit, therefore an operator
                return chars.emit(Token.Type.OPERATOR);
            }
        }

        // handle zero integers first
        if (chars.has(0) && peek("0")) {
            chars.advance();
            // check if zero part of decimal
            if (chars.has(0) && peek("\\.")) {
                chars.advance();
                // decimal must be followed by digit
                if (!chars.has(0) || !(peek("\\d"))) {
                    throw new ParseException("Invalid Decimal", chars.index);
                }
                // handle rest of numbers after .
                while (peek("\\d")) {
                    chars.advance();
                }
                return chars.emit(Token.Type.DECIMAL);
            }
            // cant have +0 or -0 for int
            if (hasSign) {
                throw new ParseException("Invalid Integer", chars.index);
            }
            else {
                return chars.emit(Token.Type.INTEGER);
            }
        }
        // handle non-zero integer cases
        if (chars.has(0) && peek("[1-9]")) {
            while (peek("\\d")) {
                chars.advance();
            }
        }
        else {
            throw new ParseException("Invalid Number", chars.index);
        }
        // handle non-zero decimal cases
        if (chars.has(1) && peek("\\.", "\\d")) {
            chars.advance();
            // get rest of digits after .
            while (peek("\\d")) {
                chars.advance();
            }
            return chars.emit(Token.Type.DECIMAL);
        }
        return chars.emit(Token.Type.INTEGER);
    }

    public Token lexCharacter() {
        //lets get this out the way
        match("'");
        //now the fun begins
        if(!chars.has(0) || peek("'") || peek("\n") ){
            //cannot be an empty char or new line or end of stream
            throw new ParseException("Invalid Character", chars.index);
        }


        //check if it is an escape character
        if(match("\\\\")){
            lexEscape();
        }
        else{
            //else one character gets advanced
            chars.advance();
        }

        if(!chars.has(0)){
            //make sure there is another character
            throw new ParseException("no ending single quote", chars.index);
        }

        if(!match("'")){
            //char not closed after 1 character, parse error
            throw new ParseException("char too long", chars.index);
        }

        return chars.emit(Token.Type.CHARACTER); //TODO: Testing
    }

    public Token lexString() {
        //here we go
        match("\\\"");

        while( (chars.has(0) && !peek("\n")) && !peek("\\\"") ){
            if(match("\\\\")){
                lexEscape();
            }
            else{
                chars.advance();
            }
        }

        if(!chars.has(0) || peek("\\n")){
            //if the while loop is exited and there is no closing string or newline
            throw new ParseException("unterminated string", chars.index);
        }

        chars.advance();

        return chars.emit(Token.Type.STRING);
         //TODO: Testing
    }

    public void lexEscape() {
        //let's operate to where the backslash is matched before this function is called
        //bnrt'"\
        if(chars.has(0) && (peek("b") || peek("n") || peek("r") || peek("t") || peek("'") || peek("\\\"") || peek("\\\\"))){
            chars.advance();
        }
        else{
            throw new ParseException("invalid escape", chars.index);
        }
    }

    public Token lexOperator() {
        // 2 character sequences are handled seperatedly
        if(chars.has(1) && (peek("&", "&") || peek("|", "|") || peek("!", "=") || peek("=", "=") || peek("<", "=") || peek(">", "="))) {
            chars.advance();
            chars.advance();
        }
        // operator cannot be whitespace or escape sequence
        else if(chars.has(0) && !Character.isWhitespace(chars.get(0)) && (!peek("\\\\"))) {
            chars.advance();
        }
        else{
            System.out.println("Unexpected character at index: " + chars.index + " - '" + chars.get(0) + "'");
            throw new ParseException("Invalid Operator", chars.index);
        }
        return chars.emit(Token.Type.OPERATOR);
    }

    /**
     * Returns true if the next sequence of characters match the given patterns,
     * which should be a regex. For example, {@code peek("a", "b", "c")} would
     * return true if the next characters are {@code 'a', 'b', 'c'}.
     */
    public boolean peek(String... patterns) {
        for(int i = 0; i < patterns.length; i++){
            if(!chars.has(i) ||
            !String.valueOf(chars.get(i)).matches(patterns[i])){
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true in the same way as {@link #peek(String...)}, but also
     * advances the character stream past all matched characters if peek returns
     * true. Hint - it's easiest to have this method simply call peek.
     */
    public boolean match(String... patterns) {
        boolean peek = peek(patterns);
        if(peek){
            for(int i = 0; i < patterns.length; i++){
                chars.advance();
            }
        }
        return peek;
    }

    /**
     * A helper class maintaining the input string, current index of the char
     * stream, and the current length of the token being matched.
     *
     * You should rely on peek/match for state management in nearly all cases.
     * The only field you need to access is {@link #index} for any {@link
     * ParseException} which is thrown.
     */
    public static final class CharStream {

        private final String input;
        private int index = 0;
        private int length = 0;

        public CharStream(String input) {
            this.input = input;
        }

        public boolean has(int offset) {
            return index + offset < input.length();
        }

        public char get(int offset) {
            return input.charAt(index + offset);
        }

        public void advance() {
            index++;
            length++;
        }

        public void skip() {
            length = 0;
        }

        public Token emit(Token.Type type) {
            int start = index - length;
            skip();
            return new Token(type, input.substring(start, index), start);
        }

    }

}
