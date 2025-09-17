package glass.refactoring;

import java.util.Set;

import glass.ast.IMethod;
import glass.ast.IType;
import spoon.reflect.declaration.CtMethod;

public interface IClassCreator {

	public IType createClassFromMethods(Set<CtMethod<?>> methods, String pckge);
}
