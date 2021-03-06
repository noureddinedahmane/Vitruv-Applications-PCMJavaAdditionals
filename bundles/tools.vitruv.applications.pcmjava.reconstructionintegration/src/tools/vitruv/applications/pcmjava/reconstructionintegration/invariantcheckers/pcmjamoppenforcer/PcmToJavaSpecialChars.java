package tools.vitruv.applications.pcmjava.reconstructionintegration.invariantcheckers.pcmjamoppenforcer;

import tools.vitruv.applications.pcmjava.reconstructionintegration.invariantcheckers.PcmtoJavaRenameInvariantEnforcer;

/**
 * The Class PCMtoJaMoPPSpecialChars.
 */
public class PcmToJavaSpecialChars extends PcmtoJavaRenameInvariantEnforcer {

    /*
     * (non-Javadoc)
     * 
     * @see tools.vitruv.integration.invariantChecker.PCMJaMoPPEnforcer.
     * PCMtoJaMoPPRenameInvariantEnforcer#checkForNameConflict(java.lang.String)
     */
    @Override
    protected boolean checkForNameConflict(final String a) {

        for (int i = 1; i < a.length(); i++) {
            if (!Character.isJavaIdentifierPart(a.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see tools.vitruv.integration.invariantChecker.PCMJaMoPPEnforcer.
     * PCMtoJaMoPPRenameInvariantEnforcer#renameElement(java.lang.String)
     */
    @Override
    protected String renameElement(final String element) {

        logger.info("Detected Special Character: " + element);
        final StringBuilder sb = new StringBuilder();
        sb.append(element.charAt(0)); // PreRequisite: BeginChar already done
        for (int i = 1; i < element.length(); i++) {
            if (!Character.isJavaIdentifierPart(element.charAt(i))) {
                sb.append("_" + this.renameCtr);

            } else {
                sb.append(element.charAt(i));
            }
        }
        this.renameCtr++;
        return sb.toString();
    }

}
