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

    private static final String[] RESERVED_IDENTIFIERS = {"clear", "decr", "do", "end", "incr", "while"};
    public static final String COMMENT_REG_EX = "\\s*#.*";
    private static final String VARIABLE_REG_EX = "\\s*(clear|incr|decr)\\s+(\\w+)\\s*;\\s*";
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

        Pattern variablePattern = Pattern.compile(VARIABLE_REG_EX);
        Matcher variableMatcher = variablePattern.matcher(line);
        variableMatcher.find();

        // TODO: redo like GUI pattern matching with groups

        String instruction = "";
        String variable = "";
        try
        {
            instruction = variableMatcher.group(1);
            variable = variableMatcher.group(2);
        }
        catch (IllegalStateException e)
        {
            Pattern whilePattern = Pattern.compile(WHILE_REG_EX);
            Matcher whileMatcher = whilePattern.matcher(line);
            whileMatcher.find();

            try
            {
                instruction = whileMatcher.group(1);
                variable = whileMatcher.group(2);
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
                    instruction = "";
                    variable = "";
                }
            }
        }

        if (isReservedIdentifier(variable))
            throw new InterpreterException("Syntax Error in line " + (_linePtr + 1) + ": " + variable + " is a reserved keyword");

        switch (instruction)
        {
            case "clear":
                clear(variable);
                break;
            case "incr":
                increment(variable);
                break;
            case "decr":
                decrement(variable);
                break;
            case "while":
                whileLoopPtrs.push(new WhileLoopPtr(_linePtr));
                while (_variables.get(variable) > 0)
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
        Pattern variablePattern = Pattern.compile(VARIABLE_REG_EX);
        Matcher variableMatcher = variablePattern.matcher(line);

        Pattern whilePattern = Pattern.compile(WHILE_REG_EX);
        Matcher whileMatcher = whilePattern.matcher(line);

        Pattern endPattern = Pattern.compile(END_REG_EX);
        Matcher endMatcher = endPattern.matcher(line);


        return variableMatcher.matches() || whileMatcher.matches() || endMatcher.matches();
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
