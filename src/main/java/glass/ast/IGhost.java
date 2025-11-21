package glass.ast;

/**
 * Interface modeling a ghost i.e., a type defined outside the analyzed project
 * @author Luca Scistri
 */
public interface IGhost extends IType{

	@Override
	public default boolean isGhost() {return true;}
}
