package glass.lattice.model.impl;

import glass.ast.IMethod;
import glass.ast.IType;

public class Attribute {

	private final String name;
	private final boolean isRoot;
	private final boolean isLeaf;
	private final boolean isExtendedAttribute;
	private final IType type;
	private boolean isAdhoc = false;
	
	public Attribute(String signature, boolean isRoot, boolean isLeaf, boolean isExtendedAttribute, IType relatedType) {
		this.name = signature;
		this.isRoot = isRoot;
		this.isLeaf = isLeaf;
		this.isExtendedAttribute = isExtendedAttribute;
		this.type = relatedType;
	}
	
	public Attribute(String signature) {
		this(signature, false, false, false, null);
	}
	
	public String getName() {
		return this.name;
	}
	
	public boolean isRoot() {
		return this.isRoot;
	}
	
	public boolean isLeaf() {
		return this.isLeaf;
	}
	
	public boolean isExtendedAttribute() {
		return this.isExtendedAttribute;
	}
	
	// Todo : raise exception when type is null
	public IType getType() {
		return this.type;
	}
	
	public void setAdhoc(boolean isAdhoc) {
		this.isAdhoc = isAdhoc;
	}
	
	public boolean isAdhoc() {
		return this.isAdhoc;
	}
	
	@Override
	public String toString() {
		if (!this.isExtendedAttribute) {
			return name;
		}
		StringBuilder fullName = new StringBuilder();
		if (this.isRoot) {
			fullName.append("root ");
		}
		if (this.isLeaf) {
			fullName.append("leaf ");
		}
		fullName.append(this.type.getFullyQualifiedName() + " ");
		fullName.append(this.name);
		return fullName.toString();
	}
}
