package glass.lattice.visitor.impl;

import glass.ast.IMethod;
import glass.ast.IType;
import glass.lattice.model.ILatticeNode;
import glass.lattice.visitor.AbstractVisitor;
import glass.lattice.visitor.IVisitor;
import guru.nidi.graphviz.attribute.Font;
import guru.nidi.graphviz.attribute.Rank;
import guru.nidi.graphviz.attribute.Rank.RankDir;
import guru.nidi.graphviz.attribute.Records;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.MutableNode;
import static guru.nidi.graphviz.attribute.Records.*;

import static guru.nidi.graphviz.model.Factory.*;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/**
 * Visitor meant to produce an image from a given lattice.
 * This class uses the graphviz-java library, and traverses the lattice
 * FROM TOP TO BOTTOM (the other way doesn't work yet) to produce an svg image
 */
public class LatticePrinterGraphviz extends AbstractVisitor implements IVisitor{

	private MutableGraph latticeGraph;
	private String graphName;
	private Map<ILatticeNode, MutableNode> graphvizNodes;
	private int conceptCounter;
	
	public LatticePrinterGraphviz(String graphName) {
		this.graphvizNodes = new HashMap<ILatticeNode, MutableNode>();
		this.conceptCounter = 0;
		this.graphName = graphName;
		this.latticeGraph = mutGraph(graphName).setDirected(true)
				.graphAttrs().add(Rank.dir(RankDir.BOTTOM_TO_TOP))
				.nodeAttrs().add(Font.name("arial"));
	}
	
	private String getStringExtent(ILatticeNode latticeNode) {
		final StringBuilder builder = new StringBuilder();
		final Set<Object> nodeExtent = latticeNode.getExtent();
		final Iterator<Object> itExtent = nodeExtent.iterator();
		while (itExtent.hasNext()) {
			IType currentType = (IType) itExtent.next();
			builder.append(currentType.getFullyQualifiedName() + "\n");
		}
		return builder.toString();
	}
	
	private String getStringIntent(ILatticeNode latticeNode) {
		final StringBuilder builder = new StringBuilder();
		final Set<Object> nodeIntent = latticeNode.getIntent();
		final Iterator<Object> itIntent = nodeIntent.iterator();
		while (itIntent.hasNext()) {
			builder.append(itIntent.next() + "\n");
		}
		return builder.toString();
	}
	
	private MutableNode createNode(ILatticeNode latticeNode) {
		MutableNode currentNode = mutNode("Node_"+this.conceptCounter).add(
				Records.of(turn(
						rec("conceptName", "Concept_"+this.conceptCounter),
						rec("extent", getStringExtent(latticeNode)),
						rec("intent", getStringIntent(latticeNode)))));
		this.graphvizNodes.put(latticeNode, currentNode);
		this.conceptCounter++;
		return currentNode;
	}
	
	private void linkNodes(MutableNode children, MutableNode parent) {
		children.addLink(parent);
	}

	/**
	 * Overridden visit method, because we need to keep in mind the parent node in such
	 * a way that we can make a connection to the child node when building the graph.
	 * The method assumes that the lattice is visited from top to bottom
	 * 
	 * The method has to be overridden because it is called by the lattice nodes.
	 * Another visit method will be called for the children.
	 */
	@Override
	public void visitLatticeNode(ILatticeNode latticeNode, Direction direction) {
		if (this.graphvizNodes.containsKey(latticeNode)) { // probably useless, but we never know
			return;
		}
		
		MutableNode startingNode = this.createNode(latticeNode);
		
		for (ILatticeNode child : latticeNode.getChildren()) {
			this.visitLatticeNode(child, direction, startingNode);
		}
		
	}
	
	private void visitLatticeNode(ILatticeNode latticeNode, Direction direction, MutableNode parent) {
		if (this.graphvizNodes.containsKey(latticeNode)) {
			MutableNode currentNode = this.graphvizNodes.get(latticeNode);
			this.linkNodes(currentNode, parent);
			return;
		}
		
		MutableNode currentNode = this.createNode(latticeNode);
		this.linkNodes(currentNode, parent);
		
		for (ILatticeNode child : latticeNode.getChildren()) {
			this.visitLatticeNode(child, direction, currentNode);
		}
	}
	
	public void processResults() {
		for (MutableNode currentNode : this.graphvizNodes.values()) {
			currentNode.addTo(latticeGraph);
		}
		try {
			Graphviz.fromGraph(latticeGraph).render(Format.SVG).toFile(new File(this.graphName + ".svg"));
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	@Override
	public void reset() {
		this.latticeGraph = mutGraph(graphName).setDirected(true)
				.graphAttrs().add(Rank.dir(RankDir.BOTTOM_TO_TOP))
				.nodeAttrs().add(Font.name("arial"));
		this.graphvizNodes = new HashMap<ILatticeNode, MutableNode>();
		this.conceptCounter = 0;
	}

	/**
	 * I don't know what to do with this xd
	 */
	@Override
	public void processNode(ILatticeNode node) {
		// TODO Auto-generated method stub
		
	}

}
