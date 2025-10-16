package glass.lattice.model.impl;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import glass.ast.IMethod;
import glass.ast.IProject;
import glass.ast.IType;
import glass.lattice.model.IRelation;
import glass.lattice.model.IRelationBuilder;

public class ExtendedRIRBuilder implements IRelationBuilder {
	
	private IRelation extendedRelation;
	private Map<IType, Set<String>> localInterfaces;
	private Map<String, Attribute> signatureAttrMap;
	private Collection<IType> definedTypes;
	
	public ExtendedRIRBuilder() {
		this.extendedRelation = new Relation();
	}

	@Override
	public IRelation buildRelationFrom(IProject project) {
		this.definedTypes = project.getDefinedTypes();
		
		for (IType type : this.definedTypes) {
			this.addTypeAttributesToRelation(type);
		}
		
		return extendedRelation;
	}
	
	private Set<String> putLocalInterface(IType type) {
		Set<String> localInterface = new HashSet<String>();
		for (IMethod method : type.getLocalMethods()) {
			localInterface.add(method.getSignature());
		}
		return this.localInterfaces.put(type, localInterface);
	}
	
	private void addTypeAttributesToRelation(IType type) {
		for (IMethod method : type.getLocalMethods()) {
			String signature = method.getSignature();
			if (!this.signatureAttrMap.containsKey(signature)) {
				final Attribute attr = new Attribute(signature);
				this.signatureAttrMap.put(signature, attr);
			}
			this.extendedRelation.addRelation(type, this.signatureAttrMap.get(signature));
			this.addExtendedAttributeToRelation(type, method);
		}
	}
	
	private void addExtendedAttributeToRelation(IType type, IMethod method) {
		String signature = method.getSignature();
		IType[] subTypes = type.getAllSubtypes();
		IType[] superTypes = type.getAllSupertypes();
		boolean isRoot = this.detectDuplicateSignature(signature, subTypes);
		boolean isLeaf = this.detectDuplicateSignature(signature, superTypes);
		
		final Attribute extendedAttr = new Attribute(signature, isRoot, isLeaf, true, type);
		this.extendedRelation.addRelation(type, extendedAttr);
	}
	
	private boolean detectDuplicateSignature(String signature, IType[] otherTypes) {
		boolean noDuplicates = true;
		int i = 0;
		
		while (noDuplicates && i<otherTypes.length) {
			IType otherType = otherTypes[i];
			if (!this.localInterfaces.containsKey(otherType)) {
				this.putLocalInterface(otherType);
			}
			Iterator<String> itSignatures = this.localInterfaces.get(otherType).iterator();
			while (noDuplicates && itSignatures.hasNext()) {
				String subTypeSignature = itSignatures.next();
				if (signature.equals(subTypeSignature)) {
					noDuplicates = false;
				}
			}
			i++;
		}
		return noDuplicates;
	}

}
