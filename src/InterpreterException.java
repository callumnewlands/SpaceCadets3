class InterpreterException extends Exception
{

    InterpreterException() { super(); }

    InterpreterException(String errorMessage) { super(errorMessage); }

    InterpreterException(String errorMessage, Throwable e) { super(errorMessage, e); }
}

class EndOfLoopException extends InterpreterException
{
    EndOfLoopException() { super(); }

    EndOfLoopException(String errorMessage) { super(errorMessage); }

}
