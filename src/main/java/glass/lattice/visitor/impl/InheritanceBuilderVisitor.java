package glass.lattice.visitor.impl;

import java.util.HashSet;
import java.util.Set;

import glass.ast.IMethod;
import glass.ast.IType;
import glass.lattice.model.ILatticeNode;
import glass.lattice.visitor.AbstractVisitor;
import glass.lattice.visitor.IVisitor;

/**
 * Visitor used to build the inheritance concept lattice from a 'normal' lattice
 * The lattice to be transformed should be visited from BOTH the top and bottom
 * 
 * @author Luca Scistri
 */
public class InheritanceBuilderVisitor extends AbstractVisitor implements IVisitor {
	
	private Set<Object> visitedTypes;
	private Set<Object> visitedMethods;
	
	@Override
	public void processNode(ILatticeNode node) {
		Set<ILatticeNode> parents = node.getParents();
		Set<Object> currentClasses = node.getExtent();
		for (ILatticeNode parent : parents) {
			Set<Object> classes = parent.getExtent();
			Set<Object> newClasses = new HashSet<Object>();
			for (Object clasz : classes) {
				if (!currentClasses.contains(clasz)) {
					newClasses.add(clasz);
				}
			}
			parent.setExtent(newClasses);
		}
		
		Set<ILatticeNode> children = node.getChildren();
		Set<Object> currentMethods = node.getIntent();
		for(ILatticeNode child : children) {
			Set<Object> methods = child.getIntent();
			Set<Object> newMethods = new HashSet<Object>();
			for (Object method : methods) {
				if (!currentMethods.contains(method)) {
					newMethods.add(method);
				}
			}
			child.setIntent(newMethods);
		}

	/*
	@Override
	public void processNode(ILatticeNode node) {
		switch (this.getCurrentVisitDirection()) {
		case TopDown:
			// When visiting from the top, we look to delete methods
			// that we've already encountered
			Set<Object> currentMethods = node.getIntent();
			for (Object method : currentMethods) {
				if (visitedMethods.contains(method)) {
					node.removeFromIntent(method);
				} else {
					visitedMethods.add(method);
				}
			}
			break;
		case BottomUp:
			// This time, we're looking for types
			Set<Object> currentTypes = node.getExtent();
			for (Object type : currentTypes) {
				if (visitedTypes.contains(type)) {
					node.removeFromExtent(type);
				} else {
					visitedTypes.add(type);
				}
			}
		default:
			System.out.println("Problem! No direction for current traversal.");
			break;
		}
		*/
	}

}
