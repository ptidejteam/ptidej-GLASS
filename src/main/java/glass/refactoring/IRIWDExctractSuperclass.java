package glass.refactoring;

import glass.ast.IMethod;
import glass.ast.IType;

public interface IRIWDExctractSuperclass {

	public IType ExtractSuperclass(IMethod[] methodsFromIntent);
}
