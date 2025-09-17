package glass.refactoring;

import java.util.Set;

import glass.ast.IType;
import glass.lattice.model.ILatticeNode;

public interface IInterfaceExtractor {

	public IType extractInterfaceFromClass(IType baseclass);
	public Set<IType> extractInterfacesFromFeature(ILatticeNode feature);
}
