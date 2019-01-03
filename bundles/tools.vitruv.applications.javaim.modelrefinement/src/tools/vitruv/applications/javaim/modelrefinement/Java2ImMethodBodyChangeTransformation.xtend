package tools.vitruv.applications.javaim.modelrefinement

import java.util.ArrayList
import org.emftext.language.java.members.Method
import org.emftext.language.java.statements.Statement
import org.emftext.language.java.statements.StatementsPackage
import tools.vitruv.applications.pcmjava.seffstatements.code2seff.Code2SeffFactory
import tools.vitruv.domains.im.ImDomainProvider
import tools.vitruv.domains.java.JavaDomainProvider
import tools.vitruv.domains.java.echange.feature.JavaFeatureEChange
import tools.vitruv.framework.change.description.CompositeTransactionalChange
import tools.vitruv.framework.change.description.ConcreteChange
import tools.vitruv.framework.change.description.TransactionalChange
import tools.vitruv.framework.change.echange.feature.reference.InsertEReference
import tools.vitruv.framework.change.echange.feature.reference.RemoveEReference
import tools.vitruv.framework.change.echange.feature.reference.UpdateReferenceEChange
import tools.vitruv.framework.change.processing.impl.AbstractChangePropagationSpecification
import tools.vitruv.framework.correspondence.CorrespondenceModel
import tools.vitruv.framework.userinteraction.UserInteractor
import tools.vitruv.framework.util.command.ResourceAccess

class Java2ImMethodBodyChangeTransformation extends AbstractChangePropagationSpecification {
	val Code2SeffFactory code2SeffFactory
	new(Code2SeffFactory code2SEFFfactory) {
		super(new JavaDomainProvider().domain, new ImDomainProvider().domain)
		this.code2SeffFactory = code2SEFFfactory
	}
	
	
	override propagateChange(TransactionalChange change, CorrespondenceModel correspondenceModel, ResourceAccess resourceAccess) {
		if (doesHandleChange(change, correspondenceModel)) {
			val compositeChange = change as CompositeTransactionalChange;
			executeJava2ImTransformation(correspondenceModel, userInteractor, compositeChange, false)
		}
		else if(isOnlyInternalActionBodyChange(change, correspondenceModel)){
			// apply a transformation that only update the correspondence model
			val compositeChange = change as CompositeTransactionalChange;
			executeJava2ImTransformation(correspondenceModel, userInteractor, compositeChange, true)
		}
	}
	
	
	/**
	 * return true if a statement has been added or deleted
	 */
	def isOnlyInternalActionBodyChange(TransactionalChange change, CorrespondenceModel correspondenceModel){
		return false
	}
	
	
	override doesHandleChange(TransactionalChange change, CorrespondenceModel correspondenceModel) {
		if (!(change instanceof CompositeTransactionalChange)) {
			return false;
		} 
		val eChanges = new ArrayList<JavaFeatureEChange<?, ?>>();
		for (eChange : change.EChanges) {
			if (eChange instanceof UpdateReferenceEChange<?>) {
				if (eChange.isContainment) {
					if (eChange instanceof JavaFeatureEChange<?, ?>) {
						val typedChange = eChange as JavaFeatureEChange<?, ?>;
						eChanges += typedChange;
					}
				}
			}
		}
		if (eChanges.size != change.EChanges.size) {
			return false
		}

		val firstChange = eChanges.get(0);
		if (!(firstChange.oldAffectedEObject instanceof Method) || !(firstChange.affectedEObject instanceof Method)) {
			return false
		}

		if (!eChanges.forall[affectedFeature == StatementsPackage.eINSTANCE.statementListContainer_Statements]) {
			return false
		}

		if (!eChanges.forall[affectedEObject == firstChange.affectedEObject]) {
			return false
		}

		if (!eChanges.forall[oldAffectedEObject == firstChange.oldAffectedEObject]) {
			return false
		}

		val deleteChanges = new ArrayList<RemoveEReference<?, ?>>;
		eChanges.forEach [
			if (it instanceof RemoveEReference<?, ?>) {
				val typedChange = it as RemoveEReference<?, ?>;
				deleteChanges += typedChange
			}
		]
		val addChanges = new ArrayList<InsertEReference<?, ?>>;
		eChanges.forEach [
			if (it instanceof InsertEReference<?, ?>) {
				val typedChange = it as InsertEReference<?, ?>;
				addChanges += typedChange
			}
		]

		if (!deleteChanges.forall[oldValue instanceof Statement]) {
			return false
		}

		if (!addChanges.forall[newValue instanceof Statement]) {
			return false
		}
		
		return true
	}
	
	
	def executeJava2ImTransformation(CorrespondenceModel correspondenceModel,
		UserInteractor userInteracting, CompositeTransactionalChange compositeChange,
		boolean onlyInternalActionsUpdate ){
			
		val ConcreteChange emfChange = compositeChange.getChanges().get(0) as ConcreteChange;
		val JavaFeatureEChange<?, ?> eFeatureChange = emfChange.getEChanges().get(0) as JavaFeatureEChange<?, ?>;
		
		val oldMethod = eFeatureChange.getOldAffectedEObject() as Method;
		val newMethod = eFeatureChange.getAffectedEObject() as Method;
		
		if(onlyInternalActionsUpdate){
			val Java2ImInternalActionChangeTransformationImp java2Im =
		                new Java2ImInternalActionChangeTransformationImp(code2SeffFactory, newMethod, correspondenceModel)
		    java2Im.updateCorrespondingStatements()
		}
		else{
			Java2ImMethodChangeTransformationUtil.execute(correspondenceModel, oldMethod, newMethod)
		}	

	}
	
	
}