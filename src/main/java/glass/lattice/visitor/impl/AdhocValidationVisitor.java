package glass.lattice.visitor.impl;

import java.util.HashSet;
import java.util.Set;

import glass.lattice.model.ILatticeNode;
import glass.lattice.model.impl.Attribute;
import glass.lattice.visitor.AbstractVisitor;
import glass.lattice.visitor.IVisitor;

public class AdhocValidationVisitor extends AbstractVisitor implements IVisitor{

	
	
	@Override
	public void processNode(ILatticeNode node) {
		Set<Object> intent = node.getIntent();
		Set<Attribute> attrIntent = new HashSet<Attribute>();
		for (Object objIntent : intent) {
			Attribute attr = (Attribute) objIntent;
			attrIntent.add(attr);
		}
		for (Attribute attr : attrIntent) {
			if (!attr.isExtendedAttribute()) {
				this.validateAdHocAttribute(attrIntent, attr);
			}
		}
	}
	
	private void validateAdHocAttribute(Set<Attribute> intent, Attribute attrToValidate) {
		for (Attribute attr : intent) {
			if (attr.isExtendedAttribute() && attr.getName().equals(attrToValidate.getName())) {
				if (attr.isRoot()) {
					attrToValidate.setAdhoc(false);
					return;
				}
			}
		}
		attrToValidate.setAdhoc(true);
	}

}
