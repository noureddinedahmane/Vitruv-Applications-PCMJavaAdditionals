package edu.kit.ipd.sdq.vitruvius.framework.model.monitor.classification.postreconcile;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import edu.kit.ipd.sdq.vitruvius.framework.model.monitor.events.ChangeClassifyingEvent;
import edu.kit.ipd.sdq.vitruvius.framework.model.monitor.events.ChangeMethodModifiersEvent;
import edu.kit.ipd.sdq.vitruvius.framework.model.monitor.util.CompilationUnitUtil;
import edu.kit.ipd.sdq.vitruvius.framework.model.monitor.util.JavaModel2AST;

public class ChangeMethodModifiersClassifier extends SingleNodeChangeClassifier {

    @Override
    protected List<? extends ChangeClassifyingEvent> classifyChange(IJavaElementDelta delta,
            CompilationUnit currentCompilationUnit, CompilationUnit oldCompilationUnit) {
        List<ChangeMethodModifiersEvent> returns = new ArrayList<ChangeMethodModifiersEvent>(1);
        IJavaElement element = delta.getElement();
        if (element.getElementType() == IJavaElement.METHOD && delta.getKind() == IJavaElementDelta.CHANGED
                && (delta.getFlags() & IJavaElementDelta.F_MODIFIERS) != 0) {
            IMethod imethod = (IMethod) element;
            IType itype = (IType) imethod.getParent();
            int line = CompilationUnitUtil.getLineNumberOfMethod(imethod, itype.getElementName().toString(),
                    currentCompilationUnit);
            MethodDeclaration changed = JavaModel2AST.getMethodDeclaration((IMethod) element, currentCompilationUnit);
            MethodDeclaration original = CompilationUnitUtil.findMethodDeclarationOnLine(line, oldCompilationUnit);
            if (changed != null && original != null)
                returns.add(new ChangeMethodModifiersEvent(original, changed, line));
        }
        return returns;
    }
}