package glass.lattice.model.impl;

import java.util.Collection;
import java.util.HashMap;
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
	private Map<Object, Set<Attribute>> normalAttrMap;
	private Map<Object, Set<Attribute>> extendedAttrMap;
	private Map<Object, Set<Attribute>> inheritedAttrMap;
	private Map<Object, Set<Attribute>> reverseInheritedAttrMap;
	private Map<Object, Set<Attribute>> signatureAndTypeToInheritedAttrMap;
	
	public ExtendedRIRBuilder() {
		this.extendedRelation = new Relation();
		this.localInterfaces = new HashMap<IType, Set<String>>();
		this.signatureAttrMap = new HashMap<String, Attribute>();
		this.normalAttrMap = new HashMap<Object, Set<Attribute>>();
		this.extendedAttrMap = new HashMap<Object, Set<Attribute>>();
		this.inheritedAttrMap = new HashMap<Object, Set<Attribute>>();
		this.signatureAndTypeToInheritedAttrMap = new HashMap<Object, Set<Attribute>>();
		this.reverseInheritedAttrMap = new HashMap<Object, Set<Attribute>>();
	}

	@Override
	public IRelation buildRelationFrom(IProject project) {
		this.definedTypes = project.getDefinedTypes();
		for (IType type : this.definedTypes) {
			this.addTypeAttributesToRelation(type);
		}
		for (IType type : this.definedTypes) {
			this.inheritExtendedAttributes(type);
		}
		for (IType type : this.definedTypes) {
			this.reverseInheritAttributes(type);
		}
		
		return extendedRelation;
	}
	
	private void addToAttrMap(Object key, Attribute attr, Map<Object, Set<Attribute>> attrMap) {
		Set<Attribute> attrSet = null;
		if (!attrMap.containsKey(key)) {
			attrSet = new HashSet<Attribute>();
			attrMap.put(key, attrSet);
		} else {
			attrSet = attrMap.get(key);
		}
		attrSet.add(attr);
	}
	
	private Set<String> putLocalInterface(IType type) {
		Set<String> localInterface = new HashSet<String>();
		for (IMethod method : type.getLocalMethods()) {
			localInterface.add(method.getSignature());
		}
		return this.localInterfaces.put(type, localInterface);
	}
	
	private void addTypeAttributesToRelation(IType type) {
		this.putLocalInterface(type);
		for (IMethod method : type.getLocalMethods()) {
			String signature = method.getSignature();
			if (!this.signatureAttrMap.containsKey(signature)) {
				final Attribute attr = new Attribute(signature);
				this.signatureAttrMap.put(signature, attr);
			}
			this.extendedRelation.addRelation(type, this.signatureAttrMap.get(signature));
			this.addToAttrMap(type, this.signatureAttrMap.get(signature), this.normalAttrMap);
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
		this.addToAttrMap(type, extendedAttr, extendedAttrMap);
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
				String otherTypeSignature = itSignatures.next();
				if (signature.equals(otherTypeSignature)) {
					noDuplicates = false;
				}
			}
			i++;
		}
		return noDuplicates;
	}

	private void inheritExtendedAttributes(IType type) {
		IType[] superTypes = type.getAllSupertypes();
		Set<String> localInterface = this.localInterfaces.get(type);
		for (IType superType : superTypes) {
			if (!this.extendedAttrMap.containsKey(superType)) {
				continue;
			}
			Set<Attribute> extendedAttrSet = this.extendedAttrMap.get(superType);
			for (Attribute extendedAttr : extendedAttrSet) {
				if (localInterface.contains(extendedAttr.getName())) {
					this.addToAttrMap(type, extendedAttr, this.inheritedAttrMap);
					this.extendedRelation.addRelation(type, extendedAttr);
					this.addToAttrMap(this.hashTypeAttribute(type, extendedAttr), extendedAttr, this.signatureAndTypeToInheritedAttrMap);
				}
			}
		}
	}
	
	private void reverseInheritAttributes(IType type) {
		IType[] subTypes = type.getAllSubtypes();
		Set<String> localInterface = this.localInterfaces.get(type);
		for (IType subType : subTypes) {
			if (!this.normalAttrMap.containsKey(subType)) { // ugly :(
				continue;
			}
			Set<Attribute> subTypeNormalAttributes = this.normalAttrMap.get(subType);
			for (Attribute regularAttr : subTypeNormalAttributes) {
				this.addToAttrMap(type, regularAttr, normalAttrMap);
				this.extendedRelation.addRelation(type, regularAttr);
			}
			if (!this.extendedAttrMap.containsKey(subType)) {
				continue;
			}
			Set<Attribute> subTypeExtendedAttributes = this.extendedAttrMap.get(subType);
			for (Attribute extendedAttr : subTypeExtendedAttributes) {
				if (!localInterface.contains(extendedAttr.getName())) {
					String hashedTypeAttr = this.hashTypeAttribute(subType, extendedAttr);
					if (this.signatureAndTypeToInheritedAttrMap.containsKey(hashedTypeAttr)) {
						Set<Attribute> relatedInheritedAttr = this.signatureAndTypeToInheritedAttrMap.get(this.hashTypeAttribute(subType, extendedAttr));
						for (Attribute attr : relatedInheritedAttr) {
							this.addToAttrMap(type, attr, inheritedAttrMap);
							this.extendedRelation.addRelation(type, attr);
						}
					}
				}
				this.addToAttrMap(type, extendedAttr, extendedAttrMap);
				this.extendedRelation.addRelation(type, extendedAttr);
			}
		}
	}
	
	private String hashTypeAttribute(IType type, Attribute attr) {
		return type.getFullyQualifiedName() + attr.getName();
	}
	
	public Map<IType, Set<String>> getLocalInterfaces() {
		return this.localInterfaces;
	}
	
}
