package de.uhd.ifi.se.decision.management.jira.filtering;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlElement;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.user.ApplicationUser;
import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import de.uhd.ifi.se.decision.management.jira.model.DecisionKnowledgeProject;
import de.uhd.ifi.se.decision.management.jira.model.DocumentationLocation;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeElement;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeGraph;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeStatus;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeType;
import de.uhd.ifi.se.decision.management.jira.model.LinkType;
import de.uhd.ifi.se.decision.management.jira.persistence.KnowledgePersistenceManager;
import de.uhd.ifi.se.decision.management.jira.persistence.singlelocations.AbstractPersistenceManagerForSingleLocation;
import de.uhd.ifi.se.decision.management.jira.quality.completeness.CompletenessCheck;
import de.uhd.ifi.se.decision.management.jira.view.vis.VisGraph;

/**
 * Represents the filter criteria. For example, the filter settings cover the
 * key of the selected project, the time frame, documentation locations, Jira
 * issue types, and decision knowledge types. The search term can contain a
 * query in Jira Query Language (JQL), a {@link JiraFilter} or a search string
 * specified in the frontend of the plug-in.
 */
public class FilterSettings {

	private DecisionKnowledgeProject project;
	private String searchTerm;
	private List<DocumentationLocation> documentationLocations;
	private Set<String> knowledgeTypes;
	private List<KnowledgeStatus> knowledgeStatus;
	private Set<String> linkTypes;
	private List<String> decisionGroups;
	private boolean isOnlyDecisionKnowledgeShown;
	private boolean isTestCodeShown;
	private boolean isIncompleteKnowledgeShown;
	private int linkDistance;
	private int minDegree;
	private int maxDegree;
	private KnowledgeElement selectedElement;
	private long startDate;
	private long endDate;
	private boolean isHierarchical;
	private boolean isIrrelevantTextShown;
	private boolean createTransitiveLinks;

	private static final Logger LOGGER = LoggerFactory.getLogger(FilterSettings.class);

	@JsonCreator
	public FilterSettings(@JsonProperty("projectKey") String projectKey,
			@JsonProperty("searchTerm") String searchTerm) {
		this.project = new DecisionKnowledgeProject(projectKey);
		setSearchTerm(searchTerm);

		// the following values are the default values of the filter criteria
		this.knowledgeTypes = project.getNamesOfKnowledgeTypes();
		this.linkTypes = DecisionKnowledgeProject.getNamesOfLinkTypes();
		this.startDate = -1;
		this.endDate = -1;
		this.documentationLocations = DocumentationLocation.getAllDocumentationLocations();
		this.knowledgeStatus = KnowledgeStatus.getAllKnowledgeStatus();
		this.decisionGroups = Collections.emptyList();
		this.isOnlyDecisionKnowledgeShown = false;
		this.isTestCodeShown = false;
		this.isIncompleteKnowledgeShown = false;
		this.linkDistance = 3;
		this.minDegree = 0;
		this.maxDegree = 50;
		this.isHierarchical = false;
		this.isIrrelevantTextShown = false;
		this.createTransitiveLinks = false;
	}

	public FilterSettings(String projectKey, String query, ApplicationUser user) {
		this(projectKey, query);

		// The JiraQueryHandler parses a Jira query into attributes of this class
		JiraQueryHandler queryHandler = new JiraQueryHandler(user, projectKey, query);
		this.searchTerm = queryHandler.getQuery();

		Set<String> namesOfJiraIssueTypesInQuery = queryHandler.getNamesOfJiraIssueTypesInQuery();
		if (!namesOfJiraIssueTypesInQuery.isEmpty()) {
			this.knowledgeTypes = namesOfJiraIssueTypesInQuery;
		}

		this.startDate = queryHandler.getCreatedEarliest();
		this.endDate = queryHandler.getCreatedLatest();
	}

	/**
	 * @return key of the Jira project.
	 */
	@XmlElement(name = "projectKey")
	public String getProjectKey() {
		return project != null ? project.getProjectKey() : "";
	}

	/**
	 * @param projectKey
	 *            of the Jira project.
	 */
	@JsonProperty("projectKey")
	public void setProjectKey(String projectKey) {
		this.project = new DecisionKnowledgeProject(projectKey);
	}

	/**
	 * @return search term. This string can be a substring filter. If the search
	 *         term start with "?jql=" oder "?filter=", it is a Jira Query or a
	 *         predefined {@link JiraFilter} (e.g. allopenissues).
	 */
	public String getSearchTerm() {
		return searchTerm;
	}

	/**
	 * @param searchTerm
	 *            can be a substring filter. If the search term start with "?jql="
	 *            oder "?filter=", it is a Jira Query or a predefined
	 *            {@link JiraFilter} (e.g. allopenissues).
	 */
	@JsonProperty("searchTerm")
	public void setSearchTerm(String searchTerm) {
		this.searchTerm = searchTerm != null ? searchTerm : "";
	}

	/**
	 * @return earliest creation or update date of an element to be included in the
	 *         filter/shown in the knowledge graph.
	 */
	public long getStartDate() {
		return startDate;
	}

	/**
	 * @param startDate
	 *            earliest creation or update date of an element to be included in
	 *            the filter/shown in the knowledge graph.
	 */
	@JsonProperty("startDate")
	public void setStartDate(long startDate) {
		this.startDate = startDate;
	}

	/**
	 * @return latest creation or update date of an element to be included in the
	 *         filter/shown in the knowledge graph.
	 */
	public long getEndDate() {
		return endDate;
	}

	/**
	 * @param endDate
	 *            latest creation or update date of an element to be included in the
	 *            filter/shown in the knowledge graph.
	 */
	@JsonProperty("endDate")
	public void setEndDate(long endDate) {
		this.endDate = endDate;
	}

	/**
	 * @return list of {@link DocumentationLocation}s to be shown in the knowledge
	 *         graph.
	 */
	public List<DocumentationLocation> getDocumentationLocations() {
		return documentationLocations;
	}

	/**
	 * @return list of {@link DocumentationLocation}s to be shown in the knowledge
	 *         graph as Strings.
	 */
	@XmlElement(name = "documentationLocations")
	public List<String> getNamesOfDocumentationLocations() {
		List<String> documentationLocations = new ArrayList<>();
		for (DocumentationLocation location : getDocumentationLocations()) {
			documentationLocations.add(DocumentationLocation.getName(location));
		}
		return documentationLocations;
	}

	/**
	 * @param namesOfDocumentationLocations
	 *            {@link DocumentationLocation}s to be shown in the knowledge graph
	 *            as Strings.
	 */
	@JsonProperty("documentationLocations")
	public void setDocumentationLocations(List<String> namesOfDocumentationLocations) {
		if (namesOfDocumentationLocations != null) {
			this.documentationLocations = new ArrayList<>();
			for (String location : namesOfDocumentationLocations) {
				this.documentationLocations.add(DocumentationLocation.getDocumentationLocationFromString(location));
			}
		} else {
			this.documentationLocations = DocumentationLocation.getAllDocumentationLocations();
		}
	}

	/**
	 * @return list of {@link KnowledgeStatus} types to be shown in the knowledge
	 *         graph as strings.
	 */
	@XmlElement(name = "status")
	public List<KnowledgeStatus> getStatus() {
		return knowledgeStatus;
	}

	/**
	 * @param status
	 *            list of {@link KnowledgeStatus} types to be shown in the knowledge
	 *            graph as strings.
	 */
	@JsonProperty("status")
	public void setStatus(List<String> status) {
		knowledgeStatus = new ArrayList<>();
		if (status == null) {
			Collections.addAll(knowledgeStatus, KnowledgeStatus.values());
			return;
		}
		for (String stringStatus : status) {
			if (stringStatus.equals("incomplete")) {
				continue;
			}
			knowledgeStatus.add(KnowledgeStatus.getKnowledgeStatus(stringStatus));
		}
	}

	/**
	 * @return {@link LinkType}s to be shown in the knowledge graph as strings.
	 */
	@XmlElement(name = "linkTypes")
	public Set<String> getLinkTypes() {
		return linkTypes;
	}

	/**
	 * @param namesOfTypes
	 *            {@link LinkType}s to be shown in the knowledge graph as strings.
	 */
	@JsonProperty("linkTypes")
	public void setLinkTypes(Set<String> namesOfTypes) {
		linkTypes = namesOfTypes != null ? namesOfTypes : DecisionKnowledgeProject.getNamesOfLinkTypes();
	}

	/**
	 * @param decGroups
	 *            list of names of all groups.
	 */
	@JsonProperty("groups")
	public void setDecisionGroups(List<String> decGroups) {
		decisionGroups = decGroups != null ? decGroups : Collections.emptyList();
	}

	/**
	 * @return list of names of all groups.
	 */
	@XmlElement(name = "groups")
	public List<String> getDecisionGroups() {
		return decisionGroups;
	}

	/**
	 * @return true if only decision knowledge elements are included in the filtered
	 *         graph. False if also requirements and other knowledge elements are
	 *         included.
	 */
	public boolean isOnlyDecisionKnowledgeShown() {
		return isOnlyDecisionKnowledgeShown;
	}

	/**
	 * @param isOnlyDecisionKnowledgeShown
	 *            true if only decision knowledge elements should be included in the
	 *            filtered graph. False if also requirements and other knowledge
	 *            elements are included.
	 */
	@JsonProperty("isOnlyDecisionKnowledgeShown")
	public void setOnlyDecisionKnowledgeShown(boolean isOnlyDecisionKnowledgeShown) {
		this.isOnlyDecisionKnowledgeShown = isOnlyDecisionKnowledgeShown;
	}

	/**
	 * @return maximal distance from the start node to nodes to be included in the
	 *         filtered graph. All nodes with a greater distance are not included.
	 */
	@XmlElement(name = "linkDistance")
	public int getLinkDistance() {
		return linkDistance;
	}

	/**
	 * @param linkDistance
	 *            nodes within this distance from the start node are included in the
	 *            filtered graph. All nodes with a greater distance are not
	 *            included. Also called "number of hops".
	 */
	@JsonProperty("linkDistance")
	public void setLinkDistance(int linkDistance) {
		this.linkDistance = linkDistance;
	}

	/**
	 * @return minimal number of links that a knowledge element (=node) needs to
	 *         have to be included in the filtered graph.
	 */
	public int getMinDegree() {
		return minDegree;
	}

	/**
	 * @param minDegree
	 *            minimal number of links that a knowledge element (=node) needs to
	 *            have to be included in the filtered graph.
	 */
	public void setMinDegree(int minDegree) {
		this.minDegree = minDegree;
	}

	/**
	 * @return maximal number of links that a knowledge element (=node) needs to
	 *         have to be included in the filtered graph.
	 */
	public int getMaxDegree() {
		return maxDegree;
	}

	/**
	 * @param maxDegree
	 *            maximal number of links that a knowledge element (=node) needs to
	 *            have to be included in the filtered graph.
	 */
	public void setMaxDegree(int maxDegree) {
		this.maxDegree = maxDegree;
	}

	/**
	 * @return true if code classes for unit tests are shown in the filtered graph.
	 */
	public boolean isTestCodeShown() {
		return isTestCodeShown;
	}

	/**
	 * @param isTestCodeShown
	 *            true if code classes for unit tests are shown in the filtered
	 *            graph.
	 */
	@JsonProperty("isTestCodeShown")
	public void setTestCodeShown(boolean isTestCodeShown) {
		this.isTestCodeShown = isTestCodeShown;
	}

	/**
	 * @return true if incompletely documented knowledge elements are shown in the
	 *         filtered graph.
	 * 
	 * @see CompletenessCheck
	 */
	public boolean isIncompleteKnowledgeShown() {
		return isIncompleteKnowledgeShown;
	}

	/**
	 * @param isIncompleteKnowledgeShown
	 *            true if incompletely documented knowledge elements should be shown
	 *            in the filtered graph.
	 * 
	 * @see CompletenessCheck
	 */
	@JsonProperty("isIncompleteKnowledgeShown")
	public void setIncompleteKnowledgeShown(boolean isIncompleteKnowledgeShown) {
		this.isIncompleteKnowledgeShown = isIncompleteKnowledgeShown;
	}

	/**
	 * @return {@link KnowledgeElement} that is currently selected (e.g. as root
	 *         element in the knowlegde tree view). For example, this can be a Jira
	 *         issue such as a work item, bug report or requirement.
	 */
	@JsonFilter(value = "selectedElementFilter")
	public KnowledgeElement getSelectedElement() {
		return selectedElement;
	}

	/**
	 * @param selectedElement
	 *            {@link KnowledgeElement} that is currently selected (e.g. as root
	 *            element in the knowlegde tree view). For example, this can be a
	 *            Jira issue such as a work item, bug report or requirement.
	 */
	public void setSelectedElement(KnowledgeElement selectedElement) {
		this.selectedElement = selectedElement;
	}

	/**
	 * @param elementKey
	 *            key of the {@link KnowledgeElement} that is currently selected
	 *            (e.g. as root element in the knowlegde tree view). For example,
	 *            this can be the key of a Jira issue such as a work item, bug
	 *            report or requirement, e.g. CONDEC-123.
	 * 
	 * @issue How can we identify knowledge elements from different documentation
	 *        locations?
	 * 
	 *        TODO Solve this issue and make code class recognition more explicit
	 */
	@JsonProperty("selectedElement")
	public void setSelectedElement(String elementKey) {
		if (elementKey == null || elementKey.isBlank()) {
			return;
		}
		if (elementKey.contains(":graph")) {
			// not in database, only in RAM (singleton KnowledgeGraph object)
			selectedElement = KnowledgeGraph.getInstance(elementKey.split(":")[0]).getElement(elementKey);
			return;
		}
		AbstractPersistenceManagerForSingleLocation persistenceManager;
		if (elementKey.contains(":code")) {
			persistenceManager = KnowledgePersistenceManager.getOrCreate(project)
					.getManagerForSingleLocation(DocumentationLocation.CODE);
		} else if (elementKey.contains(":")) {
			persistenceManager = KnowledgePersistenceManager.getOrCreate(project)
					.getManagerForSingleLocation(DocumentationLocation.JIRAISSUETEXT);
		} else {
			persistenceManager = KnowledgePersistenceManager.getOrCreate(project).getJiraIssueManager();
		}
		selectedElement = persistenceManager.getKnowledgeElement(elementKey);
	}

	/**
	 * @return list of selected {@link KnowledgeType}s to be shown in the knowledge
	 *         graph.
	 */
	@XmlElement(name = "knowledgeTypes")
	public Set<String> getKnowledgeTypes() {
		if (isIrrelevantTextShown) {
			knowledgeTypes.add(KnowledgeType.OTHER.toString());
		}
		return knowledgeTypes;
	}

	/**
	 * @param namesOfTypes
	 *            names of {@link KnowledgeType}s, such as decision knowledge types
	 *            and other Jira {@link IssueType}s.
	 */
	@JsonProperty("knowledgeTypes")
	public void setKnowledgeTypes(Set<String> namesOfTypes) {
		knowledgeTypes = namesOfTypes != null ? namesOfTypes : project.getNamesOfKnowledgeTypes();
	}

	/**
	 * @return true if the {@link KnowledgeGraph} or a respective subgraph provided
	 *         by the {@link FilteringManager} should be shown with a hierarchy of
	 *         nodes. This is used in the {@link VisGraph}.
	 */
	public boolean isHierarchical() {
		return isHierarchical;
	}

	/**
	 * @param isHierarchical
	 *            true if the {@link KnowledgeGraph} or a respective subgraph
	 *            provided by the {@link FilteringManager} should be shown with a
	 *            hierarchy of nodes. This is used in the {@link VisGraph}.
	 */
	@JsonProperty("isHierarchical")
	public void setHierarchical(boolean isHierarchical) {
		this.isHierarchical = isHierarchical;
	}

	/**
	 * @return true if sentences that are not classified as decision knowledge
	 *         elements should be included in the filtered knowledge graph.
	 */
	public boolean isIrrelevantTextShown() {
		return isIrrelevantTextShown;
	}

	/**
	 * @param isIrrelevantTextShown
	 *            true if sentences that are not classified as decision knowledge
	 *            elements should be included in the filtered knowledge graph.
	 */
	@JsonProperty("isIrrelevantTextShown")
	public void setIrrelevantTextShown(boolean isIrrelevantTextShown) {
		this.isIrrelevantTextShown = isIrrelevantTextShown;
	}

	/**
	 * @return true if the {@link KnowledgeGraph} or a respective subgraph provided
	 *         by the {@link FilteringManager} should contain transitive links as a
	 *         replacement for knowledge elements removed by filters.
	 */
	public boolean createTransitiveLinks() {
		return createTransitiveLinks;
	}

	/**
	 * @param createTransitiveLinks
	 *            true if the {@link KnowledgeGraph} or a respective subgraph
	 *            provided by the {@link FilteringManager} should contain transitive
	 *            links as a replacement for knowledge elements removed by filters.
	 */
	@JsonProperty("createTransitiveLinks")
	public void setCreateTransitiveLinks(boolean createTransitiveLinks) {
		this.createTransitiveLinks = createTransitiveLinks;
	}

	@Override
	public String toString() {
		SimpleFilterProvider filterProvider = new SimpleFilterProvider();
		filterProvider.addFilter("selectedElementFilter", SimpleBeanPropertyFilter.filterOutAllExcept("key"));
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.setFilterProvider(filterProvider);
		String filterSettingsAsJson = "";
		try {
			filterSettingsAsJson = objectMapper.writeValueAsString(this);
		} catch (IOException e) {
			LOGGER.error(e.getMessage());
		}
		return filterSettingsAsJson;
	}
}