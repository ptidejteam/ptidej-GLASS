package glass.lattice.visitor.impl;

import glass.ast.IMethod;
import glass.ast.IType;
import glass.lattice.model.ILatticeNode;
import glass.lattice.model.impl.Attribute;
import glass.lattice.visitor.AbstractVisitor;
import glass.lattice.visitor.IVisitor;
import guru.nidi.graphviz.attribute.Color;
import guru.nidi.graphviz.attribute.Font;
import guru.nidi.graphviz.attribute.Rank;
import guru.nidi.graphviz.attribute.Rank.RankDir;
import guru.nidi.graphviz.attribute.Records;
import guru.nidi.graphviz.attribute.Shape;
import guru.nidi.graphviz.attribute.Style;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import static guru.nidi.graphviz.attribute.Records.*;

import static guru.nidi.graphviz.attribute.Attributes.attr;
import static guru.nidi.graphviz.model.Factory.*;

import guru.nidi.graphviz.attribute.Attributes;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.MutableNode;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Visitor meant to produce an image from a given lattice.
 * This class uses the graphviz-java library, and traverses the lattice
 * FROM TOP TO BOTTOM (the other way doesn't work yet) to produce an svg image
 */
public class LatticePrinterGraphviz extends AbstractVisitor
		implements IVisitor {

	private MutableGraph latticeGraph;
	private String graphName;
	private Map<ILatticeNode, MutableNode> graphvizNodes;
	private int conceptCounter;
	private boolean excludeTop;
	private Map<ILatticeNode, Integer> layerMapping;
	private Queue<ILatticeNode> queueBFS = new ArrayDeque<ILatticeNode>();
	private Queue<ILatticeNode> bufferQueue = new ArrayDeque<ILatticeNode>();
	private Map<ILatticeNode, MutableNode> graphvizNodesNoLink;
	private ILatticeNode top;
	private Map<Integer, MutableGraph> subGraphMapping;
	private List<List<ILatticeNode>> layers;
	//private Map<ILatticeNode, ILatticeNode> simplifiedConceptMapping;

	public LatticePrinterGraphviz(String graphName, boolean excludeTop) {
		this.graphvizNodes = new HashMap<ILatticeNode, MutableNode>();
		this.graphvizNodesNoLink = new HashMap<ILatticeNode, MutableNode>();
		this.layerMapping = new HashMap<ILatticeNode, Integer>();
		this.subGraphMapping = new HashMap<Integer, MutableGraph>();
		this.conceptCounter = 0;
		this.graphName = graphName;
		//this.simplifiedConceptMapping = simplifiedConceptMapping;
		this.excludeTop = excludeTop;
		this.latticeGraph = mutGraph(graphName).setDirected(true)
				.graphAttrs().add(Rank.dir(RankDir.BOTTOM_TO_TOP))
				.nodeAttrs().add(Font.name("arial"));
		this.latticeGraph.graphAttrs().add(attr("newrank", "true"));
		this.latticeGraph.graphAttrs().add(attr("ranksep", "10"));
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
	/*
	private Set<Object> getIntroducedAttributes(ILatticeNode node) {
		final Set<Object> introducedAttr = new HashSet<Object>();
		for (Object obj : node.getIntent()) {
			
		}
	}
	*/
	
	private String getStringIntent(ILatticeNode latticeNode) {
		final StringBuilder builder = new StringBuilder();
		final Set<Object> nodeIntent = latticeNode.getIntent();
		// The following can probably be optimized but I'm lazy
		final List<Object> listAdhoc = nodeIntent.stream()
				.filter(attr -> ((Attribute) attr).isAdhoc())
				.collect(Collectors.toCollection(ArrayList::new));
		final List<Object> listNonAdhoc = nodeIntent.stream()
				.filter(attr -> (!((Attribute) attr).isAdhoc() && (!((Attribute) attr).isExtendedAttribute())))
				.collect(Collectors.toCollection(ArrayList::new));
		listAdhoc.sort(Comparator.comparing(o -> o.toString()));
		listNonAdhoc.sort(Comparator.comparing(o -> o.toString()));
		for (Object attr : listAdhoc) {
			builder.append(attr + "\n");
		}
		if (listNonAdhoc.size() > 0) {
			builder.append("----------\n");
		}
		for (Object attr : listNonAdhoc) {
			builder.append(attr + "\n");
		}
		return builder.toString();
	}
	
	private MutableNode createNode(ILatticeNode latticeNode) {
		MutableNode currentNode = mutNode("Node_"+this.conceptCounter).add(
				Records.of(turn(
						rec("conceptName", "Concept_"+this.conceptCounter),
						rec("extent", getStringExtent(latticeNode)),
						rec("intent", getStringIntent(latticeNode))))).add(Shape.M_RECORD).add(Style.FILLED);
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
		MutableNode startingNode = null;
		this.top = latticeNode;
		this.queueBFS.add(this.top); // preparing the layeredSearch
		if (!this.excludeTop) {
			startingNode = this.createNode(latticeNode);
		}
		
		for (ILatticeNode child : latticeNode.getChildren()) {
			this.visitLatticeNode(child, direction, startingNode);
		}
		
	}
	
	private void visitLatticeNode(ILatticeNode latticeNode, Direction direction, MutableNode parent) {
		if (this.graphvizNodes.containsKey(latticeNode)) {
			MutableNode currentNode = this.graphvizNodes.get(latticeNode);
			if (parent != null) {
				this.linkNodes(currentNode, parent);
			}
			return;
		}
		
		MutableNode currentNode = this.createNode(latticeNode);
		this.graphvizNodesNoLink.put(latticeNode, currentNode.copy());
		if (parent != null) {
			this.linkNodes(currentNode, parent);
		}
		
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
	
	public void processResultsFeature() {
		this.layeredSearchFromTop();
		this.setColorsPerLayers();
		for (ILatticeNode currentNode : this.graphvizNodes.keySet()) {
			MutableNode currentGraphicNode = this.graphvizNodes.get(currentNode);
			//MutableNode currentGraphicNodeNoLink = this.graphvizNodesNoLink.get(currentNode);
			int depth = this.layerMapping.get(currentNode);
			if (depth == 0) {
				continue; // The top node is not a feature
			}
			MutableGraph subGraph = this.subGraphMapping.get(depth);
			//currentGraphicNode.addTo(this.latticeGraph);
			//currentGraphicNodeNoLink.addTo(subGraph);
			currentGraphicNode.addTo(subGraph);
		}
		try {
			Graphviz.fromGraph(latticeGraph).render(Format.SVG).toFile(new File(this.graphName + ".svg"));
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void layeredSearchFromTop() { // this is a disaster
		int depth = 0;
		this.layers = new ArrayList<List<ILatticeNode>>();
		while (!(this.queueBFS.isEmpty())) {
			ILatticeNode currentNode = this.queueBFS.poll();
			this.layerMapping.put(currentNode, depth); // A node could be at different depths, we just want the max depth
			for (ILatticeNode child : currentNode.getChildren()) {
				this.bufferQueue.add(child);
			}
			if (this.queueBFS.isEmpty()) {
				this.queueBFS.addAll(bufferQueue);
				this.bufferQueue = new ArrayDeque<ILatticeNode>();
				depth++;
				MutableGraph subGraph = mutGraph(graphName + depth).setCluster(true)
						.graphAttrs().add(Style.FILLED, Color.BLANCHEDALMOND)
						.nodeAttrs().add(Font.name("arial"));
				subGraph.graphAttrs().add(attr("rank", "same"));
				subGraph.addTo(this.latticeGraph);
				this.subGraphMapping.put(depth, subGraph);
				this.layers.add(new ArrayList<ILatticeNode>());
			}
		}
		for (Entry<ILatticeNode, Integer> e : this.layerMapping.entrySet()) {
			this.layers.get(e.getValue()).add(e.getKey());
		}
	}
	
	private void setColorsPerLayers() {
		for (List<ILatticeNode> layer : this.layers) {
			if (layer.contains(this.top)) {
				continue;
			}
			double minMetric = layer.get(0).getMetric();
			double maxMetric = minMetric;
			for (ILatticeNode node : layer) {
				double nodeMetric = node.getMetric();
				if (nodeMetric < minMetric) {
					minMetric = nodeMetric;
				}
				if (nodeMetric > maxMetric) {
					maxMetric = nodeMetric;
				}
			}
			for (ILatticeNode node : layer) {
				MutableNode graphicNode = this.graphvizNodes.get(node);
				if (minMetric == maxMetric) {
					graphicNode.add(Color.rgb(0, 0, 255));
				} else {
					int nonBlueColor = (int) (255 * (1 - ((node.getMetric() - minMetric) / (maxMetric - minMetric))));
					graphicNode.add(Color.rgb(nonBlueColor, nonBlueColor, 255));
				}
			}
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
	}

}
