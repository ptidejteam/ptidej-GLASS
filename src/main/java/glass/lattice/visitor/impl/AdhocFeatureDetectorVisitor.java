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
	private Map<ILatticeNode, ILatticeNode> simplifiedConceptMapping;
	
	public AdhocFeatureDetectorVisitor(ILattice processedLattice, Map<ILatticeNode, ILatticeNode> simplifiedConceptMapping) {
		this.processedLattice = processedLattice;
		this.featureSemiLattice = new Lattice();
		this.adhocNodeMapping = new HashMap<ILatticeNode, Boolean>();
		this.nodeToFeatureMapping = new HashMap<ILatticeNode, ILatticeNode>();
		this.visitedNodes = new HashSet<ILatticeNode>();
		this.simplifiedConceptMapping = simplifiedConceptMapping;
	}

	@Override
	public void processNode(ILatticeNode node) {
		int extentSize = node.getExtent().size();
		if (extentSize > 1 && this.isAdhocCandidate(node)) {
			Set<Attribute> intentAttributes= new HashSet<Attribute>();
			Set<Object> intent= node.getIntent();
			for (Object obj : intent) {
				intentAttributes.add((Attribute) obj);
			}
			boolean isCandidate = true;
			int nbAdhocElement = this.countAdhoc(node);
			for (ILatticeNode candidateSuperfeature : this.getPotentialSuperfeature(node)) {
				if (candidateSuperfeature.getExtent().size() >= extentSize) { // There are edge cases where a super feature can have more occurrences
					isCandidate = false;
					break;
				}
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
	
	private int countAdhoc(ILatticeNode node) {
		int nbAdhocElement = 0;
		for (Object obj : node.getIntent()) {
			Attribute attr = (Attribute) obj;
			if (attr.isAdhoc()) {
				nbAdhocElement++;
			}
		}
		return nbAdhocElement;
	}
	private Set<Object> getAdhocElements(ILatticeNode node) {
		Set<Object> adhocElements = new HashSet<Object>();
		for (Object obj : node.getIntent()) {
			Attribute attr = (Attribute) obj;
			if (attr.isAdhoc()) {
				adhocElements.add(attr);
			}
		}
		return adhocElements;
	}
	
	private boolean isAdhocCandidate(ILatticeNode node) {
		// for a feature to be 'interesting' it has to have at 
		// least one more ad-hoc element than its 'biggest' parent,
		// or it should introduce a new adhoc attribute
		int nbIntroducedAdhocElement = this.countAdhoc(this.simplifiedConceptMapping.get(node));
		if (nbIntroducedAdhocElement > 0) {
			return true;
		}
		
		int nbAdhocElement = this.countAdhoc(node);
		int maxAdhocInParent = 0;
		Set<ILatticeNode> parents = node.getParents();
		for (ILatticeNode parent : parents) {
			int counterAdhoc = 0;
			for (Object obj : parent.getIntent()) {
				Attribute attr = (Attribute) obj;
				if (attr.isAdhoc()) {
					counterAdhoc++;
				}
			}
			if (counterAdhoc > maxAdhocInParent) {
				maxAdhocInParent = counterAdhoc;
			}
		}
		return !(nbAdhocElement==maxAdhocInParent);
	}
	
	private Set<ILatticeNode> getPotentialSuperfeature(ILatticeNode node) {
		Set<ILatticeNode> potentialSuperfeatures = new HashSet<ILatticeNode>();
		Set<Object> adhocElements = this.getAdhocElements(node);
		for (ILatticeNode child : node.getChildren()) {
			Set<Object> adhocEltChild = this.getAdhocElements(child);
			if (adhocEltChild.size()>adhocElements.size()) { 	// By definition, a child will always contain the method
				potentialSuperfeatures.add(child);				// of the concept above, so we don't need to check for that
			}
			else {
				potentialSuperfeatures.addAll(this.getPotentialSuperfeature(child));
			}
		}
		return potentialSuperfeatures;
	}

}
