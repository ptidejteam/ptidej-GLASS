package glass.refactoring.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import glass.ast.IField;
import glass.ast.IMethod;
import glass.ast.IType;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtMethod;


/**
 * Class meant to be used when creating a new interface using Spoon.
 * The created interface is meant to be a 'clone' of a concrete class, meaning that
 * they share the same public methods and the new interface is implemented by the latter.
 * 
 * @author Luca Scistri
 */
public class SpoonInterface implements IType{
	
	private CtInterface newInterface;
	private IMethod[] methods; // contains only local methods
	private IType baseclass;
	private IType[] superTypes; // non recursive
	private IType[] subTypes; // non recursive
	
	public SpoonInterface(CtInterface newInterface, List<CtMethod> methods, IType baseclass) {
		this.newInterface = newInterface;
		this.methods = new IMethod[methods.size()];
		for (int i = 0; i<methods.size(); i++) {
			this.methods[i] = new SpoonMethod(methods.get(i));
		}
		this.baseclass = baseclass;
		this.subTypes = new IType[1];
		this.subTypes[0] = baseclass;
	}

	@Override
	public boolean isAnonymous() {
		return false;
	}

	@Override
	public boolean isInterface() {
		return true;
	}

	@Override
	public IMethod[] getMethods() {
		Set<IMethod> allMethods = new HashSet<IMethod>();
		allMethods.addAll(Arrays.asList(this.getLocalMethods()));
		// Since we have an interface, its supertypes are also interfaces
		// thus we don't need to check the visibility of the methods
		for (IType superType : this.getAllSupertypes()) {
			allMethods.addAll(Arrays.asList(superType.getMethods()));
		}
		return (IMethod[]) allMethods.toArray();
	}

	@Override
	public IField[] getFields() {
		return null; // The created interface shouldn't have any fields
	}

	@Override
	public String getElementName() {
		return this.newInterface.getSimpleName();
	}

	@Override
	public String getFullyQualifiedName() {
		return this.newInterface.getQualifiedName();
	}

	@Override
	public String getFullyQualifiedParameterizedName() {
		return this.newInterface.getQualifiedName();
	}

	@Override
	public String[][] resolveType(String typeName) {
		return null; // not needed in this case
	}

	@Override
	public IType[] getAllSubtypes() {
		IType[] baseclassSubtypes = this.baseclass.getAllSubtypes();
		IType[] res = new IType[1 + baseclassSubtypes.length];
		res[0] = this.baseclass;
		System.arraycopy(baseclassSubtypes, 0, res, 1, res.length);
		return res;
	}

	@Override
	public IType[] getAllSupertypes() {
		return null; // the new interface does not implement anything
	}

	@Override
	public IType[] getImplementingClasses() {
		IType[] res = {this.baseclass};
		return res;
	}

	@Override
	public String getPackage() {
		return this.baseclass.getPackage();
	}

	@Override
	public void addSuperInterface(IType superInterface) {
		this.superTypes = this.addToArray(superTypes, superInterface);
	}

	@Override
	public void changeSuperclass(IType newSuperclass) {
		// do nothing, should throw exception in the future?
	}

	@Override
	public void addSubType(IType subType) {
		this.subTypes = this.addToArray(subTypes, subType);
	}
	
	private IType[] addToArray(IType[] typeArray, IType newType) {
		IType[] newArray = null;
		if (typeArray == null) {
			newArray = new IType[1];
			newArray[0] = newType;
		} else {
			newArray = new IType[typeArray.length + 1];
			newArray[0] = newType;
			System.arraycopy(typeArray, 0, newArray, 1, newArray.length);
		}
		return newArray;
	}

	@Override
	public boolean hasSamePublicInterface(IType comparedType) {
		Set<String> publicComparedMethods = Stream.of(comparedType.getMethods()).
				filter(m -> m.isPublic()).
				map(m -> m.getSignature()).
				collect(Collectors.toSet());
		
		String[] publicLocalMethods = Stream.of(this.getMethods()).
				filter(m -> m.isPublic()).
				map(m -> m.getSignature()).
				toArray(String[]::new);
		
		int nbPublicMethods = publicLocalMethods.length;
		
		if (nbPublicMethods != publicComparedMethods.size()) {
			return false;
		}
		
		int i = 0;
		while (i<nbPublicMethods && publicComparedMethods.contains(publicLocalMethods[i])) {
			i++;
		}
		
		return i == nbPublicMethods;
		
	}

	@Override
	public IMethod[] getLocalMethods() {
		return this.methods;
	}

}
