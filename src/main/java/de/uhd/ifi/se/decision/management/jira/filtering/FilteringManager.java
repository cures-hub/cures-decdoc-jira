package de.uhd.ifi.se.decision.management.jira.filtering;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jgrapht.Graph;
import org.jgrapht.graph.AsSubgraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.link.IssueLinkType;
import com.atlassian.jira.issue.link.IssueLinkTypeManager;
import com.atlassian.jira.user.ApplicationUser;

import de.uhd.ifi.se.decision.management.jira.model.DocumentationLocation;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeElement;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeGraph;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeStatus;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeType;
import de.uhd.ifi.se.decision.management.jira.model.Link;

/**
 * Filters the {@link KnowledgeGraph}. The filter criteria are specified in the
 * {@link FilterSettings} class.
 */
public class FilteringManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(FilteringManager.class);
	private ApplicationUser user;
	private FilterSettings filterSettings;
	private KnowledgeGraph graph;

	public FilteringManager(FilterSettings filterSettings) {
		this(null, filterSettings);
	}

	public FilteringManager(ApplicationUser user, FilterSettings filterSettings) {
		this.user = user;
		this.filterSettings = filterSettings;
		if (filterSettings != null) {
			this.graph = KnowledgeGraph.getOrCreate(filterSettings.getProjectKey());
		}
	}

	public FilteringManager(String projectKey, ApplicationUser user, String query) {
		this(user, new FilterSettings(projectKey, query, user));
	}

	/**
	 * @return all knowledge elements that match the {@link FilterSetting}s.
	 */
	public Set<KnowledgeElement> getElementsMatchingFilterSettings() {
		if (filterSettings == null || filterSettings.getProjectKey() == null || graph == null) {
			LOGGER.error("FilteringManager misses important attributes.");
			return new HashSet<KnowledgeElement>();
		}
		Set<KnowledgeElement> elements = new HashSet<>();
		if (filterSettings.getSelectedElement() != null) {
			graph.addVertex(filterSettings.getSelectedElement());
			elements = getElementsInLinkDistance();
		} else {
			elements = graph.vertexSet();
		}
		elements = filterElements(elements);
		if (filterSettings.getSelectedElement() != null) {
			elements.add(filterSettings.getSelectedElement());
		}
		return elements;
	}

	/**
	 * @return subgraph of the {@link KnowledgeGraph} that matches the
	 *         {@link FilterSetting}s.
	 */
	public Graph<KnowledgeElement, Link> getSubgraphMatchingFilterSettings() {
		if (filterSettings == null || filterSettings.getProjectKey() == null || graph == null) {
			LOGGER.error("FilteringManager misses important attributes.");
			return null;
		}
		Set<KnowledgeElement> elements = getElementsMatchingFilterSettings();
		Graph<KnowledgeElement, Link> subgraph = new AsSubgraph<>(graph, elements);

		IssueLinkTypeManager linkTypeManager = ComponentAccessor.getComponent(IssueLinkTypeManager.class);
		Collection<IssueLinkType> types = linkTypeManager.getIssueLinkTypes(false);
		if (filterSettings.getLinkTypes().size() < types.size()) {
			Set<Link> linksNotMatchingFilterSettings = getLinksNotMatchingFilterSettings(subgraph.edgeSet());
			subgraph.removeAllEdges(linksNotMatchingFilterSettings);
		}
		return subgraph;
	}

	private Set<KnowledgeElement> getElementsInLinkDistance() {
		KnowledgeElement selectedElement = filterSettings.getSelectedElement();
		int linkDistance = filterSettings.getLinkDistance();
		Set<KnowledgeElement> elements = new HashSet<KnowledgeElement>();
		elements.addAll(getLinkedElements(selectedElement, linkDistance));
		return elements;
	}

	private Set<KnowledgeElement> getLinkedElements(KnowledgeElement currentElement, int currentDistance) {
		Set<KnowledgeElement> elements = new HashSet<KnowledgeElement>();
		Set<Link> traversedLinks = new HashSet<>();
		elements.add(currentElement);

		if (currentDistance == 0) {
			return elements;
		}
		for (Link link : graph.edgesOf(currentElement)) {
			if (!traversedLinks.add(link)) {
				continue;
			}
			KnowledgeElement oppositeElement = link.getOppositeElement(currentElement);
			if (oppositeElement == null) {
				continue;
			}
			elements.addAll(getLinkedElements(oppositeElement, currentDistance - 1));
		}
		return elements;
	}

	private Set<Link> getLinksNotMatchingFilterSettings(Set<Link> links) {
		Set<Link> linksNotMatchingFilterSettings = new HashSet<Link>();
		for (Link link : links) {
			if (!filterSettings.getLinkTypes().contains(link.getType())) {
				linksNotMatchingFilterSettings.add(link);
			}
		}
		return linksNotMatchingFilterSettings;
	}

	private Set<KnowledgeElement> filterElements(Set<KnowledgeElement> elements) {
		Set<KnowledgeElement> filteredElements = new HashSet<KnowledgeElement>();
		if (elements == null || elements.isEmpty()) {
			return filteredElements;
		}
		for (KnowledgeElement element : elements) {
			if (isElementMatchingFilterSettings(element)) {
				filteredElements.add(element);
			}
		}
		return filteredElements;
	}

	/**
	 * @param element
	 *            {@link KnowledgeElement} object.
	 * @return true if the element matches the specified filter criteria in the
	 *         {@link FilterSetting}s.
	 */
	public boolean isElementMatchingFilterSettings(KnowledgeElement element) {
		if (!isElementMatchingKnowledgeTypeFilter(element)) {
			return false;
		}
		if (!isElementMatchingTimeFilter(element)) {
			return false;
		}
		if (!isElementMatchingStatusFilter(element)) {
			return false;
		}
		if (!isElementMatchingDocumentationLocationFilter(element)) {
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
	 *         {@link DocumentationLocation}s in the {@link FilterSetting}s.
	 */
	public boolean isElementMatchingDocumentationLocationFilter(KnowledgeElement element) {
		return filterSettings.getDocumentationLocations().contains(element.getDocumentationLocation());
	}

	/**
	 * @param element
	 *            {@link KnowledgeElement} object.
	 * @return true if the element's status equals one of the given
	 *         {@link KnowledgeStatus} in the {@link FilterSetting}s.
	 */
	public boolean isElementMatchingStatusFilter(KnowledgeElement element) {
		return filterSettings.getStatus().contains(element.getStatus());
	}

	/**
	 * @param element
	 *            {@link KnowledgeElement} object.
	 * @return true if the element is created in the given time frame in the
	 *         {@link FilterSetting}s. See {@link KnowledgeElement#getCreated()}.
	 */
	public boolean isElementMatchingTimeFilter(KnowledgeElement element) {
		boolean isMatchingTimeFilter = true;
		if (filterSettings.getCreatedEarliest() != -1) {
			isMatchingTimeFilter = element.getCreated().getTime() >= filterSettings.getCreatedEarliest();
		}
		if (filterSettings.getCreatedLatest() != -1) {
			isMatchingTimeFilter = isMatchingTimeFilter
					&& element.getCreated().getTime() <= filterSettings.getCreatedLatest() + 86400000;
		}
		return isMatchingTimeFilter;
	}

	/**
	 * @param element
	 *            {@link KnowledgeElement} object.
	 * @return true if the element's description, summary, or key contains the given
	 *         substring in the {@link FilterSetting}s.
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
		return element.getKey() != null && element.getKey().toLowerCase().contains(searchString);
	}

	/**
	 * @param element
	 *            {@link KnowledgeElement} object.
	 * @return true if the element's type equals one of the given
	 *         {@link KnowledgeType}s in the {@link FilterSetting}s.
	 */
	public boolean isElementMatchingKnowledgeTypeFilter(KnowledgeElement element) {
		String type = element.getType().replaceProAndConWithArgument().toString();
		if (element.getType() == KnowledgeType.OTHER) {
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
	 *         {@link FilterSetting}s.
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
	 *         minDegree and maxDegree in the {@link FilterSetting}s.
	 */
	public boolean isElementMatchingDegreeFilter(KnowledgeElement element) {
		int degree = element.getLinks().size();
		return degree >= filterSettings.getMinDegree() && degree <= filterSettings.getMaxDegree();
	}

	/**
	 * @param element
	 *            {@link KnowledgeElement} object.
	 * @return true if the element is a test class.
	 */
	public boolean isElementMatchingIsTestCodeFilter(KnowledgeElement element) {
		// TODO Make code class recognition more explicit
		if (element.getDocumentationLocation() != DocumentationLocation.COMMIT) {
			return true;
		}
		if (!element.getSummary().contains(".java")) {
			return true;
		}
		return filterSettings.isTestCodeShown() || !element.getSummary().startsWith("Test");
	}

	/**
	 * @return {@link FilterSettings} object (=filter criteria) that the filtering
	 *         manager uses.
	 */
	public FilterSettings getFilterSettings() {
		return this.filterSettings;
	}

	/**
	 * @param filterSettings
	 *            {@link FilterSettings} object (=filter criteria) that the
	 *            filtering manager uses.
	 */
	public void setFilterSettings(FilterSettings filterSettings) {
		this.filterSettings = filterSettings;
	}

	/**
	 * @return {@link ApplicationUser} who performs filtering.
	 */
	public ApplicationUser getUser() {
		return user;
	}

	/**
	 * @param user
	 *            {@link ApplicationUser} object who performs filtering. The user
	 *            needs to have the rights to query the database.
	 */
	public void setUser(ApplicationUser user) {
		this.user = user;
	}
}
