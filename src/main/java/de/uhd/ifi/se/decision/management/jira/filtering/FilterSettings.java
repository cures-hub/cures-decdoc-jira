package de.uhd.ifi.se.decision.management.jira.filtering;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlElement;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.user.ApplicationUser;

import de.uhd.ifi.se.decision.management.jira.model.DecisionKnowledgeProject;
import de.uhd.ifi.se.decision.management.jira.model.DocumentationLocation;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeElement;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeStatus;
import de.uhd.ifi.se.decision.management.jira.model.LinkType;
import de.uhd.ifi.se.decision.management.jira.persistence.KnowledgePersistenceManager;
import de.uhd.ifi.se.decision.management.jira.persistence.singlelocations.AbstractPersistenceManagerForSingleLocation;
import de.uhd.ifi.se.decision.management.jira.persistence.singlelocations.CodeClassPersistenceManager;

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
	private Set<String> jiraIssueTypes;
	private List<KnowledgeStatus> knowledgeStatus;
	private List<String> linkTypes;
	private List<String> decisionGroups;
	private boolean isOnlyDecisionKnowledgeShown;
	private boolean isTestCodeShown;
	private int linkDistance;
	private int minDegree;
	private int maxDegree;
	private KnowledgeElement selectedElement;

	@XmlElement
	private long startDate;
	@XmlElement
	private long endDate;

	@JsonCreator
	public FilterSettings(@JsonProperty("projectKey") String projectKey,
			@JsonProperty("searchTerm") String searchTerm) {
		this.project = new DecisionKnowledgeProject(projectKey);
		setSearchTerm(searchTerm);
		this.jiraIssueTypes = project.getJiraIssueTypeNames();
		this.linkTypes = LinkType.toStringList();
		this.startDate = -1;
		this.endDate = -1;
		this.documentationLocations = DocumentationLocation.getAllDocumentationLocations();
		this.knowledgeStatus = KnowledgeStatus.getAllKnowledgeStatus();
		this.decisionGroups = Collections.emptyList();
		this.isOnlyDecisionKnowledgeShown = false;
		this.isTestCodeShown = false;
		this.linkDistance = 3;
		this.minDegree = 0;
		this.maxDegree = 50;
	}

	public FilterSettings(String projectKey, String query, ApplicationUser user) {
		this(projectKey, query);

		// The JiraQueryHandler parses a Jira query into attributes of this class
		JiraQueryHandler queryHandler = new JiraQueryHandler(user, projectKey, query);
		this.searchTerm = queryHandler.getQuery();

		Set<String> namesOfJiraIssueTypesInQuery = queryHandler.getNamesOfJiraIssueTypesInQuery();
		if (!namesOfJiraIssueTypesInQuery.isEmpty()) {
			this.jiraIssueTypes = namesOfJiraIssueTypesInQuery;
		}

		this.startDate = queryHandler.getCreatedEarliest();
		this.endDate = queryHandler.getCreatedLatest();
	}

	/**
	 * @return key of the Jira project.
	 */
	public String getProjectKey() {
		return project.getProjectKey();
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
	 * @return search term. This string can also be a Jira Query or a predefined
	 *         {@link JiraFilter} (e.g. allopenissues).
	 */
	public String getSearchTerm() {
		return searchTerm;
	}

	/**
	 * @param searchTerm
	 *            search term. This string can also be a Jira Query or a predefined
	 *            {@link JiraFilter} (e.g. allopenissues).
	 */
	@JsonProperty("searchTerm")
	public void setSearchTerm(String searchTerm) {
		this.searchTerm = searchTerm != null ? searchTerm : "";
	}

	/**
	 * @return earliest creation date of an element to be included in the
	 *         filter/shown in the knowledge graph.
	 */
	public long getCreatedEarliest() {
		return startDate;
	}

	/**
	 * @param createdEarliest
	 *            earliest creation date of an element to be included in the
	 *            filter/shown in the knowledge graph.
	 */
	@JsonProperty("createdEarliest")
	public void setCreatedEarliest(long createdEarliest) {
		this.startDate = createdEarliest;
	}

	/**
	 * @return latest creation date of an element to be included in the filter/shown
	 *         in the knowledge graph.
	 */
	public long getCreatedLatest() {
		return endDate;
	}

	/**
	 * @param createdLatest
	 *            latest creation date of an element to be included in the
	 *            filter/shown in the knowledge graph.
	 */
	@JsonProperty("createdLatest")
	public void setCreatedLatest(long createdLatest) {
		this.endDate = createdLatest;
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
		List<String> documentationLocations = new ArrayList<String>();
		for (DocumentationLocation location : getDocumentationLocations()) {
			documentationLocations.add(DocumentationLocation.getName(location));
		}
		return documentationLocations;
	}

	/**
	 * @param documentationLocations
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
	 * @return list of knowledge types to be shown in the knowledge graph.
	 */
	@XmlElement(name = "jiraIssueTypes")
	public Set<String> getJiraIssueTypes() {
		return jiraIssueTypes;
	}

	/**
	 * @param namesOfTypes
	 *            list of names of Jira {@link IssueType}s as Strings.
	 */
	@JsonProperty("jiraIssueTypes")
	public void setJiraIssueTypes(Set<String> namesOfTypes) {
		jiraIssueTypes = namesOfTypes != null ? namesOfTypes : project.getJiraIssueTypeNames();
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
		knowledgeStatus = new ArrayList<KnowledgeStatus>();
		if (status == null) {
			for (KnowledgeStatus eachStatus : KnowledgeStatus.values()) {
				knowledgeStatus.add(eachStatus);
			}
			return;
		}
		for (String stringStatus : status) {
			knowledgeStatus.add(KnowledgeStatus.getKnowledgeStatus(stringStatus));
		}
	}

	/**
	 * @return list of {@link LinkType}s to be shown in the knowledge graph as
	 *         strings.
	 */
	@XmlElement(name = "linkTypes")
	public List<String> getLinkTypes() {
		return linkTypes;
	}

	/**
	 * @param namesOfTypes
	 *            list of {@link LinkType}s to be shown in the knowledge graph as
	 *            strings.
	 */
	@JsonProperty("linkTypes")
	public void setLinkTypes(List<String> namesOfTypes) {
		linkTypes = namesOfTypes != null ? namesOfTypes : LinkType.toStringList();
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
	public int getLinkDistance() {
		return linkDistance;
	}

	/**
	 * @param linkDistance
	 *            nodes within this distance from the start node are included in the
	 *            filtered graph. All nodes with a greater distance are not
	 *            included.
	 */
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
	 * @return {@link KnowledgeElement} that is currently selected (e.g. as root
	 *         element in the knowlegde tree view). For example, this can be a Jira
	 *         issue such as a work item, bug report or requirement.
	 */
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
		AbstractPersistenceManagerForSingleLocation persistenceManager;
		if (elementKey.contains(":")) {
			persistenceManager = KnowledgePersistenceManager.getOrCreate(project)
					.getManagerForSingleLocation(DocumentationLocation.JIRAISSUETEXT);
		} else {
			persistenceManager = KnowledgePersistenceManager.getOrCreate(project).getJiraIssueManager();
		}
		selectedElement = persistenceManager.getKnowledgeElement(elementKey);
		if (selectedElement == null || selectedElement.getKey() == null) {
			CodeClassPersistenceManager ccManager = new CodeClassPersistenceManager(project.getProjectKey());
			selectedElement = ccManager.getKnowledgeElement(elementKey);
		}
	}
}