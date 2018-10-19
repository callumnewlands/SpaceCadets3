import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.scene.text.Text;

// TODO: javadoc
// TODO: fix StackOverflowException

class Interpreter
{
    private ArrayList<String> _sourceCode = new ArrayList<>();
    private Map<String, Integer> _variables = new HashMap<>();
    private Integer _linePtr = 0;
    private Stack<WhileLoopPtr> whileLoopPtrs = new Stack<>();

    private static final String[] RESERVED_IDENTIFIERS = {"clear", "decr", "do", "end", "incr", "while", "swap", "copy", "to"};
    public static final String COMMENT_REG_EX = "\\s*#.*";
    private static final String UNARY_OPERATOR_REG_EX = "\\s*(clear|incr|decr)\\s+(\\w+)\\s*;\\s*";
    private static final String BINARY_AFTER_OPERATOR_REG_EX = "\\s*(swap)\\s+(\\w+)\\s+(\\w+)\\s*;\\s*";
    private static final String BINARY_MIDDLE_OPERATOR_REG_EX = "\\s*(\\w+)\\s+(\\+|-|/|\\*)\\s+(\\w+)\\s*;\\s*";
    private static final String COPY_TO_REG_EX = "\\s*(copy)\\s+(\\w+)\\s+to\\s+(\\w+)\\s*;\\s*";
    public static final String KEYWORD_REG_EX = "\\b(" + String.join("|", RESERVED_IDENTIFIERS) + ")\\b";
    private static final String WHILE_REG_EX = "\\s*(while)\\s+(\\w+)\\s+not\\s+0\\s+do\\s*;\\s*";
    private static final String END_REG_EX = "\\s*(end)\\s*;\\s*";
    private static final String BLANK_REG_EX = "\\s*";
    public static final String SEMICOLON_REG_EX = "\\;";

    private Text txtOutput;

   Interpreter(String code, Text txtOutput)
    {
        String[] lines = code.split("\\r?\\n");
        for (String line : lines)
            _sourceCode.add(line);

        this.txtOutput = txtOutput;
        this.txtOutput.setText("Output: ");
    }


    void execute() throws InterpreterException
    {
        if (_sourceCode.size() <= 0)
            throw new InterpreterException("No source code to execute");

        while(_linePtr < _sourceCode.size())
        {
            try { executeLine(_sourceCode.get(_linePtr)); }
            catch (EndOfLoopException e) { return; }
            outputVariables();
            _linePtr++;
        }

    }

    private void executeLine(String line) throws InterpreterException
    {
        if (isCommentOrBlank(line))
            return;

        if (!isSyntacticallyCorrect(line))
            throw new InterpreterException("Syntax Error in line " + (_linePtr + 1) + ": " + line);

        Pattern variablePattern = Pattern.compile(UNARY_OPERATOR_REG_EX);
        Matcher variableMatcher = variablePattern.matcher(line);
        variableMatcher.find();

        // TODO: redo like GUI pattern matching with groups

        String instruction = "";
        String operator1 = "";
        String operator2 = "";
        try
        {
            instruction = variableMatcher.group(1);
            operator1 = variableMatcher.group(2);
        }
        catch (IllegalStateException e)
        {
            Pattern whilePattern = Pattern.compile(WHILE_REG_EX);
            Matcher whileMatcher = whilePattern.matcher(line);
            whileMatcher.find();

            try
            {
                instruction = whileMatcher.group(1);
                operator1 = whileMatcher.group(2);
            }
            catch (IllegalStateException e2)
            {
                Pattern endPattern = Pattern.compile(END_REG_EX);
                Matcher endMatcher = endPattern.matcher(line);
                endMatcher.find();

                try
                {
                    instruction = endMatcher.group(1);
                }
                catch (IllegalStateException e3)
                {

                    Pattern binaryPattern = Pattern.compile(BINARY_AFTER_OPERATOR_REG_EX);
                    Matcher binaryMatcher = binaryPattern.matcher(line);
                    binaryMatcher.find();

                    try
                    {
                        instruction = binaryMatcher.group(1);
                        operator1 = binaryMatcher.group(2);
                        operator2 = binaryMatcher.group(3);
                    }
                    catch (IllegalStateException e4)
                    {

                        Matcher binaryMiddleMatcher = Pattern.compile(BINARY_MIDDLE_OPERATOR_REG_EX).matcher(line);
                        binaryMiddleMatcher.find();
                        try
                        {
                            instruction = binaryMatcher.group(2);
                            operator1 = binaryMatcher.group(1);
                            operator2 = binaryMatcher.group(3);
                        }
                        catch (IllegalStateException e5)
                        {

                            Matcher copyToMatcher = Pattern.compile(COPY_TO_REG_EX).matcher(line);
                            copyToMatcher.find();

                            try
                            {
                                instruction = copyToMatcher.group(1);
                                operator1 = copyToMatcher.group(2);
                                operator2 = copyToMatcher.group(3);
                            }
                            catch (IllegalStateException e6)
                            {
                                operator1 = "";
                                operator2 = "";
                                instruction = "";
                            }
                        }

                    }

                }
            }
        }

        if (isReservedIdentifier(operator1))
            throw new InterpreterException("Syntax Error in line " + (_linePtr + 1) + ": " + operator1 + " is a reserved keyword");

        switch (instruction)
        {
            case "clear":
                clear(operator1);
                break;
            case "incr":
                increment(operator1);
                break;
            case "decr":
                decrement(operator1);
                break;
            case "swap":
                swap(operator1, operator2);
                break;
            case "copy":
                copyTo(operator1, operator2);
                break;
            //TODO: implement + - * / and change regex to take 3 args.
            case "while":
                whileLoopPtrs.push(new WhileLoopPtr(_linePtr));
                while (_variables.get(operator1) > 0)
                {
                    _linePtr++;
                    execute();
                }
                _linePtr = whileLoopPtrs.pop().endLine;
                break;
            case "end":
                whileLoopPtrs.peek().endLine = _linePtr;
                _linePtr = whileLoopPtrs.peek().startLine;
                throw new EndOfLoopException();
            default:
                throw new InterpreterException("Invalid command in line " + (_linePtr + 1) + ": " + line);
        }


    }

    private void clear(String variable)
    {
        _variables.put(variable, 0);
    }

    private void increment(String variable)
    {
        try {_variables.put(variable, _variables.get(variable) + 1); }
        catch (NullPointerException e) {_variables.put(variable, 1); }
    }

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

    private void swap(String var1, String var2)
    {
        // TODO: extract method and replace with Map.putIfAbsent
        if (_variables.get(var1) == null)
            _variables.put(var1, 0);
        if (_variables.get(var2) == null)
            _variables.put(var2, 0);


        Integer tmp = _variables.get(var1);
        _variables.put(var1, _variables.get(var2));
        _variables.put(var2, tmp);
    }

    private void copyTo(String var1, String var2)
    {
        // TODO: extract method and replace with Map.putIfAbsent
        if (_variables.get(var1) == null)
            _variables.put(var1, 0);

        _variables.put(var2, _variables.get(var1));
    }

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

    private boolean isCommentOrBlank(String line)
    {
        Pattern commentPattern = Pattern.compile(COMMENT_REG_EX);
        Matcher commentMatcher = commentPattern.matcher(line);

        Pattern blankPattern = Pattern.compile(BLANK_REG_EX);
        Matcher blankMatcher = blankPattern.matcher(line);

        return commentMatcher.matches() || blankMatcher.matches();
    }

    private boolean isSyntacticallyCorrect(String line)
    {
        Matcher unaryMatcher = Pattern.compile(UNARY_OPERATOR_REG_EX).matcher(line);
        Matcher binaryAfterMatcher = Pattern.compile(BINARY_AFTER_OPERATOR_REG_EX).matcher(line);
        Matcher whileMatcher = Pattern.compile(WHILE_REG_EX).matcher(line);
        Matcher endMatcher = Pattern.compile(END_REG_EX).matcher(line);
        Matcher binaryMiddleMatcher = Pattern.compile(BINARY_MIDDLE_OPERATOR_REG_EX).matcher(line);
        Matcher copyToMatcher = Pattern.compile(COPY_TO_REG_EX).matcher(line);

        return unaryMatcher.matches() || binaryAfterMatcher.matches() || whileMatcher.matches()
                || endMatcher.matches() || binaryMiddleMatcher.matches() || copyToMatcher.matches();
    }

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
