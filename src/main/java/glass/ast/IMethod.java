package glass.ast;

/**
 * Interface grouping all the methods that are required by GLASS to be implemented
 * in order to model a method
 * 
 * @author Luca Scistri
 */
public interface IMethod {
	
	public String getSignature();
	
	/**
	 * Returns the full signature of the method, i.e., the signature with the package
	 * and the class that implements the method.
	 * @return The full signature of the method
	 */
	public String getFullSignature();
	public boolean isSimilar(IMethod comparedMethod);
	public String getElementName();
	public String[] getParameterNames();
	public String getReturnType();
	public boolean isConstructor();
	public boolean isPublic();
	public boolean isProtected();
}
