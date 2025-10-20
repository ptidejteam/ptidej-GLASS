package glass.lattice.visitor.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import glass.lattice.model.ILattice;
import glass.lattice.model.ILatticeNode;
import glass.lattice.model.impl.Attribute;
import glass.lattice.model.impl.Lattice;
import glass.lattice.model.impl.LatticeNode;
import glass.lattice.visitor.AbstractVisitor;
import glass.lattice.visitor.IVisitor;

public class AdhocFeatureDetectorVisitor extends AbstractVisitor implements IVisitor{
	
	private ILattice processedLattice;
	// TODO : proper semilattice type?
	private ILattice featureSemiLattice;
	private Map<ILatticeNode, Boolean> adhocNodeMapping;
	private Map<ILatticeNode, ILatticeNode> nodeToFeatureMapping;
	private Set<ILatticeNode> visitedNodes;
	
	public AdhocFeatureDetectorVisitor(ILattice processedLattice) {
		this.processedLattice = processedLattice;
		this.featureSemiLattice = new Lattice();
		this.adhocNodeMapping = new HashMap<ILatticeNode, Boolean>();
		this.nodeToFeatureMapping = new HashMap<ILatticeNode, ILatticeNode>();
		this.visitedNodes = new HashSet<ILatticeNode>();
	}

	@Override
	public void processNode(ILatticeNode node) {
		int extentSize = node.getExtent().size();
		if (extentSize > 1) {
			Set<Attribute> intentAttributes= new HashSet<Attribute>();
			Set<Object> intent= node.getIntent();
			for (Object obj : intent) {
				intentAttributes.add((Attribute) obj);
			}
			boolean isCandidate = false;
			final Iterator<Attribute> itAttr = intentAttributes.iterator();
			while (!isCandidate && itAttr.hasNext()) {
				isCandidate = itAttr.next().isAdhoc();
			}
			if (isCandidate) { // add feature to semi lattice
				this.adhocNodeMapping.put(node, true);
				this.nodeToFeatureMapping.put(node, node.copy());
			} else {
				this.adhocNodeMapping.put(node, false);
			}
		} else {
			this.adhocNodeMapping.put(node, false);
		}
	}
	
	private void buildFeatureSemiLattice() {
		ILatticeNode top = this.processedLattice.getTop();
		ILatticeNode featureTop = new LatticeNode();
		this.featureSemiLattice.setTop(featureTop);
		this.buildFeatureRelationshipTopDown(featureTop, top);
	}
	
	private void buildFeatureRelationshipTopDown(ILatticeNode parentFeatureNode, ILatticeNode currentNode) {
		if (this.adhocNodeMapping.get(currentNode)) {
			ILatticeNode currentFeatureNode = this.nodeToFeatureMapping.get(currentNode);
			parentFeatureNode.addChild(currentFeatureNode);
			currentFeatureNode.addParent(parentFeatureNode);
			if (visitedNodes.contains(currentNode)) {
				return;
			}
			parentFeatureNode = currentFeatureNode;
		}
		visitedNodes.add(currentNode);
		for (ILatticeNode child : currentNode.getChildren()) {
			this.buildFeatureRelationshipTopDown(parentFeatureNode, child);
		}
	}
	
	public ILattice getFeatureSemiLattice() {
		this.buildFeatureSemiLattice();
		return this.featureSemiLattice;
	}

}
