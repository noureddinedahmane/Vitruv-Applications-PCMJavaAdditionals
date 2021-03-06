package tools.vitruv.applications.pcmjava.reconstructionintegration.invariantcheckers.pcmjamoppenforcer;

import tools.vitruv.applications.pcmjava.reconstructionintegration.invariantcheckers.PcmtoJavaRenameInvariantEnforcer;

/**
 * The Class PCMtoJaMoPPBeginChar.
 */
public class PcmtoJavaBeginChar extends PcmtoJavaRenameInvariantEnforcer {

    // TODO: check for all name conflicts: JLS allowed/Eclipse allowed (a vs A)
    // etc...
    /*
     * (non-Javadoc)
     * 
     * @see tools.vitruv.integration.invariantChecker.PCMJaMoPPEnforcer.
     * PCMtoJaMoPPRenameInvariantEnforcer#checkForNameConflict(java.lang.String)
     */
    @Override
    protected boolean checkForNameConflict(final String a) {
        return !Character.isJavaIdentifierStart(a.charAt(0));

        // return !a.matches("^[a-zA-Z_$]*");
        // Character.isJavaIdentifierPart(arg0)
    }

    /*
     * (non-Javadoc)
     * 
     * @see tools.vitruv.integration.invariantChecker.PCMJaMoPPEnforcer.
     * PCMtoJaMoPPRenameInvariantEnforcer#renameElement(java.lang.String)
     */
    @Override
    protected String renameElement(final String element) {

        logger.info("Detected Invalid First Character: " + element);

        final String ret = "RN" + this.renameCtr + "_" + element.substring(1);
        this.renameCtr++;
        return ret;
    }

}
