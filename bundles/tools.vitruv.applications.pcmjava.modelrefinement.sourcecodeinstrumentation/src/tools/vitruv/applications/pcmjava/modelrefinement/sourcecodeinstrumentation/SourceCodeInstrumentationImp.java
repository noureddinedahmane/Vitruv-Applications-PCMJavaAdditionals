package tools.vitruv.applications.pcmjava.modelrefinement.sourcecodeinstrumentation;


import tools.vitruv.applications.pcmjava.modelrefinement.sourcecodeinstrumentation.instrumentationcodegenerator.InstrumentationStatements;
import tools.vitruv.applications.pcmjava.modelrefinement.sourcecodeinstrumentation.instrumentationcodegenerator.MonitoringStatementBranch;
import tools.vitruv.applications.pcmjava.modelrefinement.sourcecodeinstrumentation.instrumentationcodegenerator.MonitoringStatementInternalAction;
import tools.vitruv.applications.pcmjava.modelrefinement.sourcecodeinstrumentation.instrumentationcodegenerator.MonitoringStatementLoop;
import tools.vitruv.applications.pcmjava.modelrefinement.sourcecodeinstrumentation.instrumentationcodegenerator.MonitoringStatementOperation;
import tools.vitruv.applications.pcmjava.modelrefinement.sourcecodeinstrumentation.jamoppplaincodeparser.JamoppPlainCodeParser;
import tools.vitruv.applications.pcmjava.modelrefinement.sourcecodeinstrumentation.probesprovider.ProbesProvider;
import tools.vitruv.applications.pcmjava.modelrefinement.sourcecodeinstrumentation.projectmanager.ProjectManager;
import tools.vitruv.applications.pcmjava.modelrefinement.sourcecodeinstrumentation.util.CodeInstrumentationUtil;
import tools.vitruv.framework.correspondence.Correspondence;
import tools.vitruv.framework.correspondence.CorrespondenceModel;
import tools.vitruv.framework.correspondence.CorrespondenceModelUtil;


import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.jdt.core.IJavaProject;
import org.emftext.language.java.classifiers.ConcreteClassifier;
import org.emftext.language.java.containers.impl.CompilationUnitImpl;
import org.emftext.language.java.members.ClassMethod;
import org.emftext.language.java.members.Method;
import org.emftext.language.java.parameters.Parameter;
import org.emftext.language.java.statements.Return;
import org.emftext.language.java.statements.Statement;
import org.palladiosimulator.pcm.seff.AbstractAction;
import org.palladiosimulator.pcm.seff.BranchAction;
import org.palladiosimulator.pcm.seff.InternalAction;
import org.palladiosimulator.pcm.seff.LoopAction;
import org.palladiosimulator.pcm.seff.ResourceDemandingBehaviour;
import org.palladiosimulator.pcm.seff.ResourceDemandingSEFF;

import de.uka.ipd.sdq.identifier.Identifier;

public class SourceCodeInstrumentationImp implements SourceCodeInstrumentation{
	private static final Logger logger = Logger.getLogger(SourceCodeInstrumentationImp.class.getSimpleName());
	
    private Map<AbstractAction, List<Statement>> probesToStatements;
    private List<Method> instrumentedMethods;
    private CorrespondenceModel correspondenceModel;
    private IJavaProject iProject;
    private ProbesProvider probesProvider;
	
	public SourceCodeInstrumentationImp(CorrespondenceModel correspondenceModel, IJavaProject iProject,
			ProbesProvider probesProvider) {
		this.probesToStatements =  new HashMap<AbstractAction, List<Statement>>();
		this.instrumentedMethods = new ArrayList<Method>();
		this.iProject = iProject;
		this.probesProvider = probesProvider;
		this.correspondenceModel = correspondenceModel;
	}
	
	
	@Override
	public void execute() throws IOException {
		// 1- copy the current project
		// FIXME: uncomment: this is not needed in the tests, because Vitruv already
		//  a java project for every test
		//ProjectManager.copyProject(projectName)
		
		// 2- load the resource from copied project 
		ResourceSet resourceSet = CodeInstrumentationUtil.extractSoftwareModelFromProjects(this.iProject);
		
		// 3- get the list of probes from correspondence model
		List<AbstractAction> listProbes = this.probesProvider.getListProbes();
		
		// 4- find the corresponding statements of the abstract actions in the copied project
		this.getProbesStatement(listProbes, this.correspondenceModel, resourceSet);
		
		// 5- instrumentation
		this.instrumentSourceCode();
		
	}
	
	
	
	private  void getProbesStatement(List<AbstractAction> listProbes, CorrespondenceModel ci, ResourceSet resourceSet) {
		for(AbstractAction aa : listProbes) {
			// get the corresponding Statements of the probe from corresponding model
			Set<Statement> originalStatements =
					CorrespondenceModelUtil.getCorrespondingEObjectsByType(ci, aa, Statement.class);
			
			if(originalStatements.size() == 0) 
				continue;
			
			Statement statement = originalStatements.iterator().next(); 
			String statementMethodName = statement.getParentByType(Method.class).getName();
			String statementClassName = statement.getParentByType(ConcreteClassifier.class).getName();
			
			// method in the cloned project
			ClassMethod clonedMethod = this.findCorrespondingMethod(statementMethodName, statementClassName, resourceSet);
			
			if(clonedMethod != null) {
				List<Statement> clonedStatements = clonedMethod.getStatements();
				List<Statement> statementsToMap = this.findSimilarStatements(originalStatements, clonedStatements);
				this.probesToStatements.put(aa, statementsToMap);
			}
		}
		
	}
		
	
	private List<Statement> findSimilarStatements(Set<Statement> originalStatements, List<Statement> clonedStatements){
		List<Statement> statementsToMap = new ArrayList<Statement>();
		
		for(Statement originalStatement: originalStatements) {
			for(Statement clonedStatement: clonedStatements) {
				if(CodeInstrumentationUtil.compareStatements(originalStatement, clonedStatement)) {
					statementsToMap.add(clonedStatement);
				}
			}
		}
		
		return statementsToMap;
	}
	
	
	private ClassMethod findCorrespondingMethod(String methodName, String className, ResourceSet resourceSet) {
		for(Resource resource: resourceSet.getResources()) {
			if(resource.getContents().get(0) instanceof  CompilationUnitImpl) {
				CompilationUnitImpl compilationUnit = (CompilationUnitImpl) resource.getContents().get(0);
				ConcreteClassifier clazz = CodeInstrumentationUtil.findConcreteClassifierWithName(compilationUnit, className);
				if(clazz != null) {
					Method method = CodeInstrumentationUtil.findMethodByName(clazz, methodName);
					if(method != null) {
						return (ClassMethod)method;
					}
				}
			}
		}
		
		return null;
	}
	
	
    
	private void instrumentSourceCode() throws IOException { 		
		for (Map.Entry<AbstractAction, List<Statement>> entry : this.probesToStatements.entrySet()){
			AbstractAction aa = entry.getKey();
			List<Statement> statements = entry.getValue();

			// instrument the method if not yet instrumented
			instrumentOperation((ClassMethod)statements.get(0).getParentByType(Method.class));
			
			// instrumented based on the abstract action type
			if(aa instanceof InternalAction) {
    			MonitoringStatementInternalAction internalActionProbe = 
						InstrumentationStatements.getInternalActionInstrumentationCode(aa.getId(), null);
				
				JamoppPlainCodeParser.addBeforeContainingStatement(statements.get(0), 
						internalActionProbe.getBeforeExecution());
				
				JamoppPlainCodeParser.addAfterContainingStatement(statements.get(statements.size() - 1),
						internalActionProbe.getAfterExecution());
    		}
    		else if(aa instanceof BranchAction) {
    			Statement branchFirstChildStatement = 
   						CodeInstrumentationUtil.getbranchLoopFirstChildStatement(statements.get(0));
    			
    			if(branchFirstChildStatement == null) {
    				continue;
    			}
    			
    			MonitoringStatementBranch branchActionProbe = 
               		 InstrumentationStatements.getBranchActionInstrumentationCode(null, aa.getId(), null);
   				
   				JamoppPlainCodeParser.addBeforeContainingStatement(statements.get(0), 
   						branchActionProbe.getBeforeExecution());
   				
   				JamoppPlainCodeParser.addBeforeContainingStatement(branchFirstChildStatement,
   						branchActionProbe.getInBlock());
   				
   				JamoppPlainCodeParser.addAfterContainingStatement(statements.get(0), 
   						branchActionProbe.getAfterExecution());
    		}
    		else if(aa instanceof LoopAction) {
    			Statement loopFirstChildStatement = 
						CodeInstrumentationUtil.getbranchLoopFirstChildStatement(statements.get(0));
    			
    			if(loopFirstChildStatement == null) {
    				continue;
    			}
    			
    			MonitoringStatementLoop loopActionProbe = 
						InstrumentationStatements.getLoopActionInstrumentationCode(aa.getId(), null);	
				
				JamoppPlainCodeParser.addBeforeContainingStatement(statements.get(0), 
						loopActionProbe.getBeforeExecution());
				
				JamoppPlainCodeParser.addBeforeContainingStatement((Statement)loopFirstChildStatement,
						loopActionProbe.getInBlock());
				
				JamoppPlainCodeParser.addAfterContainingStatement(statements.get(0), 
						loopActionProbe.getAfterExecution());
    		}
			
			// show the modified code...
			ClassMethod method = statements.get(0).getParentByType(ClassMethod.class);
			this.saveResource(method);
			System.out.println("-------Instrumented code: ");
			System.out.println(showMethodCode(method));
			
			
		}
        	
	}
	
	
   private  List<AbstractAction> getAllAbstractAction(CorrespondenceModel ci){
    	List<AbstractAction> listAbstractionAction = new ArrayList<AbstractAction>();
    	
    	Set<Correspondence> allCorrespondences = ci.getAllCorrespondencesWithoutDependencies();
    	for(Correspondence correspondence: allCorrespondences) {
        	if(isProbe(correspondence.getAs().get(0))) {
        		listAbstractionAction.add((AbstractAction) correspondence.getAs().get(0));
        	}
        }
    
        return listAbstractionAction.stream().distinct().collect(Collectors.toList());
    }
    
    
    private static boolean isProbe(EObject aa) {
    	if(aa instanceof InternalAction) {
    		return true;
    	}
    	else if(aa instanceof LoopAction) {
    		return true;
    	}
    	else if(aa instanceof BranchAction) {
    		return true;
    	}
    	else {
    		return false;
    	}
    
    }
    
    
    private static List<Statement> getAbstractActionStatements(AbstractAction aa, CorrespondenceModel ci) {
		Set<Statement> listStatements =  CorrespondenceModelUtil.getCorrespondingEObjectsByType(ci, aa, Statement.class);
        return new ArrayList<Statement>(listStatements);  
    }
	    
	
	private  void instrumentOperation(Method method) throws IOException {
		if(this.instrumentedMethods.contains(method)) {
			return;
		}
        
		List<Statement> statements = ((ClassMethod) method).getStatements();
		if(statements.size() != 0) {
            String[] serviceParametersNames = getSeviceParamsNames((ClassMethod) method);
          
			// get service ID
			String serviceSeffId = this.getMethodSeffId(method);
			
			MonitoringStatementOperation operation = 
					InstrumentationStatements.getOperationInstrumentationCode(serviceSeffId, serviceParametersNames);
			
			Statement firstStatement = statements.get(0);
			Statement lastStatement = statements.get(statements.size() - 1);
			
			JamoppPlainCodeParser.addBeforeContainingStatement(firstStatement, operation.getBeforeExecution());
			if(lastStatement instanceof Return) {
				JamoppPlainCodeParser.addBeforeContainingStatement(lastStatement, operation.getAfterExecution());
			}
			else {
				JamoppPlainCodeParser.addAfterContainingStatement(lastStatement, operation.getAfterExecution());
			}
			
		}
		
		// mark the method as instrumented
		this.instrumentedMethods.add(method);
	}
	
	
	private String getMethodSeffId(Method method) {
    	final Set<ResourceDemandingSEFF> seffs = CorrespondenceModelUtil.getCorrespondingEObjectsByType(this.correspondenceModel, method,
                ResourceDemandingSEFF.class);   
        if (seffs != null) {
          if(seffs.size() != 0) {
        	  return seffs.iterator().next().getId();
          }
          else {
        	  return null;
          }
        }
        else {
        	return null;
        }
    }

	
	private static String[] getSeviceParamsNames(ClassMethod classMethod) {
    	List<Parameter> params = classMethod.getParameters();
    	String[] serviceParametersNames = new String[params.size()];
    	int i = 0;
		for(Parameter param: params) {
			serviceParametersNames[i] = param.getName();
			i++;
		}	
		return serviceParametersNames;
    }
	
	
	public static AbstractAction getAbstractAction(Identifier identifier) {
		if(identifier instanceof AbstractAction) {
			return (AbstractAction)identifier;
		}
		else if(identifier instanceof ResourceDemandingBehaviour) {
			ResourceDemandingBehaviour resourceDemandingBehabiour = (ResourceDemandingBehaviour) identifier;
			return (AbstractAction)resourceDemandingBehabiour.getAbstractBranchTransition_ResourceDemandingBehaviour()
					.getBranchAction_AbstractBranchTransition();
		}else {
			return null;
		}
	}
	
	
	public static void createJavaFileSource(String code, String fileName) throws IOException {
	    BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
	    writer.write(code);  
	    writer.close();
	}
	
	private void saveResource(Method method) {
		Resource resource = method.eResource();
		try {
			resource.save(Collections.EMPTY_MAP);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static String showMethodCode(Method method) {
		  Resource resource = method.eResource(); 
	      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			try {
				resource.save(outputStream, null);
			} catch (IOException e) {
				System.out.println(e.getMessage());
			}
			
			return outputStream.toString();			

	}

	
}
 