package glass.lattice.visitor.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import glass.ast.IMethod;
import glass.ast.IType;
import glass.lattice.model.ILattice;
import glass.lattice.model.ILatticeNode;
import glass.lattice.model.impl.Lattice;
import glass.lattice.visitor.AbstractVisitor;
import glass.lattice.visitor.IVisitor;

/**
 * Visitor used to build the inheritance concept lattice from a 'normal' lattice
 * Some optimizations could (probably) still be done as we traverse the original lattice many times
 * 
 * @author Luca Scistri
 */
public class InheritanceBuilderVisitor extends AbstractVisitor implements IVisitor {
	
	private ILattice resultInheritanceLattice;
	private Map<ILatticeNode, ILatticeNode> originalToCloneMapping;
	private ILattice originalLattice;
	
	
	public InheritanceBuilderVisitor(ILattice originalLattice) {
		this.resultInheritanceLattice = new Lattice();
		this.originalLattice = originalLattice;
		this.originalToCloneMapping = new HashMap<ILatticeNode, ILatticeNode>();
	}
	
	@Override
	public void processNode(ILatticeNode node) {
		ILatticeNode cloneCurrentNode = node.copy();
		this.originalToCloneMapping.put(node, cloneCurrentNode);
		
		Set<Object> clonedMethods = cloneCurrentNode.getIntent();
		Set<Object> clonedClasses = cloneCurrentNode.getExtent();
		
		for (ILatticeNode parent : node.getParents()) {
			for (Object method : parent.getIntent()) {
				if (clonedMethods.contains(method)) {
					clonedMethods.remove(method);
				}
			}
		}
		for (ILatticeNode child : node.getChildren()) {
			for (Object clasz : child.getExtent()) {
				if (clonedClasses.contains(clasz)) {
					clonedClasses.remove(clasz);
				}
			}
		}
		
		cloneCurrentNode.setIntent(clonedMethods);
		cloneCurrentNode.setExtent(clonedClasses);
	}
	
	private void buildInheritanceLattice() {
		ILatticeNode top = this.originalLattice.getTop();
		ILatticeNode clonedTop = this.originalToCloneMapping.get(top);
		this.resultInheritanceLattice.setTop(clonedTop);
		
		ILatticeNode bottom = this.originalLattice.getBottom();
		ILatticeNode clonedBottom = this.originalToCloneMapping.get(bottom);
		this.resultInheritanceLattice.setBottom(clonedBottom);

		for (ILatticeNode child : top.getChildren()) {
			buildChildrenRelation(child, top);
		}
		for (ILatticeNode parent : bottom.getParents()) {
			buildParentRelation(parent, bottom);
		}
		
	}
	
	private void buildChildrenRelation(ILatticeNode node, ILatticeNode parent) {
		ILatticeNode clonedParent = this.originalToCloneMapping.get(parent);
		ILatticeNode clonedNode = this.originalToCloneMapping.get(node);
		clonedParent.addChild(clonedNode);
		for (ILatticeNode child : node.getChildren()) {
			buildChildrenRelation(child, node);
		}
	}
	
	private void buildParentRelation(ILatticeNode node, ILatticeNode child) {
		ILatticeNode clonedChild = this.originalToCloneMapping.get(child);
		ILatticeNode clonedNode = this.originalToCloneMapping.get(node);
		clonedChild.addParent(clonedNode);
		for (ILatticeNode parent : node.getParents()) {
			buildParentRelation(parent, node);
		}
	}
	
	public ILattice getInheritanceLattice() {
		this.buildInheritanceLattice();
		return this.resultInheritanceLattice;
	}

}
