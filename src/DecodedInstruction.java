import java.util.regex.Matcher;
import java.util.regex.Pattern;

// TODO: document
// TODO: create subclasses to remove the ambiguty of operator1/2/dest

class DecodedInstruction
{

    private String instruction = "";
    String getInstruction() { return !instruction.equals("") ? instruction : null; }

    private String operator1 = "";
    String getOperator1() { return !operator1.equals("") ? operator1 : null; }

    private String operator2 = "";
    String getOperator2() { return !operator2.equals("") ? operator2 : null; }

    private String destination = "";
    String getDestination(){ return !destination.equals("") ? destination : null; }

    DecodedInstruction(String line)
    {
        Pattern variablePattern = Pattern.compile(Interpreter.UNARY_OPERATOR_REG_EX);
        Matcher variableMatcher = variablePattern.matcher(line);
        variableMatcher.find();

        // TODO: REDO (maybe like the GUI pattern matching with groups)

        try
        {
            instruction = variableMatcher.group(1);
            operator1 = variableMatcher.group(2);
        }
        catch (IllegalStateException e)
        {
            Pattern whilePattern = Pattern.compile(Interpreter.WHILE_REG_EX);
            Matcher whileMatcher = whilePattern.matcher(line);
            whileMatcher.find();

            try
            {
                instruction = whileMatcher.group(1);
                operator1 = whileMatcher.group(2);
            }
            catch (IllegalStateException e2)
            {
                Pattern endPattern = Pattern.compile(Interpreter.END_REG_EX);
                Matcher endMatcher = endPattern.matcher(line);
                endMatcher.find();

                try
                {
                    instruction = endMatcher.group(1);
                }
                catch (IllegalStateException e3)
                {

                    Pattern binaryPattern = Pattern.compile(Interpreter.SWAP_REG_EX);
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
                        Matcher binaryMiddleMatcher = Pattern.compile(Interpreter.BINARY_OPERATOR_REG_EX).matcher(line);
                        binaryMiddleMatcher.find();
                        try
                        {
                            instruction = binaryMiddleMatcher.group(3);
                            operator1 = binaryMiddleMatcher.group(2);
                            operator2 = binaryMiddleMatcher.group(4);
                            destination = binaryMiddleMatcher.group(1);
                        }
                        catch (IllegalStateException e5)
                        {
                            Matcher copyToMatcher = Pattern.compile(Interpreter.COPY_TO_REG_EX).matcher(line);
                            copyToMatcher.find();

                            try
                            {
                                instruction = copyToMatcher.group(1);
                                operator1 = copyToMatcher.group(2);
                                operator2 = copyToMatcher.group(3);
                            }
                            catch (IllegalStateException e6)
                            {
                                Matcher assignmentMatcher = Pattern.compile(Interpreter.ASSIGNMENT_REG_EX).matcher(line);
                                assignmentMatcher.find();
                                try
                                {
                                       destination = assignmentMatcher.group(1);
                                       operator1 = assignmentMatcher.group(3);
                                       instruction = assignmentMatcher.group(2);
                                }
                                catch (IllegalStateException e7) {

                                    Matcher ifMatcher = Pattern.compile(Interpreter.IF_REG_EX).matcher(line);
                                    ifMatcher.find();
                                    try
                                    {
                                        instruction = ifMatcher.group(1);
                                        operator1 = ifMatcher.group(2);
                                        destination = ifMatcher.group(3);
                                    }
                                    catch (IllegalStateException e8) {
                                        // There is a syntactically valid line which is not matched by the RegEx...
                                        // Should never happen
                                        operator1 = "";
                                        operator2 = "";
                                        instruction = "";
                                        destination = "";
                                    }
                                }
                            }
                        }

                    }

                }
            }
        }
    }



}
