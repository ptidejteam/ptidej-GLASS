package glass.lattice.model.impl;

import glass.ast.IMethod;
import glass.ast.IProject;
import glass.ast.IType;
import glass.lattice.model.IRelation;
import glass.lattice.model.IRelationBuilder;

public class ExtendedRIRBuilder implements IRelationBuilder {
	
	
	private IRelationBuilder RIRBuilder;
	
	public ExtendedRIRBuilder(IRelationBuilder builder) {
		this.RIRBuilder = builder;
	}

	@Override
	public IRelation buildRelationFrom(IProject project) {
		IRelation extendedRelation = RIRBuilder.buildRelationFrom(project);
		for (Object objType : extendedRelation.getDomain()) {
			IType type = (IType) objType;
			for (IMethod method : type.getLocalMethods()) {
				extendedRelation.addRelation(type, method.getFullSignature());
			}
		}
		return extendedRelation;
	}

}
