package glass.refactoring.impl;

import java.util.Set;

import glass.ast.IMethod;
import glass.ast.IType;
import glass.refactoring.IClassCreator;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.factory.Factory;

public class ClassCreator implements IClassCreator{
	
	private static int newClassCounter = 1; // to help with naming new classes
	private Factory factory;
	
	public ClassCreator(Factory factory) {
		this.factory = factory;
	}
	
	
	@Override
	public IType createClassFromMethods(Set<CtMethod<?>> methods, String pckge) {
		
		CtPackage outputPackage = factory.Package().getOrCreate(pckge);
		return null;
	}

}
