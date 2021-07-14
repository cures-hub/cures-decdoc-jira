package de.uhd.ifi.se.decision.management.jira.filtering;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jgrapht.GraphPath;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm.SingleSourcePaths;
import org.jgrapht.alg.shortestpath.TreeSingleSourcePathsImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.user.ApplicationUser;

import de.uhd.ifi.se.decision.management.jira.git.model.ChangedFile;
import de.uhd.ifi.se.decision.management.jira.model.DecisionKnowledgeProject;
import de.uhd.ifi.se.decision.management.jira.model.DocumentationLocation;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeElement;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeGraph;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeStatus;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeType;
import de.uhd.ifi.se.decision.management.jira.model.Link;
import de.uhd.ifi.se.decision.management.jira.model.LinkType;
import de.uhd.ifi.se.decision.management.jira.quality.completeness.DefinitionOfDone;
import de.uhd.ifi.se.decision.management.jira.quality.completeness.KnowledgeElementCheck;

/**
 * Filters the {@link KnowledgeGraph}. The filter criteria are specified in the
 * {@link FilterSettings} class.
 */
public class FilteringManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(FilteringManager.class);
	private FilterSettings filterSettings;
	private KnowledgeGraph graph;

	public FilteringManager(FilterSettings filterSettings) {
		this.filterSettings = filterSettings;
		if (filterSettings != null) {
			this.graph = KnowledgeGraph.getInstance(filterSettings.getProjectKey());
		}
	}

	public FilteringManager(String projectKey, ApplicationUser user, String query) {
		this(new FilterSettings(projectKey, query, user));
	}

	/**
	 * @return all knowledge elements that match the {@link FilterSettings}.
	 */
	public Set<KnowledgeElement> getElementsMatchingFilterSettings() {
		if (filterSettings == null || graph == null) {
			LOGGER.error("FilteringManager misses important attributes.");
			return new HashSet<>();
		}
		Set<KnowledgeElement> elements = getElementsInLinkDistanceFromSelectedElementOrEntireVertexSet();
		elements = elements.stream().filter(element -> isElementMatchingFilterSettings(element))
				.collect(Collectors.toSet());
		if (filterSettings.getSelectedElement() != null) {
			elements.add(filterSettings.getSelectedElement());
		}
		return elements;
	}

	/**
	 * @return new {@link KnowledgeGraph} that matches the {@link FilterSettings}.
	 *         If no transitive links are created, a subgraph of the original graph
	 *         is returned. If transitive links are created, the returned graph
	 *         contains new {@link Link}s (and is thus no subgraph).
	 */
	public KnowledgeGraph getFilteredGraph() {
		if (filterSettings == null || graph == null) {
			LOGGER.error("FilteringManager misses important attributes.");
			return null;
		}

		Set<KnowledgeElement> elements = getElementsMatchingFilterSettings();
		KnowledgeGraph filteredGraph = graph.getMutableSubgraphFor(elements);

		if (filterSettings.getSelectedElement() != null) {
			if (filterSettings.createTransitiveLinks()) {
				addTransitiveLinksToFilteredGraph(filteredGraph);
			} else {
				filteredGraph = ensureThatFilteredGraphHasCorrectLinkDistance(filteredGraph);
			}
		}

		removeLinksWithTypesNotInFilterSettings(filteredGraph);
		return filteredGraph;
	}

	/**
	 * @param filteredGraph
	 *            subgraph of the entire {@link KnowledgeGraph} that matches the
	 *            {@link FilterSettings} but without transitive links.
	 * @return new {@link KnowledgeGraph} that matches the {@link FilterSettings}.
	 *         Filtered {@link KnowledgeElement}s (=nodes/verteces) are replaced
	 *         with transitive links.
	 */
	private KnowledgeGraph addTransitiveLinksToFilteredGraph(KnowledgeGraph filteredGraph) {
		SingleSourcePaths<KnowledgeElement, Link> paths = filterSettings.getSelectedElement()
				.getAllPaths(filterSettings.getLinkDistance());
		int id = Integer.MIN_VALUE;

		for (KnowledgeElement element : filteredGraph.vertexSet()) {
			GraphPath<KnowledgeElement, Link> path = paths.getPath(element);
			KnowledgeElement lastValidElementOnPath = filterSettings.getSelectedElement();
			for (KnowledgeElement elementOnPath : path.getVertexList()) {
				if (!filteredGraph.vertexSet().contains(elementOnPath)) {
					// the element on the former path is filtered out
					continue;
				}
				Link transitiveLink = new Link(lastValidElementOnPath, elementOnPath, LinkType.TRANSITIVE);
				if (!filteredGraph.containsUndirectedEdge(transitiveLink)) {
					transitiveLink.setId(id++);
					filteredGraph.addEdge(transitiveLink);
				}
				lastValidElementOnPath = elementOnPath;
			}
		}
		return filteredGraph;
	}

	private Set<KnowledgeElement> getElementsInLinkDistanceFromSelectedElementOrEntireVertexSet() {
		if (filterSettings.getSelectedElement() != null) {
			graph.addVertex(filterSettings.getSelectedElement());
			return getElementsInLinkDistance(filterSettings.getSelectedElement());
		}
		return graph.vertexSet();
	}

	private Set<KnowledgeElement> getElementsInLinkDistance(KnowledgeElement element) {
		int linkDistance = filterSettings.getLinkDistance();
		return element.getLinkedElements(linkDistance);
	}

	/**
	 * Removes those edges from the graph that have types that are not in the
	 * {@link FilterSettings#getLinkTypes()}.
	 * 
	 * @param subgraph
	 *            {@link KnowledgeGraph} object.
	 * @return subgraph of the {@link KnowledgeGraph} which might have fewer edges
	 *         than before.
	 */
	private KnowledgeGraph removeLinksWithTypesNotInFilterSettings(KnowledgeGraph subgraph) {
		if (filterSettings.getLinkTypes().size() < DecisionKnowledgeProject.getNamesOfLinkTypes().size()) {
			Set<Link> linksNotMatchingFilterSettings = getLinksNotMatchingFilterSettings(subgraph.edgeSet());
			subgraph.removeAllEdges(linksNotMatchingFilterSettings);
		}
		return subgraph;
	}

	public Set<Link> getLinksNotMatchingFilterSettings(Set<Link> links) {
		Set<Link> linksNotMatchingFilterSettings = new HashSet<>();
		for (Link link : links) {
			if (filterSettings.getLinkTypes().parallelStream()
					.noneMatch(selectedType -> selectedType.toLowerCase().startsWith(link.getTypeAsString()))) {
				linksNotMatchingFilterSettings.add(link);
			}
		}
		return linksNotMatchingFilterSettings;
	}

	private KnowledgeGraph ensureThatFilteredGraphHasCorrectLinkDistance(KnowledgeGraph filteredGraph) {
		SingleSourcePaths<KnowledgeElement, Link> paths = filteredGraph
				.getShortestPathAlgorithm(filterSettings.getLinkDistance())
				.getPaths(filterSettings.getSelectedElement());
		Set<KnowledgeElement> reachableElements = ((TreeSingleSourcePathsImpl<KnowledgeElement, Link>) paths)
				.getDistanceAndPredecessorMap().keySet();
		return filteredGraph.getMutableSubgraphFor(reachableElements);
	}

	/**
	 * @param element
	 *            {@link KnowledgeElement} object.
	 * @return true if the element matches the specified filter criteria in the
	 *         {@link FilterSettings}.
	 */
	public boolean isElementMatchingFilterSettings(KnowledgeElement element) {
		if (!isElementMatchingKnowledgeTypeFilter(element)) {
			return false;
		}
		if (!isElementMatchingTimeFilter(element)) {
			return false;
		}
		if (!isElementMatchingDocumentationLocationFilter(element)) {
			return false;
		}
		if (!isElementMatchingStatusFilter(element) && !filterSettings.isOnlyIncompleteKnowledgeShown()) {
			return false;
		}
		if (!isElementMatchingDocumentationCompletenessFilter(element)) {
			return false;
		}
		if (!isElementMatchingDecisionGroupFilter(element)) {
			return false;
		}
		if (!isElementMatchingIsTestCodeFilter(element)) {
			return false;
		}
		if (!isElementMatchingDegreeFilter(element)) {
			return false;
		}
		return isElementMatchingSubStringFilter(element);
	}

	/**
	 * @param element
	 *            {@link KnowledgeElement} object.
	 * @return true if the element is documented in one of the given
	 *         {@link DocumentationLocation}s in the {@link FilterSettings}.
	 */
	public boolean isElementMatchingDocumentationLocationFilter(KnowledgeElement element) {
		return filterSettings.getDocumentationLocations().contains(element.getDocumentationLocation());
	}

	/**
	 * @param element
	 *            {@link KnowledgeElement} object.
	 * @return true if the element's status equals one of the given
	 *         {@link KnowledgeStatus} in the {@link FilterSettings}.
	 */
	public boolean isElementMatchingStatusFilter(KnowledgeElement element) {
		return filterSettings.getStatus().contains(element.getStatus());
	}

	/**
	 * @param element
	 *            {@link KnowledgeElement} object.
	 * @return true if the element is created or updated in the given time frame in
	 *         the {@link FilterSettings}. See
	 *         {@link KnowledgeElement#getCreationDate()} and
	 *         {@link KnowledgeElement#getUpdatingDate()}.
	 */
	public boolean isElementMatchingTimeFilter(KnowledgeElement element) {
		boolean isMatchingTimeFilter = true;
		if (filterSettings.getStartDate() > 0) {
			isMatchingTimeFilter = element.getUpdatingDate().getTime() >= filterSettings.getStartDate();
		}
		if (filterSettings.getEndDate() > 0) {
			isMatchingTimeFilter = isMatchingTimeFilter
					&& element.getCreationDate().getTime() <= filterSettings.getEndDate() + 86400000;
		}
		return isMatchingTimeFilter;
	}

	/**
	 * @param element
	 *            {@link KnowledgeElement} object.
	 * @return true if the element's description, summary, or key contains the given
	 *         substring in the {@link FilterSettings}.
	 */
	public boolean isElementMatchingSubStringFilter(KnowledgeElement element) {
		String searchString = filterSettings.getSearchTerm().toLowerCase();
		if (searchString.isBlank()) {
			return true;
		}
		if (JiraQueryType.getJiraQueryType(searchString) != JiraQueryType.OTHER) {
			// JQL string or filter
			return true;
		}
		if (element.getDescription() != null && element.getDescription().toLowerCase().contains(searchString)) {
			return true;
		}
		if (element.getSummary() != null && element.getSummary().toLowerCase().contains(searchString)) {
			return true;
		}
		return element.getKey().toLowerCase().contains(searchString);
	}

	/**
	 * @param element
	 *            {@link KnowledgeElement} object.
	 * @return true if the element's type equals one of the given
	 *         {@link KnowledgeType}s in the {@link FilterSettings}.
	 */
	public boolean isElementMatchingKnowledgeTypeFilter(KnowledgeElement element) {
		String type = element.getType().replaceProAndConWithArgument().toString();
		if (element.getType() == KnowledgeType.OTHER || element.getType() == KnowledgeType.CODE) {
			if (filterSettings.isOnlyDecisionKnowledgeShown()) {
				return false;
			}
			type = element.getTypeAsString();
		}
		return filterSettings.getKnowledgeTypes().contains(type);
	}

	/**
	 * @param element
	 *            {@link KnowledgeElement} object.
	 * @return true if the element's groups are equal to the given groups in the
	 *         {@link FilterSettings}.
	 */
	public boolean isElementMatchingDecisionGroupFilter(KnowledgeElement element) {
		List<String> selectedGroups = filterSettings.getDecisionGroups();
		if (selectedGroups.isEmpty()) {
			return true;
		}

		List<String> groups = element.getDecisionGroups();

		int matches = 0;
		for (String group : selectedGroups) {
			if (groups.contains(group)) {
				matches++;
			}
		}
		return matches == selectedGroups.size();
	}

	/**
	 * @param element
	 *            {@link KnowledgeElement} object.
	 * @return true if the element's degree (i.e. number of links) is in between
	 *         minDegree and maxDegree in the {@link FilterSettings}.
	 */
	public boolean isElementMatchingDegreeFilter(KnowledgeElement element) {
		if (filterSettings.getMinDegree() > 0) {
			int degree = element.getLinks().size();
			return degree >= filterSettings.getMinDegree() && degree <= filterSettings.getMaxDegree();
		}
		return true;
	}

	/**
	 * @param element
	 *            {@link ChangedFile} object. (A {@link ChangedFile} is a specific
	 *            {@link KnowledgeElement}.)
	 * @return true if the element is a test class.
	 */
	public boolean isElementMatchingIsTestCodeFilter(KnowledgeElement element) {
		if (element.getType() != KnowledgeType.CODE) {
			return true;
		}
		return filterSettings.isTestCodeShown() || !element.getSummary().contains("Test");
	}

	/**
	 * @param element
	 *            {@link KnowledgeElement} object.
	 * @return always true if
	 *         {@link FilterSettings#isOnlyIncompleteKnowledgeShown()} is false.
	 *         True if the element is incompletely documented according to the
	 *         {@link DefinitionOfDone} (checked by {@link KnowledgeElementCheck})
	 *         and only incomplete knowledge elements should be shown
	 *         ({@link FilterSettings#isOnlyIncompleteKnowledgeShown()} is true).
	 *         False otherwise.
	 */
	public boolean isElementMatchingDocumentationCompletenessFilter(KnowledgeElement element) {
		return !filterSettings.isOnlyIncompleteKnowledgeShown() || element.failsDefinitionOfDone(filterSettings);
	}

	/**
	 * @return {@link FilterSettings} object (=filter criteria) that the filtering
	 *         manager uses.
	 */
	public FilterSettings getFilterSettings() {
		return filterSettings;
	}

	/**
	 * @param filterSettings
	 *            {@link FilterSettings} object (=filter criteria) that the
	 *            filtering manager uses.
	 */
	public void setFilterSettings(FilterSettings filterSettings) {
		this.filterSettings = filterSettings;
	}
}
