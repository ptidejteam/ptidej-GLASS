package glass.lattice.metrics.impl;

import glass.lattice.metrics.IMetricCalculator;
import glass.lattice.model.ILatticeNode;
import glass.lattice.visitor.AbstractVisitor;

public class ProximityMetric extends AbstractVisitor implements IMetricCalculator {

	@Override
	public double calculateMetric(ILatticeNode node) {
		return node.getChildren().size() + node.getParents().size();
	}

	@Override
	public void processNode(ILatticeNode node) {
		node.setMetric(this.calculateMetric(node));
	}

}
