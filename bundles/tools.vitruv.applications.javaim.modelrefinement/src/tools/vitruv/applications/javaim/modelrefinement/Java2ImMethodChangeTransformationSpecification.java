package tools.vitruv.applications.javaim.modelrefinement;

import tools.vitruv.applications.pcmjava.seffstatements.pojotransformations.code2seff.PojoJava2PcmCodeToSeffFactory;

public class Java2ImMethodChangeTransformationSpecification extends Java2ImMethodBodyChangeTransformation{

	public Java2ImMethodChangeTransformationSpecification() {
		super(new PojoJava2PcmCodeToSeffFactory());
	}

}
