package glass.lattice.visitor.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import glass.ast.IMethod;
import glass.ast.IType;
import glass.lattice.model.ILatticeNode;
import glass.lattice.model.impl.Attribute;
import glass.lattice.model.impl.ExtendedRIRBuilder;
import glass.lattice.model.impl.ReverseInheritanceRelationBuilder;
import glass.lattice.visitor.AbstractVisitor;
import glass.lattice.visitor.IVisitor;

public class ComplexPurgeExtentsVisitor extends AbstractVisitor implements IVisitor{
	/**
	 * we need a reference to the builder that built the relation to have access to the local
	 * interfaces and the cumulative interfaces of the various nodes
	 */
	private ExtendedRIRBuilder relationBuilder;
	private Map<ILatticeNode, ILatticeNode> simplifiedConceptMapping;
	
	
	public ComplexPurgeExtentsVisitor (ExtendedRIRBuilder builder, Map<ILatticeNode, ILatticeNode> simplifiedConceptMapping){
		relationBuilder = builder;
		this.simplifiedConceptMapping = simplifiedConceptMapping;
	}
	
	private Set<Attribute> extractInterfaceFromNode(ILatticeNode node) {
		Set<Attribute> intentInterface = new HashSet<Attribute>();
		for (Object objIntent : node.getIntent()) {
			Attribute attr = (Attribute) objIntent;
			if (!attr.isExtendedAttribute()) {
				intentInterface.add(attr);
			}
		}
		return intentInterface;
	}
	
	private Set<Attribute> getInterfaceOutsideExtent(IType type, Set<Object> extent) {
		Set<Attribute> attributesOutsideExtent = this.relationBuilder.getLocalAttributes(type);
		IType[] superTypes = type.getAllSupertypes();
		Set<Object> superTypesSet = new HashSet<Object>(Arrays.asList(superTypes));
		Set<Object> filteredExtent = new HashSet<Object>(extent);
		filteredExtent.removeIf(typeExtent -> (superTypesSet.contains(typeExtent)));
		Set<Object> extentAndChild = new HashSet<Object>();
		extentAndChild.addAll(filteredExtent);
		// To make sure we get independent occurrences, we have to be
		// completely separated from the types in the extent that are below/unrelated to type
		for (Object objExtent : filteredExtent) {
			IType typeExtent = (IType) objExtent;
			extentAndChild.addAll(Arrays.asList(typeExtent.getAllSubtypes()));
		}
		for (IType subType : type.getDirectSubTypes()) {
			if (extentAndChild.contains(subType)) {
				continue;
			}
			attributesOutsideExtent.addAll(this.relationBuilder.getLocalAttributes(subType));
			attributesOutsideExtent.addAll(this.getInterfaceOutsideExtent(subType, extent));
		}
		return attributesOutsideExtent;
	}
	
	private Set<Object> getIntroducedAdhocAttributes(ILatticeNode node) {
		Set<Object> attrIntroduced = new HashSet<Object>();
		ILatticeNode simplifiedNode = this.simplifiedConceptMapping.get(node);
		for (Object obj : simplifiedNode.getIntent()) {
			Attribute attr = (Attribute) obj;
			if (attr.isAdhoc()) {
				attrIntroduced.add(attr);
			}
		}
		return attrIntroduced;
	}
	
	private void reduceExtent(ILatticeNode node) {

		// first, if this is the top node, exit
		if (node.getIntent().isEmpty()) return;

		Set<Object> intersection = null, extent = node.getExtent(), intent = node.getIntent();

		ArrayList<Object> classesToProcess = new ArrayList<Object>();
		classesToProcess.addAll(extent);

		// while there are still classes to process from the extent
		while (!classesToProcess.isEmpty()) {

			// first remove first element from classesToProcess
			IType nextClass = (IType) classesToProcess.remove(0);

			IType[] itsAncestors = nextClass.getAllSupertypes();

			// compute the intersection between the extent and the list of
			// ancestors
			intersection = new HashSet<Object>();
			intersection.addAll(extent);
			intersection.retainAll(Arrays.asList(itsAncestors));

			// now, go over the elements of the intersection.
			// if an element's local interface contains the intent, we should not remove
			// it because it has the intent, NOT by virtue of cumulating the interfaces
			// of its children, but has them independently, and should be counted as an
			// independent occurrence.
			// Luca 2025/11/06 : If we truly want to catch independent occurences, we should not
			// only look at the local interface, we should look at the whole interface (that we obtain
			// from reverse-inheritance),  considering only subclasses that are not in the extent. 
			// The reason for that is that an ancestor can get the intent from subclasses that are not
			// part of the extent.
			for (Object element: intersection){
				IType type = (IType) element;
				Set<Attribute> domainInterfaceNoExtent = this.getInterfaceOutsideExtent(type, extent);
				if (!domainInterfaceNoExtent.containsAll(this.extractInterfaceFromNode(node))){
					// indeed, this is the case where we need to remove the element from the extent
					extent.remove(element);
				} else {
					// just print a node
					//System.out.println("Class " + type.getElementName() + " was not purged from a node extent even though it is not a minimum. Is it an interface? "+ type.isInterface());
				}
				
				// either way, remove it from classes to process
				// classes to process
				classesToProcess.remove(element);
			}
		}
	}
	
	// TODO: debate whether or not I should also delete the attributes in the children
	// From the point of view of the intent, the attributes from above are not adhoc?
	// Makes senses to delete the extra attribute, even if the feature can be detected?
	private void deleteAttributes(ILatticeNode node, Set<Object> attrToDelete) {
		node.getIntent().removeAll(attrToDelete);
		for (ILatticeNode child : node.getChildren()) {
			this.deleteAttributes(child, attrToDelete);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ca.uqam.latece.aspects.extractor.lattice.visitors.impl.AbstractVisitor#
	 * processNode(ca.uqam.latece.aspects.extractor.lattice.model.LatticeNode)
	 */
	@Override
	/**
	 * This method purges extents from classes that are ancestors of other
	 * classes in the extent.
	 * 
	 * The way we do this: we iterate over the contents. For each class: 1) we
	 * get its ancestors 2) we compute the intersection between the ancestors
	 * and the extent 3) we remove the classes in the intersection from the
	 * extent-- and from further consideration by the method
	 * 
	 * Note that going from class to ancestors is more efficient than going from
	 * class to subclasses, for two reasons: 1) for most classes, there are more
	 * descendants than ancestors to look at 2) if the descendants of a class
	 * intersect with the extent, the only thing we know is that we must remove
	 * the class, whereas with the ancestors, I can remove all the ancestors in
	 * a single swoop
	 * 
	 * If the extent consists of a chain of n elements, by going from class to
	 * ancestors, on the average, I can hope to perform the test n/2 times. With
	 * the descendants, the worst case is the best case is n
	 */
	public void processNode(ILatticeNode node) {
		Set<Object> extentCopy = new HashSet<Object>();
		Set<Object> intentCopy = new HashSet<Object>();
		extentCopy.addAll(node.getExtent());
		intentCopy.addAll(node.getIntent());
		this.reduceExtent(node);
		// If we don't have a feature, we try to see if there are adhoc attributes introduced by the concept
		// so we can still find an interesting subfeature that we would have missed otherwise
		if (node.getExtent().size() == 1) {
			node.setExtent(extentCopy);
			Set<Object> reducedIntent = this.getIntroducedAdhocAttributes(node);
			if (reducedIntent.size() > 0) {
				node.setIntent(reducedIntent);
				this.reduceExtent(node); // Since there is at least 1 new adhoc attribute, we are guaranteed to have |extent| > 1.
				Set<Object> attrToDelete = new HashSet<Object>();
				attrToDelete.addAll(intentCopy);
				attrToDelete.removeAll(reducedIntent);
				// We can delete the attributes from the parents, because if we don't find a feature
				// with the big set of attributes, we won't find a feature in the children since they
				// contain an even bigger set of attributes (I might be wrong? -> yes I'm wrong)
				this.deleteAttributes(node, attrToDelete);
			}
		}
	}
}
