import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.stereotype.Component;
import vendor.ParsingException;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author izaharkin
 * @since 11.10.16
 * <p>
 * Implementation using CF-grammatic (КС-грамматика) (syntax analyzator).
 */
@Component("RestfulCalculator")
@Configurable
public class RestfulCalculator {
    public static final RestfulCalculator INSTANCE = new RestfulCalculator();
    @Autowired
    private BillingDao dbms;
    private String username;

    private RestfulCalculator() {
    }

    public double calculate(String userName, String expression) throws ParsingException {
        if (expression == null) {
            throw new ParsingException("Null expression");
        }
        if (expression.matches(".*[\\d.]\\s+[\\d.].*")) {
            throw new ParsingException("Illegal spaces between numbers");
        }
        expression = expression.replaceAll("\\s+", "");
        if (expression.length() == 0) {
            throw new ParsingException("Empty string");
        }
        SyntaxAnalyzer arithmeticalAnalyzer = new SyntaxAnalyzer();
        return arithmeticalAnalyzer.evaluate(userName, expression);
    }

    private class SyntaxAnalyzer {
        private final char[] ARITHMETIC_SYMBOLS = {'+', '-', '*', '/', '(', ')'};
        private TokenStream tokenStream = new TokenStream();

        private boolean isOperator(char character) {
            for (char ch : ARITHMETIC_SYMBOLS) {
                if (character == ch) {
                    return true;
                }
            }
            return false;
        }

        private class Token {
            private char type;
            private double value;

            Token(char tokenType) {
                type = tokenType;
            }

            Token(char tokenType, double val) {
                type = tokenType;
                value = val;
            }

            Token(double val) {
                type = 'n';
                value = val;
            }

            char getType() {
                return type;
            }

            double getValue() {
                return value;
            }
        }

        public double evaluate(String userName, String expression) throws ParsingException {
            username = userName;
            tokenStream.setParsingExpression(expression);
            checkBracesBalance(expression);
            double value = expressionRule();
            if (!tokenStream.atLastSymbol()) {
                throw new ParsingException("Illegal expression");
            }
            return value;
        }

        // throws ParsingException if there is too much or too few braces
        private void checkBracesBalance(String expression) throws ParsingException {
            Stack<Character> openBraces = new Stack<>();
            for (int i = 0; i < expression.length(); ++i) {
                if (expression.charAt(i) == '(') {
                    openBraces.push(expression.charAt(i));
                } else if (expression.charAt(i) == ')') {
                    if (openBraces.empty()) {
                        throw new ParsingException("Too few braces");
                    }
                    openBraces.pop();
                }
            }
            if (!openBraces.empty()) {
                throw new ParsingException("Too much braces");
            }
        }

        public String bindFunctionWithNumericalParams(String userName, String funcName, List<Double> valuesOfArguments) {
            String body = dbms.getFunctionBody(userName, funcName);
            int valence = dbms.getFunctionValence(userName, funcName);
            if (valence != valuesOfArguments.size()) {
                throw new IllegalArgumentException("Wrong number of parameters in specified function");
            }
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; i < body.length(); ++i) {
                if (body.charAt(i) == '$' && i != body.length()-1) {
                    int argNum = Character.getNumericValue(body.charAt(++i)); // TODO: Numbers, not digits!!
                    stringBuilder.append(valuesOfArguments.get(argNum));
                } else {
                    stringBuilder.append(body.charAt(i));
                }
            }
            return stringBuilder.toString();
        }

        private class TokenStream {
            private String expression;
            private int curPos;
            Token buffer;
            private boolean filled;

            public void setParsingExpression(String parsingExpression) {
                expression = parsingExpression;
                curPos = 0;
                filled = false;
            }

            public Token getCurrentToken() throws ParsingException {
                if (filled) {
                    filled = false;
                    return buffer;
                }
                if (curPos >= expression.length()) {
                    return new Token('#');
                }
                char current = expression.charAt(curPos);
                if (isOperator(current)) {
                    curPos += 1;
                    return new Token(current);
                } else if (Character.isDigit(current)) {
                    StringBuilder stringBuilder = new StringBuilder();
                    while (curPos < expression.length() && Character.isDigit(expression.charAt(curPos))) {
                        stringBuilder.append(expression.charAt(curPos));
                        curPos += 1;
                    }
                    if (curPos < expression.length() && expression.charAt(curPos) == '.') {
                        stringBuilder.append(expression.charAt(curPos));
                        curPos += 1;
                        while (curPos < expression.length() && Character.isDigit(expression.charAt(curPos))) {
                            stringBuilder.append(expression.charAt(curPos));
                            curPos += 1;
                        }
                    }
                    String strNumber = stringBuilder.toString();
                    double value = Double.parseDouble(strNumber);
                    return new Token(value);
                } else if (Character.isAlphabetic(current)) {
                    StringBuilder stringBuilder = new StringBuilder();
                    while (curPos < expression.length() && (Character.isAlphabetic(expression.charAt(curPos)) || Character.isDigit(expression.charAt(curPos)) || expression.charAt(curPos) == '_')) {
                        stringBuilder.append(expression.charAt(curPos));
                        curPos += 1;
                    }
                    if (curPos < expression.length() && expression.charAt(curPos) == '(') {
                        String funcName = stringBuilder.toString();
                        AtomicInteger endPos = new AtomicInteger(0);
                        List<String> arguments = Helper.getArgumentsFromExpression(expression, curPos, endPos);
                        List<Double> valuesOfArguments = new ArrayList<>();
                        for (String argument : arguments) {
                            double valueOfExpression = calculate(username, argument);
                            valuesOfArguments.add(valueOfExpression); // TODO: Check handling
                        }
                        double value;
                        if (BasicFunctionsHandler.isBasicFunction(funcName)) {
                            value = BasicFunctionsHandler.execBasicFunction(funcName, valuesOfArguments);
                        } else {
                            String funcNumericalExpression = bindFunctionWithNumericalParams(username, funcName, valuesOfArguments);
                            value = calculate(username, funcNumericalExpression);
                        }
                        curPos = endPos.get() + 1;
                        return new Token('f', value);
                    }
                    String varName = stringBuilder.toString(); // TODO: spaces
                    double value = dbms.getVariableValue(username, varName);
                    return new Token('v', value);
                } else {
                    throw new ParsingException("Unexpected character");
                }
            }

            public void putBackToStream(Token token) throws ParsingException {
                if (filled) {
                    throw new ParsingException("Illegal expression");
                }
                buffer = token;
                filled = true;
            }

            public boolean atLastSymbol() {
                return curPos == expression.length();
            }
        }

        // Handle numbers, variables, functions and braces
        private double primaryExprRule() throws ParsingException {
            Token curToken = tokenStream.getCurrentToken();
            switch (curToken.getType()) {
                case 'n':
                    return curToken.getValue();
                case 'v':
                    return curToken.getValue();
                case 'f':
                    return curToken.getValue();
                case '(':
                    double value = expressionRule();
                    curToken = tokenStream.getCurrentToken();
                    if (curToken.getType() != ')') {
                        throw new ParsingException("Bad parentheses balance");
                    }
                    return value;
                case '-':
                    return -primaryExprRule();
                default:
                    throw new ParsingException("Empty parenthesis");
            }
        }

        // Handle * and /
        private double termRule() throws ParsingException {
            double value = primaryExprRule();
            Token curToken = tokenStream.getCurrentToken();
            while (true) {
                switch (curToken.getType()) {
                    case '*':
                        value *= primaryExprRule();
                        curToken = tokenStream.getCurrentToken();
                        break;
                    case '/':
                        value /= primaryExprRule();
                        curToken = tokenStream.getCurrentToken();
                        break;
                    default:
                        tokenStream.putBackToStream(curToken);
                        return value;
                }
            }
        }

        // Handle + and -
        private double expressionRule() throws ParsingException {
            double value = termRule();
            Token curToken = tokenStream.getCurrentToken();
            while (true) {
                switch (curToken.getType()) {
                    case '+':
                        value += termRule();
                        curToken = tokenStream.getCurrentToken();
                        break;
                    case '-':
                        value -= termRule();
                        curToken = tokenStream.getCurrentToken();
                        break;
                    default:
                        tokenStream.putBackToStream(curToken);
                        return value;
                }
            }
        }
    }
}
