package tools.vitruv.applications.pcmjava.seffstatements.pojotransformations.code2seff;

import org.palladiosimulator.pcm.repository.BasicComponent;
import org.somox.gast2seff.visitors.AbstractFunctionClassificationStrategy;
import org.somox.gast2seff.visitors.InterfaceOfExternalCallFinding;
import org.somox.gast2seff.visitors.ResourceDemandingBehaviourForClassMethodFinding;

import tools.vitruv.applications.pcmjava.seffstatements.code2seff.BasicComponentFinding;
import tools.vitruv.applications.pcmjava.seffstatements.code2seff.Code2SEFFFactory;
import tools.vitruv.framework.correspondence.CorrespondenceModel;

public class PojoJavaToPcmCodeToSeffFactory implements Code2SEFFFactory {

    @Override
    public BasicComponentFinding createBasicComponentFinding() {
        return new BasicComponentForPackageMappingFinder();
    }

    @Override
    public InterfaceOfExternalCallFinding createInterfaceOfExternalCallFinding(
            final CorrespondenceModel correspondenceModel, final BasicComponent basicComponent) {
        return new InterfaceOfExternalCallFinderForPackageMapping(correspondenceModel, basicComponent);
    }

    @Override
    public ResourceDemandingBehaviourForClassMethodFinding createResourceDemandingBehaviourForClassMethodFinding(
            final CorrespondenceModel correspondenceModel) {
        return new ResourceDemandingBehaviourForClassMethodFinderForPackageMapping(correspondenceModel);
    }

    @Override
    public AbstractFunctionClassificationStrategy createAbstractFunctionClassificationStrategy(
            final BasicComponentFinding basicComponentFinding,
            final CorrespondenceModel correspondenceModel, final BasicComponent basicComponent) {
        return new FunctionClassificationStrategyForPackageMapping(basicComponentFinding, correspondenceModel,
                basicComponent);
    }

}