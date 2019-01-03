package tools.vitruv.applications.javaim.modelrefinement;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.emf.ecore.util.EcoreUtil;
import org.emftext.language.java.members.ClassMethod;
import org.emftext.language.java.members.Method;
import org.emftext.language.java.statements.Statement;
import org.emftext.language.java.statements.StatementListContainer;
import org.palladiosimulator.pcm.repository.BasicComponent;
import org.palladiosimulator.pcm.seff.AbstractAction;
import org.palladiosimulator.pcm.seff.InternalAction;
import org.palladiosimulator.pcm.seff.ResourceDemandingBehaviour;
import org.palladiosimulator.pcm.seff.SeffFactory;
import org.palladiosimulator.pcm.seff.StartAction;
import org.palladiosimulator.pcm.seff.StopAction;
import org.somox.gast2seff.visitors.AbstractFunctionClassificationStrategy;
import org.somox.gast2seff.visitors.FunctionCallClassificationVisitor;
import org.somox.gast2seff.visitors.InterfaceOfExternalCallFindingFactory;
import org.somox.gast2seff.visitors.MethodCallFinder;
import org.somox.gast2seff.visitors.ResourceDemandingBehaviourForClassMethodFinding;
import org.somox.gast2seff.visitors.VisitorUtils;
import org.somox.sourcecodedecorator.SeffElementSourceCodeLink;
import org.somox.sourcecodedecorator.SourceCodeDecoratorRepository;
import org.somox.sourcecodedecorator.SourcecodedecoratorFactory;

import tools.vitruv.applications.pcmjava.seffstatements.code2seff.BasicComponentFinding;
import tools.vitruv.applications.pcmjava.seffstatements.code2seff.Code2SeffFactory;
import tools.vitruv.framework.correspondence.CorrespondenceModel;
import tools.vitruv.framework.correspondence.CorrespondenceModelUtil;
import tools.vitruv.framework.util.bridges.CollectionBridge;

public class Java2ImInternalActionChangeTransformationImp implements Java2ImInternalActionChangeTranformation{
	private Code2SeffFactory code2SeffFactory;
	private Method newMethod;
	private CorrespondenceModel correspondenceModel;
	
	public  Java2ImInternalActionChangeTransformationImp(Code2SeffFactory code2SeffFactory,
			Method newMethod, CorrespondenceModel correspondenceModel) {
		this.code2SeffFactory = code2SeffFactory;
		this.newMethod = newMethod;	
		this.correspondenceModel = correspondenceModel;
	}
	
	public void updateCorrespondingStatements() {
		SourceCodeDecoratorRepository sourceCodeDecorator = this.getSourceCodeDecorator();
	    List<AbstractAction> oldMethodAbstractActions = this.getMethodAbstractActions();

	    int index = 0;
		for(SeffElementSourceCodeLink seffElementSourceCodeLink: sourceCodeDecorator.getSeffElementsSourceCodeLinks()) {
		    if(seffElementSourceCodeLink.getSeffElement() instanceof InternalAction) {    	
		    		AbstractAction oldAbstractAction =  oldMethodAbstractActions.get(index);
		    		List<Statement> newStatements = seffElementSourceCodeLink.getStatement();
		    		
		    		// delete the old corresponding statements in correspondence model
		    		this.removeAbstractActionsOldStatements(oldAbstractAction);
		    		
		    		// bind the new corresponding state
		    		this.bindNewStatements(oldAbstractAction, newStatements);
		    		
		    		index++;
		        }
		       
		    }		
	}
	
	
	private void bindNewStatements(AbstractAction aa, List<Statement> statements) {
		for(Statement statement: statements) {
			CorrespondenceModelUtil.createAndAddCorrespondence(this.correspondenceModel, aa, statement);
		}
	}
	
	
	private void removeAbstractActionsOldStatements(AbstractAction aa) {
		final Set<Statement> oldStatements = CorrespondenceModelUtil
				.getCorrespondingEObjectsByType(this.correspondenceModel, aa, Statement.class);
		for(Statement statement: oldStatements) {
			this.correspondenceModel.removeCorrespondencesThatInvolveAtLeastAndDependend(CollectionBridge.toSet(statement));
			EcoreUtil.remove(statement);
		}
	}
	
	
	private List<AbstractAction> getMethodAbstractActions(){
		final Set<AbstractAction> correspondingAbstractActions = CorrespondenceModelUtil
				.getCorrespondingEObjectsByType(this.correspondenceModel, this.newMethod, AbstractAction.class);
		List<AbstractAction> oldAbstractActions = new ArrayList<AbstractAction>(correspondingAbstractActions);
		List<AbstractAction> bindingAbstractActions = new ArrayList<AbstractAction>(); 
		
		for(AbstractAction aa: oldAbstractActions) {
			if(aa instanceof StartAction || aa instanceof StopAction) {
				bindingAbstractActions.add(aa);
			}
		}
		
		// delete StartAction and StopAction
		for(AbstractAction aa: bindingAbstractActions) {
			oldAbstractActions.remove(aa);
		}
		
		return oldAbstractActions;
	}
	
	
	private SourceCodeDecoratorRepository getSourceCodeDecorator(){
		 ResourceDemandingBehaviour behaviour = SeffFactory.eINSTANCE.createResourceDemandingBehaviour();
		 SourceCodeDecoratorRepository sourceCodeDecorator = SourcecodedecoratorFactory.eINSTANCE.createSourceCodeDecoratorRepository();
		 MethodCallFinder methodCallFinder = new MethodCallFinder();	
		 BasicComponentFinding basicComponentFinding = code2SeffFactory.createBasicComponentFinding();
		
		 BasicComponent myBasicComponent = basicComponentFinding.findBasicComponentForMethod(newMethod,
			correspondenceModel);
		
		 AbstractFunctionClassificationStrategy classification = code2SeffFactory.createAbstractFunctionClassificationStrategy(basicComponentFinding,
			correspondenceModel, myBasicComponent);
			
		 FunctionCallClassificationVisitor functionCallClassificationVisitor = new FunctionCallClassificationVisitor(
				classification, methodCallFinder);
		
		 InterfaceOfExternalCallFindingFactory interfaceOfExternalCallFinderFactory = code2SeffFactory.
			createInterfaceOfExternalCallFindingFactory(correspondenceModel,
				myBasicComponent);
		
		 ResourceDemandingBehaviourForClassMethodFinding resourceDemandingBehaviourForClassMethodFinding = code2SeffFactory.
			createResourceDemandingBehaviourForClassMethodFinding(correspondenceModel);
			
		if(newMethod instanceof ClassMethod){
			VisitorUtils.visitJaMoPPMethod(behaviour,
				 myBasicComponent,
				 (StatementListContainer)newMethod, 
				 sourceCodeDecorator, 
				 functionCallClassificationVisitor, 
				 interfaceOfExternalCallFinderFactory,
				 resourceDemandingBehaviourForClassMethodFinding,
				 methodCallFinder
			);
				
		}
		
		return sourceCodeDecorator;
	}

	
	
}
