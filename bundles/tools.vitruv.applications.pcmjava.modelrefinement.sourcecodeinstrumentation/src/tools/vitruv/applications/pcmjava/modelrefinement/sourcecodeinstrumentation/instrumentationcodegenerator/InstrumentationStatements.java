package tools.vitruv.applications.pcmjava.modelrefinement.sourcecodeinstrumentation.instrumentationcodegenerator;

import tools.vitruv.applications.pcmjava.modelrefinement.sourcecodeinstrumentation.util.CodeInstrumentationUtil;

public class InstrumentationStatements {
	
	public static MonitoringStatementInternalAction getInternalActionInstrumentationCode(String internalActionID,
			String serviceExecutionId) {
		MonitoringStatementInternalAction monitoringStatementInternalAction =  new MonitoringStatementInternalAction();
		StringBuilder beforeExecution = new StringBuilder();
		StringBuilder afterExecution = new StringBuilder();
		// Start Time variable
		String tin = "__tin_" + CodeInstrumentationUtil.getUUID();

		
		beforeExecution.append(System.lineSeparator());
		beforeExecution.append("\n final long " + tin + " = ThreadMonitoringController.getInstance().getTime();");

        afterExecution.append("\n ThreadMonitoringController.getInstance().logResponseTime(\"" + internalActionID + "\", "
        		+ " \"" + serviceExecutionId + "\"," + tin + ");");
        
        //
        monitoringStatementInternalAction.setBeforeExecution(beforeExecution.toString());
        monitoringStatementInternalAction.setAfterExecution(afterExecution.toString());
		
		return monitoringStatementInternalAction;
	}
	
	public static MonitoringStatementOperation getOperationInstrumentationCode(String serviceId, String serviceExecutionId, 
			String[] serviceParametersNames, String callerServiceExecutionId, String callerId) {	
		
		MonitoringStatementOperation monitoringStatementOperation =  new MonitoringStatementOperation();
		StringBuilder beforeExecution = new StringBuilder();
		StringBuilder afterExecution = new StringBuilder();
		
        // service parameters
        String serviceParameters = "__serviceParameters" + CodeInstrumentationUtil.getUUID();
		
		beforeExecution.append(System.lineSeparator());
		
		// service parameters
		String serviceParametersToString = serviceParamsNamesToString(serviceParametersNames, serviceParameters);
		beforeExecution.append(" \n ServiceParamtersFactory " + serviceParameters + " = new ServiceParametersFactoryImp();");
		beforeExecution.append(" \n " + serviceParametersToString + "; \n");
		beforeExecution.append("\n ThreadMonitoringController.getInstance().enterService(\"" + serviceId + "\", " + serviceParameters + ");");
		
		afterExecution.append("\n ThreadMonitoringController.getInstance().exitService();");
        //
        monitoringStatementOperation.setBeforeExecution(beforeExecution.toString());
        monitoringStatementOperation.setAfterExecution(afterExecution.toString());
		
		return monitoringStatementOperation;	
	}
	
	
	public static MonitoringStatementLoop getLoopActionInstrumentationCode(String serviceExecutionId, String loopActionId ) {
		MonitoringStatementLoop monitoringStatementLoop =  new MonitoringStatementLoop();
		String counter = "__counter_" + CodeInstrumentationUtil.getUUID();
		
		String beforeExecution = "\n long " + counter + " = 0;";
		String inBlock = "\n " + counter + "++;";
		String afterExecution = "ThreadMonitoringController.getInstance().logLoopIterationCount(\"" + serviceExecutionId + "\", "
				+ " " + counter + ");";
		
		monitoringStatementLoop.setBeforeExecution(beforeExecution);
		monitoringStatementLoop.setInBlock(inBlock);
		monitoringStatementLoop.setAfterExecution(afterExecution);
		
		return monitoringStatementLoop;
	}
	
	public static MonitoringStatementBranch getBranchActionInstrumentationCode(String serviceExecutionId, String branchId, String executedBranchId) {
		MonitoringStatementBranch monitoringStatementBranch =  new MonitoringStatementBranch();
		String executedBranch = "__executedBranch_" + CodeInstrumentationUtil.getUUID();
		
		String beforeExecution = "\n String " + executedBranch + " = null;";
		String inBlock = "\n " + executedBranch + " = " + executedBranchId + ";";
		String afterExecution = "ThreadMonitoringController.getInstance().logBranchExecution(\"" + branchId + "\", " + executedBranch + ");";
		
		
		monitoringStatementBranch.setBeforeExecution(beforeExecution);
		monitoringStatementBranch.setInBlock(inBlock);
		monitoringStatementBranch.setAfterExecution(afterExecution);
		
		return monitoringStatementBranch;
	}
	
	
	private static String serviceParamsNamesToString(String[] serviceParametersNames, String variableName) {
		if(serviceParametersNames ==  null) {
			return "\n ServiceParameters " + variableName + " ="
					+ " serviceParamtersFactory.getServiceParameters(new Object[]{})";
		}
    	String serviceParams = "{";
		for(int i = 0; i< serviceParametersNames.length; i++) {
			if(i == serviceParametersNames.length) {
				serviceParams += serviceParametersNames[i];
			}
			else {
				serviceParams += serviceParametersNames[i] + ",";
			}
		}	
		serviceParams += "}";
		
		String primitiveValueExtracter = "\n ServiceParameters " + variableName + " ="
				+ " serviceParamtersFactory.getServiceParameters(new Object[]" + serviceParams +")";
		
		return primitiveValueExtracter;
    }
	
}
