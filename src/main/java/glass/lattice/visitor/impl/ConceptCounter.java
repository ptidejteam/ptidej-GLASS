package glass.lattice.visitor.impl;

import java.util.HashSet;
import java.util.Set;

import glass.lattice.model.ILatticeNode;
import glass.lattice.visitor.AbstractVisitor;

public class ConceptCounter extends AbstractVisitor{
	
	private int count = 0;
	private Set<ILatticeNode> visitedNodes = new HashSet<ILatticeNode>();

	@Override
	public void processNode(ILatticeNode node) {
		if (this.visitedNodes.contains(node)) {
			return;
		}
		this.visitedNodes.add(node);
		this.count++;
	}

	public int getCount() {
		return this.count-1; // The top concept is not a feature
	}
}
