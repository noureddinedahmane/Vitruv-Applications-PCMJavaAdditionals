package tools.vitruv.applications.pcmjava.modelrefinement.tests.instrumentation;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.text.edits.InsertEdit;
import org.emftext.language.java.members.Method;
import org.junit.Test;
import org.palladiosimulator.pcm.repository.OperationRequiredRole;
import org.palladiosimulator.pcm.repository.OperationSignature;
import org.palladiosimulator.pcm.repository.Repository;
import org.palladiosimulator.pcm.seff.InternalAction;
import org.palladiosimulator.pcm.seff.ResourceDemandingSEFF;
import org.palladiosimulator.pcm.seff.SeffFactory;
import org.somox.test.gast2seff.visitors.AssertSEFFHelper;

import tools.vitruv.applications.javaim.modelrefinement.Java2ImMethodChangeTransformationSpecification;
import tools.vitruv.applications.pcmjava.seffstatements.pojotransformations.Java2PcmWithSeffstatmantsChangePropagationSpecification;
import tools.vitruv.applications.pcmjava.tests.util.CompilationUnitManipulatorHelper;
import tools.vitruv.applications.pcmjava.tests.util.Java2PcmTransformationTest;
import tools.vitruv.framework.change.processing.ChangePropagationSpecification;
import tools.vitruv.framework.correspondence.CorrespondenceModel;
import tools.vitruv.framework.correspondence.CorrespondenceModelUtil;

public class InstrumentationTest extends Java2PcmTransformationTest{

	protected static final String MEDIA_STORE = "MediaStore";
    protected static final String WEBGUI = "WebGUI";
    protected static final String UPLOAD = "upload";
    protected static final String DOWNLOAD = "download";
 
    protected Repository repository;
    protected OperationSignature httpDownloadOpSig;
    protected OperationSignature httpUploadOpSig;
    protected OperationSignature uploadOpSig;
    protected OperationSignature downloadOpSig;
    protected OperationRequiredRole webGUIRequiresIMediaStoreRole;
    protected String webGUIPackageName;
    protected static final String MEDIA_STORE_CLASSNAME = MEDIA_STORE + "Impl";
    protected static final String WEBGUI_CLASSNAME = WEBGUI + "Impl";
    
	@Override
    protected List<ChangePropagationSpecification> createChangePropagationSpecifications() {
    	List<ChangePropagationSpecification> changePropagationSpecififactions = 
    			new ArrayList<ChangePropagationSpecification>();
    	
    	changePropagationSpecififactions.add(new Java2PcmWithSeffstatmantsChangePropagationSpecification());
    	changePropagationSpecififactions.add(new Java2ImMethodChangeTransformationSpecification());
    	
    	return changePropagationSpecififactions;
    }
	
	
	  /**
     * Set up simple media store, which can be used for the tests. It consists of two components
     * (WebGUI and MediaStore) two Interfaces (IWebGUI and IMediastore). The interfaces have two
     * methods each ((web)download and (web)upload).
     */
    @Override
    public void beforeTest() {
    	super.beforeTest();
        
        System.out.println("***************** VSUM Location: " + workspace.getAbsolutePath());
       
        try {
        	this.repository = this.createMediaStoreViaCode();     	
        } catch (Throwable t) {
        	fail("Exception during model creationg");
        }
        this.webGUIPackageName = WEBGUI;
    }
    
    
    @Test
    public void mediaStore() throws Throwable {
    	addInternalAction();
    	addLoopAction();
    	addBranchAction();
    	addInternalMethodCallWithoutExternalCall();
    }
    
    
    private void addInternalAction() throws Throwable {
    	final String code = "final int i = 5;\nfinal int j = i + 1;";
        final ResourceDemandingSEFF seff = this.editWebGUIDownloadMethod(code);
    }
    
    
    private void addLoopAction() throws Throwable{
    	final String code = "for(int k = 0; k < 10; k++){\n" + this.getExternalCallCode() + "\n" + "}";
        final ResourceDemandingSEFF seff = this.editWebGUIDownloadMethod(code);
    }
    
    
    private void addInternalMethodCallWithoutExternalCall() throws Throwable{
    	String methodBody = "int i = 0;\r\n" + 
    			"		for(int j=0; j<10; j++) {\r\n" + 
    			"			i = i * j;\r\n" + 
    			"		}\r\n";
    	
    	this.addInternalMethodToWebGUI("internalMethod", methodBody);
        final String code = "internalMethod();";
        final ResourceDemandingSEFF seff = this.editWebGUIDownloadMethod(code);
    }
    
    
    private void addBranchAction() throws Throwable{
    	final String code = "\nif(j<10){\n" + this.getExternalCallCode() + "\n}\n";
        final ResourceDemandingSEFF seff = this.editWebGUIDownloadMethod(code);
    }
    
    
    private void addInternalMethodToWebGUI(final String methodName, final String methodContent) throws Throwable {
        final String compilationUnitName = WEBGUI + "Impl";
        final String methodHeader = "private void " + methodName + "(){\n}";
        CompilationUnitManipulatorHelper.addMethodToCompilationUnit(compilationUnitName, methodHeader,
                this.getCurrentTestProject(), this);
        this.editMethod(methodContent, compilationUnitName, methodName, false);

    }
    
    
    private String getExternalCallCode() {
        final String code = "i" + MEDIA_STORE + "." + DOWNLOAD + "();";
        return code;
    }
    
    
    private ResourceDemandingSEFF editWebGUIDownloadMethod(final String code) throws Throwable, JavaModelException {
        final String compilationUnitName = WEBGUI + "Impl";
        final String methodName = "httpDownload";
        return this.editMethod(code, compilationUnitName, methodName, true);
    }
    
    
    private ResourceDemandingSEFF editMethod(final String code, final String compilationUnitName,
            final String methodName, final boolean shouldHaveCorrespndingSEFFAfterEdit)
                    throws Throwable, JavaModelException {
        final ICompilationUnit iCu = CompilationUnitManipulatorHelper
                .findICompilationUnitWithClassName(compilationUnitName, this.getCurrentTestProject());
        final IMethod iMethod = super.findIMethodByName(compilationUnitName, methodName, iCu);
        int offset = iMethod.getSourceRange().getOffset();
        offset += iMethod.getSource().length() - 2;
        final InsertEdit insertEdit = new InsertEdit(offset, code);
        
        CompilationUnitManipulatorHelper.editCompilationUnit(iCu, this, insertEdit);
        
        final CorrespondenceModel ci = this.getCorrespondenceModel();
        
        
        final Method method = super.findJaMoPPMethodInICU(iCu, methodName);

        
        final Set<ResourceDemandingSEFF> seffs = CorrespondenceModelUtil.getCorrespondingEObjectsByType(ci, method,
                ResourceDemandingSEFF.class);
        
        if (null == seffs || 0 == seffs.size()) {
            if (shouldHaveCorrespndingSEFFAfterEdit) {
                fail("could not find corresponding seff for method " + method);
            } else {
                return null;
            }
        }
        if (!shouldHaveCorrespndingSEFFAfterEdit) {
            fail("method has a corresponding seff but it should not have one: Method: " + method + " SEFF: "
                    + seffs.iterator().next());
        }
        
        return seffs.iterator().next();

    }
    
    
    protected Repository createMediaStoreViaCode() throws Throwable {
        // create main package
        final Repository repo = super.addRepoContractsAndDatatypesPackage();

        // create packages
        this.addPackageAndImplementingClass(MEDIA_STORE);
        this.addPackageAndImplementingClass(WEBGUI);

        final String webGuiInterfaceName = "I" + WEBGUI;
        final String mediaStoreInterfaceName = "I" + MEDIA_STORE;

        // create interfaces
        super.createInterfaceInPackageBasedOnJaMoPPPackageWithCorrespondence("contracts", webGuiInterfaceName);
        super.createInterfaceInPackageBasedOnJaMoPPPackageWithCorrespondence("contracts", mediaStoreInterfaceName);

        final String uploadMethodName = "upload";
        final String downloadMethodName = "download";

        final String httpDownloadMethodName = "httpDownload";
        final String httpUploadMethodName = "httpUpload";

        // create interface methods

        this.httpDownloadOpSig = super.addMethodToInterfaceWithCorrespondence(webGuiInterfaceName,
                httpDownloadMethodName);
        this.httpUploadOpSig = super.addMethodToInterfaceWithCorrespondence(webGuiInterfaceName, httpUploadMethodName);
        this.uploadOpSig = super.addMethodToInterfaceWithCorrespondence(mediaStoreInterfaceName, uploadMethodName);
        this.downloadOpSig = super.addMethodToInterfaceWithCorrespondence(mediaStoreInterfaceName, downloadMethodName);

        // create implements
        super.addImplementsCorrespondingToOperationProvidedRoleToClass(WEBGUI_CLASSNAME, webGuiInterfaceName);
        super.addImplementsCorrespondingToOperationProvidedRoleToClass(MEDIA_STORE_CLASSNAME, mediaStoreInterfaceName);

        // create class methods in component implementing classes in order to create SEFF
        // correspondences
        this.addClassMethodToClassThatOverridesInterfaceMethod(WEBGUI_CLASSNAME, httpUploadMethodName);
        this.addClassMethodToClassThatOverridesInterfaceMethod(WEBGUI_CLASSNAME, httpDownloadMethodName);
        this.addClassMethodToClassThatOverridesInterfaceMethod(MEDIA_STORE_CLASSNAME, uploadMethodName);
        this.addClassMethodToClassThatOverridesInterfaceMethod(MEDIA_STORE_CLASSNAME, downloadMethodName);

        // create requiredRole from webgui to IMediaStore
        this.webGUIRequiresIMediaStoreRole = this.addFieldToClassWithName(WEBGUI_CLASSNAME, mediaStoreInterfaceName,
                "i" + MEDIA_STORE, OperationRequiredRole.class);
        return repo;
    }

    protected void setWebGUIPackageName(final String webGUIPackageName) {
        this.webGUIPackageName = webGUIPackageName;
    }
	
	

}
