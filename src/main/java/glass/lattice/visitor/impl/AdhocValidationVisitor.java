package glass.lattice.visitor.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import glass.lattice.model.ILatticeNode;
import glass.lattice.model.impl.Attribute;
import glass.lattice.visitor.AbstractVisitor;
import glass.lattice.visitor.IVisitor;

public class AdhocValidationVisitor extends AbstractVisitor implements IVisitor{

	private int adhocCounter = 0;
	private Set<String> blackListAdhoc;
	
	public AdhocValidationVisitor() {
		this.initBlackList();
	}
	
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
		if (this.blackListAdhoc.contains(attrToValidate.getName())) {
			attrToValidate.setAdhoc(false);
			return;
		}
		for (Attribute attr : intent) {
			if (attr.isExtendedAttribute() && attr.getName().equals(attrToValidate.getName())) {
				if (attr.isRoot()) {
					attrToValidate.setAdhoc(false);
					return;
				}
			}
		}
		attrToValidate.setAdhoc(true);
		adhocCounter++;
	}

	public int getAdhocCounter() {
		return this.adhocCounter;
	}
	
	private void initBlackList() {
		// Reads a file containing the methods that we don't want to be considered ad-hoc
		// The file should contain one method signature per line.
		try {
			this.blackListAdhoc = new HashSet<String>(Files.readAllLines(Paths.get("methods.txt")));
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
