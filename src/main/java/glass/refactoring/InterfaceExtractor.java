package glass.refactoring;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.model.Model;

import glass.ast.IType;
import glass.lattice.model.ILatticeNode;
import spoon.Launcher;
import spoon.MavenLauncher;
import spoon.refactoring.Refactoring;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtModifiable;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.Filter;
import spoon.reflect.visitor.filter.NamedElementFilter;
import spoon.reflect.visitor.filter.TypeFilter;

/**
 * Class responsible for extracting new interface from a given feature/class
 * 
 * @author Luca Scistri
 */
public class InterfaceExtractor implements IInterfaceExtractor{
	
	private Launcher launcher;
	
	public InterfaceExtractor(String projectDirectory, boolean isMavenProject) {
		if (isMavenProject) {
			launcher = new MavenLauncher(projectDirectory,
					MavenLauncher.SOURCE_TYPE.APP_SOURCE);
		} else {
			launcher = new Launcher();
			launcher.getEnvironment().setNoClasspath(true);
			launcher.addInputResource(projectDirectory);
		}
		launcher.getEnvironment().setNoClasspath(false);
		launcher.getEnvironment().setAutoImports(true);
		launcher.buildModel();
		launcher.setSourceOutputDirectory(new File(projectDirectory));
	}
	
	/**
	 * Gets the classes of the upper hierarchy from the extent of a feature
	 * and returns the interfaces that 'represents' those classes,
	 * i.e., the interfaces that have the same public methods.
	 * This method will create new interfaces if there are none
	 * that corresponds to a specific class
	 * @param feature
	 * @return Corresponding interfaces from the upper hierarchy
	 */
	public Set<IType> extractInterfaceFromFeature(ILatticeNode feature) {
		Set<Object> intent = feature.getIntent();
		
		Set<IType> classesInIntent = new HashSet<IType>();
		for (Object obj: intent) {
			if (obj instanceof IType) {
				IType type = (IType) obj;
				if (!type.isInterface()) {
					classesInIntent.add(type);
				}
			}
		}
		
		// Collect the classes from the feature, alongside every super classes to get the complete upper hierarchy
		Set<IType> superClassHierarchyFromFeature = new HashSet<IType>();
		for (IType baseclass: classesInIntent) {
			superClassHierarchyFromFeature.add(baseclass);
			superClassHierarchyFromFeature.addAll(this.getAllSuperclasses(baseclass));
		}
		
		// Get or create the interfaces that are 'reflection' of the classes in the feature
		// i.e., they have the same public methods
		Set<IType> interfacesFromFeature = new HashSet<IType>();
		for (IType extractedClass: superClassHierarchyFromFeature) {
			boolean interfaceFound = false;
			IType[] superTypes = extractedClass.getAllSupertypes();
			int i = 0;
			while(i<superTypes.length && !interfaceFound) {
				IType superType = superTypes[i];
				if (superType.isInterface() && superType.hasSamePublicInterface(extractedClass)) {
					interfacesFromFeature.add(superType);
					interfaceFound = true;
				}
				i++;
			}
			if (!interfaceFound) {
				interfacesFromFeature.add(this.extractInterfaceFromClass(extractedClass));
			}
		}
		return interfacesFromFeature;
	}
	
	public Set<IType> getAllSuperclasses(IType baseclass) {
		Set<IType> allSuperclasses = new HashSet<IType>();
		Set<IType> nextSuperclasses = new HashSet<IType>();
		for (IType superType: baseclass.getAllSupertypes()) {
			if (!superType.isInterface()) {
				nextSuperclasses.add(superType);
			}
		}
		while (!nextSuperclasses.isEmpty()) {
			allSuperclasses.addAll(nextSuperclasses);
			Set<IType> tempSuperclasses = new HashSet<IType>();
			for (IType type: nextSuperclasses) {
				for (IType superType: type.getAllSupertypes()) {
					if(!superType.isInterface()) {
						tempSuperclasses.add(superType);
					}
				}
			}
			nextSuperclasses = tempSuperclasses;
		}
		return allSuperclasses;
	}
	
	/**
	 * Creates a new interface in the same package as the base class.
	 * The new interface contains all the public methods of the base class
	 * and is now implemented by the base class.
	 * @param baseclass
	 * @return the new interface extracted from the base class
	 */
	public IType extractInterfaceFromClass(IType baseclass) {
		CtModel model = launcher.getModel();
		
		CtClass spoonBaseClass = model.
				filterChildren(new NamedElementFilter<CtPackage>(CtPackage.class, baseclass.getPackage())).
				filterChildren(new NamedElementFilter<CtClass>(CtClass.class, baseclass.getElementName())).first();

		Set<CtMethod<?>> allMethods = spoonBaseClass.getAllMethods();
		
		Set<CtMethod<?>> allPublicMethods = new HashSet<CtMethod<?>>();
		for (CtMethod method : allMethods) {
			if (method.isPublic()) {
				allPublicMethods.add(method);
			}
		}
		
		Factory factory = launcher.getFactory();
		
		CtPackage outputPackage = factory.Package().getOrCreate(baseclass.getPackage());
		CtInterface newInterface = factory.createInterface(outputPackage, "I"+baseclass.getElementName());
		newInterface.addModifier(ModifierKind.PUBLIC);
		
		List<CtMethod> newInterfaceMethods = new ArrayList<CtMethod>();
		
		for (CtMethod method: allPublicMethods) {
			CtMethod<?> clonedMethod = Refactoring.copyMethod(method);
			clonedMethod.setBody(null);
			clonedMethod.removeModifier(ModifierKind.FINAL);
			newInterface.addMethod(clonedMethod);
			newInterfaceMethods.add(clonedMethod);
		}
		
		CtTypeReference<?> interfaceRef = factory.Type().createReference(newInterface.getQualifiedName());
		spoonBaseClass.addSuperInterface(interfaceRef);
		
		return new SpoonInterface(newInterface, newInterfaceMethods, baseclass);
	}

}
