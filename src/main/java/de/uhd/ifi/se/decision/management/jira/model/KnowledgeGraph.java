package de.uhd.ifi.se.decision.management.jira.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.AsUndirectedGraph;
import org.jgrapht.graph.DirectedWeightedMultigraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uhd.ifi.se.decision.management.jira.git.GitClient;
import de.uhd.ifi.se.decision.management.jira.persistence.AutomaticLinkCreator;
import de.uhd.ifi.se.decision.management.jira.persistence.KnowledgePersistenceManager;

/**
 * Creates the knowledge graph for the entire project. Nodes of the knowledge
 * graph are decision knowledge elements, Jira issues such as requirements and
 * work items, and code files. Extends the JGraphT graph interface. The
 * knowledge graph can be disconnected.
 * 
 * @see GitClient
 * @see Graph
 */
public class KnowledgeGraph extends DirectedWeightedMultigraph<KnowledgeElement, Link> {

	private static final Logger LOGGER = LoggerFactory.getLogger(KnowledgeGraph.class);

	private static final long serialVersionUID = 1L;

	private Set<Long> linkIds;

	// for elements that do not exist in database
	private long nextElementId = -1;
	private long nextLinkId = -1;

	/**
	 * Instances of knowledge graphs that are identified by the project key.
	 */
	public static Map<String, KnowledgeGraph> instances = new HashMap<String, KnowledgeGraph>();

	/**
	 * Retrieves an existing {@link KnowledgeGraph} instance or creates a new
	 * instance if there is no instance for the given project key. Uses the multiton
	 * design pattern.
	 * 
	 * @param projectKey
	 *            of the Jira project.
	 * @return either a new or already existing {@link KnowledgeGraph} instance.
	 */
	public static KnowledgeGraph getInstance(String projectKey) {
		if (projectKey == null || projectKey.isBlank()) {
			return null;
		}
		if (instances.containsKey(projectKey)) {
			return instances.get(projectKey);
		}
		KnowledgeGraph knowledgeGraph = new KnowledgeGraph(projectKey);
		instances.put(projectKey, knowledgeGraph);
		return knowledgeGraph;
	}

	public static KnowledgeGraph getInstance(DecisionKnowledgeProject project) {
		if (project == null) {
			return null;
		}
		return getInstance(project.getProjectKey());
	}

	public KnowledgeGraph() {
		super(Link.class);
		linkIds = new HashSet<>();
	}

	public KnowledgeGraph(String projectKey) {
		this();
		KnowledgePersistenceManager persistenceManager = KnowledgePersistenceManager.getOrCreate(projectKey);
		persistenceManager.getKnowledgeElements().parallelStream().forEach(element -> {
			addVertex(element);
			persistenceManager.getLinks(element).parallelStream().forEach(link -> {
				if (!linkIds.contains(link.getId())) {
					addEdge(link);
					linkIds.add(link.getId());
				}
			});
		});
	}

	/**
	 * Adds the specified edge ({@link Link} object) to this graph, going from the
	 * source vertex to the target vertex. More formally, adds the specified edge,
	 * <code>e</code>, to this graph if this graph contains no edge <code>e2</code>
	 * such that <code>e2.equals(e)</code>. If this graph already contains such an
	 * edge, the call leaves this graph unchanged and returns <tt>false</tt>. If the
	 * edge was added to the graph, returns <code>true</code>.
	 *
	 * @param link
	 *            edge to be added to this graph as a {@link Link} object.
	 *
	 * @return <tt>true</tt> if this graph did not already contain the specified
	 *         edge.
	 *
	 * @see #addEdge(Object, Object)
	 * @see #getEdgeSupplier()
	 */
	public boolean addEdge(Link link) {
		boolean isEdgeCreated = false;
		KnowledgeElement source = link.getSource();
		addVertex(source);

		KnowledgeElement destination = link.getTarget();
		addVertex(destination);

		try {
			isEdgeCreated = addEdge(source, destination, link);
		} catch (IllegalArgumentException e) {
			LOGGER.error("Error adding link to the graph: " + e.getMessage());
		}
		return isEdgeCreated;
	}

	/**
	 * @param element
	 *            {@link KnowledgeElement} that is not stored in the database via
	 *            the {@link KnowledgePersistenceManager} but only exists in RAM.
	 * @return element with a negative id to indicate that it is not stored in
	 *         database.
	 */
	public KnowledgeElement addVertexNotBeingInDatabase(KnowledgeElement element) {
		KnowledgeElement existingElement = getElementsNotInDatabaseBySummary(element.getSummary());
		if (existingElement != null) {
			return existingElement;
		}
		--nextElementId;
		element.setId(nextElementId);
		element.setKey(element.getProject().getProjectKey() + ":graph:" + nextElementId);
		addVertex(element);
		return element;
	}

	/**
	 * @param link
	 *            {@link Link} that is not stored in the database via the
	 *            {@link KnowledgePersistenceManager} but only exists in RAM.
	 * @return link with a negative id to indicate that it is not stored in
	 *         database.
	 */
	public Link addEdgeNotBeingInDatabase(Link link) {
		if (link.containsUnknownDocumentationLocation()) {
			return null;
		}
		link.setId(--nextLinkId);
		addEdge(link);
		return link;
	}

	/**
	 * Updates a {@link KnowledgeElement} (i.e. a graph node/vertex). If the element
	 * is not in the graph, it will be added.
	 * 
	 * @param node
	 *            {@link KnowledgeElement} to be updated (or added).
	 */
	public boolean updateElement(KnowledgeElement node) {
		KnowledgeElement oldElement = null;
		for (KnowledgeElement currentElement : vertexSet()) {
			if (node.equals(currentElement)) {
				oldElement = currentElement;
			}
		}
		if (oldElement == null) {
			return false;
		}
		return replaceVertex(oldElement, node);
	}

	private boolean replaceVertex(KnowledgeElement vertex, KnowledgeElement replace) {
		Set<Link> newLinks = new HashSet<Link>();
		for (Link edge : outgoingEdgesOf(vertex)) {
			newLinks.add(new Link(replace, edge.getTarget(), edge.getType()));
		}
		for (Link edge : incomingEdgesOf(vertex)) {
			newLinks.add(new Link(edge.getSource(), replace, edge.getType()));
		}
		removeVertex(vertex);
		for (Link link : newLinks) {
			addEdge(link);
		}
		return true;
	}

	/**
	 * Returns <tt>true</tt> if this graph contains the specified edge. More
	 * formally, returns <tt>true</tt> if and only if this graph contains an edge
	 * <code>e2</code> such that <code>e.equals(e2)</code>. If the specified edge is
	 * <code>null</code> returns <code>false</code>.
	 *
	 * @param link
	 *            edge whose presence in this graph is to be tested.
	 *
	 * @return <tt>true</tt> if this graph contains the specified edge.
	 */
	@Override
	public boolean containsEdge(Link link) {
		if (link == null) {
			return false;
		}
		return super.containsEdge(link.getSource(), link.getTarget());
	}

	/**
	 * @param link
	 *            edge whose presence in this graph is to be tested.
	 * @return <tt>true</tt> if the graph contains this edge if it was undirected
	 *         (source and target can be flipped).
	 */
	public boolean containsUndirectedEdge(Link link) {
		return containsEdge(link) || containsEdge(link.flip());
	}

	/**
	 * Removes the specified edge from the graph if it is present. Returns
	 * <tt>true</tt> if the graph contained the specified edge. (The graph will not
	 * contain the specified edge once the call returns).
	 *
	 * @param link
	 *            edge to be removed from this graph, if present.
	 *
	 * @return <code>true</code> if and only if the graph contained the specified
	 *         edge.
	 */
	@Override
	public boolean removeEdge(Link link) {
		Link removedLink = super.removeEdge(link.getSource(), link.getTarget());
		if (removedLink == null) {
			removedLink = super.removeEdge(link.getTarget(), link.getSource());
		}
		return removedLink != null;
	}

	@Override
	public Set<Link> edgesOf(KnowledgeElement element) {
		Set<Link> edges = new HashSet<Link>();
		try {
			edges = super.edgesOf(element);
		} catch (IllegalArgumentException | NullPointerException e) {
			LOGGER.error("Edges for node could not be returned. " + e);
		}
		return edges;
	}

	/**
	 * Returns all unlinked elements of the knowledge element for a project. Sorts
	 * the elements according to their similarity and their likelihood that they
	 * should be linked.
	 * 
	 * TODO Sorting according to the likelihood that they should be linked.
	 * 
	 * @issue How can the sorting be implemented?
	 *
	 * @param element
	 *            {@link KnowledgeElement} with id in database. The id is different
	 *            to the key.
	 * @return set of unlinked elements, sorted by the likelihood that they should
	 *         be linked.
	 */
	public List<KnowledgeElement> getUnlinkedElements(KnowledgeElement element) {
		return vertexSet().stream()
				.filter(vertex -> !vertex.equals(element) && !Graphs.neighborListOf(this, element).contains(vertex))
				.collect(Collectors.toList());
	}

	public List<KnowledgeElement> getUnlinkedElementsAndNotInSameJiraIssue(KnowledgeElement element) {
		List<KnowledgeElement> unlinkedElements = getUnlinkedElements(element);
		unlinkedElements
				.removeAll(unlinkedElements.stream()
						.filter(unlinkedElement -> unlinkedElement.getJiraIssue() != null
								&& unlinkedElement.getJiraIssue().equals(element.getJiraIssue()))
						.collect(Collectors.toList()));
		return unlinkedElements;
	}

	/**
	 * @return list of all {@link KnowledgeElement}s for a project with a certain
	 *         {@link KnowledgeType}.
	 */
	public List<KnowledgeElement> getElements(KnowledgeType type) {
		KnowledgeType simpleType = type.replaceProAndConWithArgument();
		return vertexSet().stream().filter(element -> element.getType().replaceProAndConWithArgument() == simpleType)
				.collect(Collectors.toList());
	}

	/**
	 * @return list of all {@link KnowledgeElement}s for a project with a certain
	 *         knowledgeType passed as String.
	 */
	public List<KnowledgeElement> getElements(String type) {
		return vertexSet().stream().filter(element -> element.getTypeAsString().equals(type))
				.collect(Collectors.toList());
	}

	/**
	 * @issue How can we get a subgraph of the entire knowledge graph that only
	 *        contains certain elements (for filtering)?
	 * @alternative Use the org.jgrapht.graph.AsSubgraph class to create a subgraph.
	 * @con The subgraph needs to include all the elements of the entire graph,
	 *      which can be a problem during knowledge management.
	 * @con This class is not a subclass of our KnowledgeGraph class and cannot be
	 *      easily cast. Thus, it lacks methods such as getElements(KnowledgeType).
	 * @decision Create a new KnowledgeGraph object and add the respective elements
	 *           and links to it!
	 * @pro Is mutable, i.e. new elements and links can be added.
	 * @pro Provides all methods as the entire graph.
	 * 
	 * @param collection
	 *            of {@link KnowledgeElement}s (verteces/nodes) that should be
	 *            included in the new {@link KnowledgeGraph}.
	 * @return new {@link KnowledgeGraph} containing the given
	 *         {@link KnowledgeElement}s as well as all the {@link Link}s between
	 *         these elements. New {@link Link}s and {@link KnowledgeElement}s can
	 *         be added to this graph. Thus, it is no real subgraph but mutable.
	 */
	public KnowledgeGraph getMutableSubgraphFor(Collection<KnowledgeElement> elements) {
		KnowledgeGraph mutableSubgraph = new KnowledgeGraph();
		elements.forEach(vertex -> mutableSubgraph.addVertex(vertex));
		edgeSet().stream().filter(edge -> {
			return elements.contains(edge.getSource()) && elements.contains(edge.getTarget());
		}).forEach(edge -> mutableSubgraph.addEdge(edge));
		return mutableSubgraph;
	}

	/**
	 * @param elementKey
	 *            e.g. CONDEC-42.
	 * @return {@link KnowledgeElement} (i.e. graph node/vertex) with the given key
	 *         or null if not existing.
	 */
	public KnowledgeElement getElementByKey(String elementKey) {
		Optional<KnowledgeElement> elementWithKey = vertexSet().parallelStream()
				.filter(element -> element.getKey().equals(elementKey)).findFirst();
		return elementWithKey.isPresent() ? elementWithKey.get() : null;
	}

	/**
	 * @param summary
	 *            of the {@link KnowledgeElement} (i.e. graph node/vertex).
	 * @return {@link KnowledgeElement} (i.e. graph node/vertex) with the given
	 *         summary or null if not existing.
	 */
	public KnowledgeElement getElementBySummary(String summary) {
		Optional<KnowledgeElement> elementWithSummary = vertexSet().parallelStream()
				.filter(element -> element.getSummary().equals(summary)).findFirst();
		return elementWithSummary.isPresent() ? elementWithSummary.get() : null;
	}

	/**
	 * @param summary
	 *            of the {@link KnowledgeElement} (i.e. graph node/vertex).
	 * @return {@link KnowledgeElement} (i.e. graph node/vertex) with the given
	 *         summary or null if not existing. The returned element needs to be
	 *         only stored in RAM, not in the database, and thus, has a negative id.
	 */
	public KnowledgeElement getElementsNotInDatabaseBySummary(String summary) {
		Iterator<KnowledgeElement> iterator = vertexSet().iterator();
		while (iterator.hasNext()) {
			KnowledgeElement element = iterator.next();
			if (element.getId() >= -1) {
				continue;
			}
			if (element.getSummary().equals(summary)) {
				return element;
			}
		}
		return null;
	}

	public void addElementsNotInDatabase(KnowledgeElement root, List<KnowledgeElement> otherElements) {
		for (KnowledgeElement element : otherElements) {
			addVertexNotBeingInDatabase(element);
			if (element.getId() >= 0) {
				return;
			}
			KnowledgeElement potentialParent = AutomaticLinkCreator.getPotentialParentElement(element, otherElements);
			Link link;
			if (potentialParent == null) {
				link = Link.instantiateDirectedLink(root, element);
			} else {
				addVertexNotBeingInDatabase(potentialParent);
				link = Link.instantiateDirectedLink(potentialParent, element);
			}
			addEdgeNotBeingInDatabase(link);
		}
		updateStatusOfElements(otherElements);
	}

	public void updateStatusOfElements(List<KnowledgeElement> otherElements) {
		otherElements.forEach(element -> {
			KnowledgeStatus newStatus = KnowledgeStatus.getNewStatus(element);
			if (newStatus != element.getStatus()) {
				element.setStatus(newStatus);
				updateElement(element);
			}
		});
	}

	public KnowledgeElement getElementById(long id) {
		Optional<KnowledgeElement> vertexInGraph = vertexSet().parallelStream().filter(vertex -> vertex.getId() == id)
				.findFirst();
		return vertexInGraph.isPresent() ? vertexInGraph.get() : null;
	}

	/**
	 * @param maxLinkDistance
	 * @return {@link DijkstraShortestPath} algorithm to get all paths within a link
	 *         distance. The paths do NOT depend on link direction because the
	 *         method converts the directed graph into an undirected graph.
	 */
	public ShortestPathAlgorithm<KnowledgeElement, Link> getShortestPathAlgorithm(int maxLinkDistance) {
		Graph<KnowledgeElement, Link> undirectedGraph = new AsUndirectedGraph<KnowledgeElement, Link>(this);
		return new DijkstraShortestPath<>(undirectedGraph, maxLinkDistance);
	}
}