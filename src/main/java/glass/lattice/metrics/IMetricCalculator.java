package glass.lattice.metrics;

import glass.lattice.model.ILatticeNode;
import glass.lattice.visitor.IVisitor;

public interface IMetricCalculator extends IVisitor{

	public double calculateMetric(ILatticeNode node);
	
}
