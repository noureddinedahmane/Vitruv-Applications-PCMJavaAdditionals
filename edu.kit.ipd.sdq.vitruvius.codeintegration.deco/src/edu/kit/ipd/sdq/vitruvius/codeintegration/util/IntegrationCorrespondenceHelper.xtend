package edu.kit.ipd.sdq.vitruvius.codeintegration.util

import edu.kit.ipd.sdq.vitruvius.codeintegration.deco.meta.correspondence.integration.IntegrationCorrespondence
import edu.kit.ipd.sdq.vitruvius.codeintegration.deco.meta.correspondence.integration.IntegrationFactory
import edu.kit.ipd.sdq.vitruvius.framework.contracts.datatypes.GenericCorrespondenceModel
import edu.kit.ipd.sdq.vitruvius.framework.contracts.datatypes.CorrespondenceModel

class IntegrationCorrespondenceHelper {
	private new() {
	}

	def static final GenericCorrespondenceModel<IntegrationCorrespondence> getEditableView(
		CorrespondenceModel ci) {
		return ci.getEditableView(typeof(IntegrationCorrespondence), [createIntegrationCorrespondence])
	}

	def static final IntegrationCorrespondence createIntegrationCorrespondence() {
		val integratedCorrespondence = IntegrationFactory::eINSTANCE.
			createIntegrationCorrespondence()
		integratedCorrespondence.setCreatedByIntegration(true)
		return integratedCorrespondence
	}
}
