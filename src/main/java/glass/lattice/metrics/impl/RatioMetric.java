package glass.lattice.metrics.impl;

import glass.lattice.metrics.IMetricCalculator;
import glass.lattice.model.ILatticeNode;
import glass.lattice.visitor.AbstractVisitor;

public class RatioMetric extends AbstractVisitor implements IMetricCalculator {

	@Override
	public double calculateMetric(ILatticeNode node) {
		return ((double) node.getExtent().size())/((double) node.getIntent().size());
	}

	@Override
	public void processNode(ILatticeNode node) {
		node.setMetric(this.calculateMetric(node));
	}

}
