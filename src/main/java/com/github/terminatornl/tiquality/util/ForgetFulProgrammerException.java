package com.github.terminatornl.tiquality.util;

public class ForgetFulProgrammerException extends RuntimeException {

    public ForgetFulProgrammerException() {
        super("Woops! Terminator_NL forgot to update this piece of code... How silly, please report this on github!");
    }

    public ForgetFulProgrammerException(String message) {
        super("Woops! Terminator_NL forgot to update this piece of code. Message: " + message);
    }
}
