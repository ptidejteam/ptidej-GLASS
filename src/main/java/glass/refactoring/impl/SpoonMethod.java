package glass.refactoring.impl;

import java.util.List;

import glass.ast.IMethod;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtMethod;

public class SpoonMethod implements IMethod{

	private CtMethod method;
	
	public SpoonMethod(CtMethod method) {
		this.method = method;
	}
	
	@Override
	public String getSignature() {
		StringBuilder builder = new StringBuilder();
		// builder.append(this.method.getVisibility() + " "); not needed for now
		builder.append(this.method.getType() + " ");
		builder.append(this.method.getSignature());
		return builder.toString();
	}

	@Override
	public boolean isSimilar(IMethod comparedMethod) {
		return this.getSignature().equals(comparedMethod.getSignature());
	}

	@Override
	public String getElementName() {
		return this.method.getSimpleName();
	}

	@Override
	public String[] getParameterNames() {
		List<CtExecutable> parameters = this.method.getParameters();
		String[] res = new String[parameters.size()];
		for (int i = 0; i<parameters.size(); i++) {
			res[i] = parameters.get(i).getSimpleName();
		}
		return res;
	}

	@Override
	public String getReturnType() {
		return this.method.getType().toString();
	}

	@Override
	public boolean isConstructor() {
		return false; // a CtMethod cannot be a constructor
	}

	@Override
	public boolean isPublic() {
		return this.method.isPublic();
	}

	@Override
	public boolean isProtected() {
		return this.method.isProtected();
	}

}
