package tools.vitruv.application.pcmjava.modelrefinement.tests.primitiveattributeextraction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import tools.vitruv.application.pcmjava.modelrefinement.monitoring.primitiveattributeextraction.PrimitiveAttributeExtracter;
import tools.vitruv.application.pcmjava.modelrefinement.monitoring.primitiveattributeextraction.PrimitiveAttributeExtracterImp;


public class Test {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		TestEntity entity = new TestEntity();	
		PrimitiveAttributeExtracter pv =  new PrimitiveAttributeExtracterImp() ;

		
		 
		for (Entry<String, Object> entry : pv.getObjectPrimitiveAttributeValues(new Object[]{entity}, 1).entrySet())
		{
		    //System.out.println(entry.getKey() + ": " + entry.getValue());
		    if(isDouble(entry.getValue())) {
		    	System.out.println("---------> it is Double: " + entry.getValue());
		    }
		    else {
		    	System.out.println("---------> it is Not Double: " + entry.getValue());
		    }
		}
		
		

	}
	
	public static boolean isDouble(Object obj) {
		try {
			Double.parseDouble(obj.toString());
			return true;
		}catch(Exception e) {
			System.out.println(e.getMessage());
			return false;
		}
	}
	

}
