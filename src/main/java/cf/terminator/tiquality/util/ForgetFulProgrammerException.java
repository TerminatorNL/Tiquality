package cf.terminator.tiquality.util;

public class ForgetFulProgrammerException extends RuntimeException {

    public ForgetFulProgrammerException(){
        super("Woops! The TiqualityCommand author forgot to reloadFromFile this piece of code... How silly, please report this to Terminator_NL on github!");
    }
}
