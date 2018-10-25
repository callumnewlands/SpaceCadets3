import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.scene.text.Text;

// TODO: javadoc
// optional TODO: add subroutines

class Interpreter
{
    private ArrayList<String> _sourceCode = new ArrayList<>();
    private Map<String, Integer> _variables = new HashMap<>();
    private Integer _linePtr = 0;
    private Stack<WhileLoopPtr> whileLoopPtrs = new Stack<>();

    private static final String[] RESERVED_IDENTIFIERS = {"clear", "decr", "do", "end", "incr", "while", "swap",
                                                            "copy", "to", "not", "not\\s+0", "and", "del", "ifp", "ifz", "goto"};
    static final String VARIABLE_REG_EX = "([a-zA-Z]\\w*)";
    static final String SEMICOLON_REG_EX = "\\s*;\\s*";
    static final String COMMENT_REG_EX = "\\s*#.*";
    static final String ASSIGNMENT_REG_EX = "\\s*" + VARIABLE_REG_EX + "\\s+(=)\\s+([0-9]+)" + SEMICOLON_REG_EX;
    static final String UNARY_OPERATOR_REG_EX = "\\s*(clear|incr|decr|del)\\s+" + VARIABLE_REG_EX + SEMICOLON_REG_EX;
    static final String BINARY_AFTER_OPERATOR_REG_EX = "\\s*(swap)\\s+" + VARIABLE_REG_EX + "\\s+and\\s+"
                                                        + VARIABLE_REG_EX + SEMICOLON_REG_EX;
    static final String BINARY_MIDDLE_OPERATOR_REG_EX = "\\s*" + VARIABLE_REG_EX + "\\s+=\\s+"
                                                        + VARIABLE_REG_EX + "\\s+([+-/*])\\s+"
                                                        + VARIABLE_REG_EX + SEMICOLON_REG_EX;
    static final String COPY_TO_REG_EX = "\\s*(copy)\\s+" + VARIABLE_REG_EX + "\\s+to\\s+"
                                                        + VARIABLE_REG_EX + SEMICOLON_REG_EX;
    static final String IF_REG_EX = "\\s*(ifp|ifz)\\s+" + VARIABLE_REG_EX + "\\s+goto\\s+([0-9]+)" + SEMICOLON_REG_EX;
    static final String KEYWORD_REG_EX = "\\b(" + String.join("|", RESERVED_IDENTIFIERS) + ")\\b";
    static final String WHILE_REG_EX = "\\s*(while)\\s+" + VARIABLE_REG_EX + "\\s+not\\s+0\\s+do" + SEMICOLON_REG_EX;
    static final String END_REG_EX = "\\s*(end)" + SEMICOLON_REG_EX;
    private static final String BLANK_REG_EX = "\\s*";


    private Text txtOutput;

   Interpreter(String code, Text txtOutput)
    {
        String[] lines = code.split("\\r?\\n");
        for (String line : lines)
            _sourceCode.add(line.toLowerCase());

        this.txtOutput = txtOutput;
        this.txtOutput.setText("Output:\n");
    }

    /**
     * Executes the source code line by line
     * @throws InterpreterException If there is no source code, or in the result of a syntax error
     */
    void execute() throws InterpreterException
    {
        if (_sourceCode.size() <= 0)
            throw new InterpreterException("No source code to execute");

        while(_linePtr < _sourceCode.size())
        {
            String line =_sourceCode.get(_linePtr);
            try { executeLine(line); }
            catch (EndOfLoopException e) { return; }
            if (!isCommentOrBlank(line))
                outputVariables();
            _linePtr++;
        }

    }

    /**
     * Executes one line of code
     * @param line the line of code to execute
     * @throws InterpreterException If there is a syntax error
     */
    private void executeLine(String line) throws InterpreterException
    {
        if (isCommentOrBlank(line))
            return;

        if (!isSyntacticallyCorrect(line))
            throw new SyntaxErrorException("Syntax Error in line " + (_linePtr + 1) + ": " + line);

        DecodedInstruction instruction = new DecodedInstruction(line);

        if (isReservedIdentifier(instruction.getOperator1()))
            throw new SyntaxErrorException("Syntax Error in line " + (_linePtr + 1) + ": " + instruction.getOperator1() + " is a reserved keyword");

        switch (instruction.getInstruction())
        {
            case "clear":
                clear(instruction.getOperator1());
                break;
            case "incr":
                increment(instruction.getOperator1());
                break;
            case "decr":
                decrement(instruction.getOperator1());
                break;
            case "swap":
                swap(instruction.getOperator1(), instruction.getOperator2());
                break;
            case "copy":
                copyTo(instruction.getOperator1(), instruction.getOperator2());
                break;
            case "+":
                add(instruction.getOperator1(), instruction.getOperator2(), instruction.getDestination());
                break;
            case "-":
                subtract(instruction.getOperator1(), instruction.getOperator2(), instruction.getDestination());
                break;
            case "*":
                multiply(instruction.getOperator1(), instruction.getOperator2(), instruction.getDestination());
                break;
            case "/":
                divide(instruction.getOperator1(), instruction.getOperator2(), instruction.getDestination());
                break;
            case "=":
                assign(instruction.getOperator1(), instruction.getDestination());
                break;
            case "del":
                delete(instruction.getOperator1());
                break;
            case "ifp":
                ifPositive(instruction.getOperator1(), instruction.getDestination());
                break;
            case "ifz":
                ifZero(instruction.getOperator1(), instruction.getDestination());
                break;
            case "while":
                whileLoopPtrs.push(new WhileLoopPtr(_linePtr));
                while (_variables.get(instruction.getOperator1()) > 0)
                {
                    _linePtr++;
                    execute();
                }
                _linePtr = whileLoopPtrs.pop().endLine;
                break;
            case "end":
                try
                {
                    whileLoopPtrs.peek().endLine = _linePtr;
                }
                catch (EmptyStackException e)
                {
                    throw new SyntaxErrorException("No matching while for line " + (_linePtr + 1) + ": " + line);
                }
                _linePtr = whileLoopPtrs.peek().startLine;
                throw new EndOfLoopException();
            default:
                throw new InterpreterException("Invalid command in line " + (_linePtr + 1) + ": " + line);
        }
    }

    /**
     * Sets a specified variable to 0
     * @param variable the variable to set
     */
    private void clear(String variable)
    {
        _variables.put(variable, 0);
    }

    /**
     * Increments the specified variable
     * @param variable The variable to increment
     */
    private void increment(String variable)
    {
        try {_variables.put(variable, _variables.get(variable) + 1); }
        catch (NullPointerException e) {_variables.put(variable, 1); }
    }

    /**
     * Decrements the specified variable
     * @param variable The variable to decrement
     */
    private void decrement(String variable)
    {
        try
        {
            int value = _variables.get(variable);
            if (value > 0)
                _variables.put(variable, value - 1);
            else
                _variables.put(variable, 0);
        }
        catch (NullPointerException e)
        {
            _variables.put(variable, 0);
        }
    }

    /**
     * Deletes a variable from the interpreter - means it's value is deleted, and it will not be output
     * @param var1 The variable to delete
     */
    private void delete(String var1)
    {
        _variables.remove(var1);
    }

    /**
     * Swaps the values of the 2 specified variables
     * @param var1 A variable to swap
     * @param var2 A variable to swap
     */
    private void swap(String var1, String var2)
    {
        checkVariableInit(var1);
        checkVariableInit(var2);
        Integer tmp = _variables.get(var1);
        _variables.put(var1, _variables.get(var2));
        _variables.put(var2, tmp);
    }

    /**
     * Copies the value from one variable to another
     * @param var1 the variable to copy from
     * @param var2 The variable to copy to
     */
    private void copyTo(String var1, String var2)
    {
        checkVariableInit(var1);
        _variables.put(var2, _variables.get(var1));
    }

    /**
     * Moves execution to the specified line if the specified variable is positive
     * @param var The variable to check is it is positive
     * @param lineNo The number of the line to go to if the variable is positive
     */
    private void ifPositive(String var, String lineNo)
    {
        checkVariableInit(var);
        _linePtr = _variables.get(var) > 0 ? Integer.parseInt(lineNo) - 2 : _linePtr;
    }

    /**
     * Moves execution to the specified line if the specified variable is zero
     * @param var The variable to check is it is zero
     * @param lineNo The number of the line to go to if the variable is zero
     */
    private void ifZero(String var, String lineNo)
    {
        checkVariableInit(var);
        _linePtr = _variables.get(var).equals(0) ? Integer.parseInt(lineNo) - 2 : _linePtr;
    }

    /**
     * Assign a given value to a variable
     * @param value The value to assign
     * @param dest The variable to be assigned to
     */
    private void assign(String value, String dest)
    {
        _variables.put(dest, Integer.parseInt(value));
    }

    /**
     * Adds 2 variables together and stores the result in destination
     * @param var1 A variable to add
     * @param var2 A variable to add
     * @param dest The variable to store the result in
     */
    private void add(String var1, String var2, String dest)
    {
        checkVariableInit(var1);
        checkVariableInit(var2);
        _variables.put(dest, _variables.get(var1) + _variables.get(var2));
    }

    /**
     * Subtracts 1 variable from another and stores the result in destination
     * @param var1 The variable to subtract from
     * @param var2 The variable to subtract
     * @param dest The variable to store the result in
     */
    private void subtract(String var1, String var2, String dest)
    {
        checkVariableInit(var1);
        checkVariableInit(var2);
        _variables.put(dest, _variables.get(var1) - _variables.get(var2));
    }

    /**
     * Multiplies 2 variables together and stores the result in destination
     * @param var1 A variable to multiply
     * @param var2 A variable to multiply
     * @param dest The variable to store the result in
     */
    private void multiply(String var1, String var2, String dest)
    {
        checkVariableInit(var1);
        checkVariableInit(var2);
        _variables.put(dest, _variables.get(var1) * _variables.get(var2));
    }


    /**
     * Performs integer division between 2 variables and stores the result in destination
     * @param var1 The denominator variable
     * @param var2 The numerator variable
     * @param dest The variable to store the result in
     */
    private void divide(String var1, String var2, String dest)
    {
        checkVariableInit(var1);
        checkVariableInit(var2);
        _variables.put(dest, _variables.get(var1) / _variables.get(var2));
    }

    /**
     * Checks whether a given variable is initialised, and if not sets its value to 0
     * @param var the variable to initialise (if not already initialised)
     */
    private void checkVariableInit(String var)
    {
        _variables.putIfAbsent(var, 0);
    }

    /**
     * Prints each variable and its value into a string
     * If txtOutput exists then the string is appended to the end of txtOutput
     * Else the string is printed to the standard output
     */
    private void outputVariables()
    {
        _variables.forEach((variable, value) ->
        {
            String output = variable + ":" + value + ", ";
            if (txtOutput != null)
                txtOutput.setText(txtOutput.getText() + output);
            else
                System.out.print(output);
        });
        if (txtOutput != null)
            txtOutput.setText(txtOutput.getText() + "\n");
        else
            System.out.println();
    }

    /**
     * Checks if the given line is a comment or blank line, or not
     * @param line The line to check
     * @return True if the line is a comment or a blank like
     */
    private boolean isCommentOrBlank(String line)
    {
        Pattern commentPattern = Pattern.compile(COMMENT_REG_EX);
        Matcher commentMatcher = commentPattern.matcher(line);

        Pattern blankPattern = Pattern.compile(BLANK_REG_EX);
        Matcher blankMatcher = blankPattern.matcher(line);

        return commentMatcher.matches() || blankMatcher.matches();
    }

    /**
     * Checks if the given line is syntactically correct
     * @param line The line to check
     * @return True if the line matches the syntax specifiers (RegEx Strings)
     */
    private boolean isSyntacticallyCorrect(String line)
    {
        Matcher unaryMatcher = Pattern.compile(UNARY_OPERATOR_REG_EX).matcher(line);
        Matcher binaryAfterMatcher = Pattern.compile(BINARY_AFTER_OPERATOR_REG_EX).matcher(line);
        Matcher whileMatcher = Pattern.compile(WHILE_REG_EX).matcher(line);
        Matcher endMatcher = Pattern.compile(END_REG_EX).matcher(line);
        Matcher binaryMiddleMatcher = Pattern.compile(BINARY_MIDDLE_OPERATOR_REG_EX).matcher(line);
        Matcher copyToMatcher = Pattern.compile(COPY_TO_REG_EX).matcher(line);
        Matcher assignmentMatcher = Pattern.compile(ASSIGNMENT_REG_EX).matcher(line);
        Matcher ifMatcher = Pattern.compile(IF_REG_EX).matcher(line);

        return unaryMatcher.matches() || binaryAfterMatcher.matches() || whileMatcher.matches()
                || endMatcher.matches() || binaryMiddleMatcher.matches() || copyToMatcher.matches()
                || assignmentMatcher.matches() || ifMatcher.matches();
    }

    /**
     * Checks if a String is a reserved identifier
     * @param id The string to check
     * @return True if it is a reserved identifier
     */
    private boolean isReservedIdentifier(String id)
    {
        for (String i : RESERVED_IDENTIFIERS)
        {
            if (i.equals(id))
                return true;
        }
        return false;
    }

}
