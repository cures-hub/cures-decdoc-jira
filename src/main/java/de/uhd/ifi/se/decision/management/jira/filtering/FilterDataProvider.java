package de.uhd.ifi.se.decision.management.jira.filtering;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.user.ApplicationUser;
import de.uhd.ifi.se.decision.management.jira.model.DocumentationLocation;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "issueTypesForDropdown")
@XmlAccessorType(XmlAccessType.FIELD)
public class FilterDataProvider {



	@XmlElement

	private List<String> allIssueTypes;
	@XmlElement
	private List<String> issueTypesMatchingFilter;
	@XmlElement
	private long startDate;
	@XmlElement
	private long endDate;
	@XmlElement
	private List<String> documentationLocations;

	public FilterDataProvider() {

	}

	public FilterDataProvider(String projectKey, String query, ApplicationUser user) {
		if ((query.matches("\\?jql=(.)+")) || (query.matches("\\?filter=(.)+"))) {
			GraphFiltering filter = new GraphFiltering(projectKey, query, user, false);
			filter.produceResultsFromQuery();
			this.allIssueTypes = new ArrayList<>();
			for (IssueType issueType : ComponentAccessor.getConstantsManager().getAllIssueTypeObjects()) {
				this.allIssueTypes.add(issueType.getName());
			}
			if (!filter.getIssueTypesInQuery().isEmpty()) {
				this.issueTypesMatchingFilter = filter.getIssueTypesInQuery();
			} else {
				this.issueTypesMatchingFilter = allIssueTypes;
			}
			this.startDate = filter.getStartDate();
			this.endDate = filter.getEndDate();
		} else {
			this.allIssueTypes = new ArrayList<>();
			this.issueTypesMatchingFilter = new ArrayList<>();
			for (IssueType issueType : ComponentAccessor.getConstantsManager().getAllIssueTypeObjects()) {
				this.allIssueTypes.add(issueType.getName());
				this.issueTypesMatchingFilter.add(issueType.getName());
			}
			this.startDate = -1;
			this.endDate = -1;
		}
		this.documentationLocations = new ArrayList<>();
		DocumentationLocation[] locations = DocumentationLocation.values();
		for (DocumentationLocation location : locations) {
			this.documentationLocations.add(DocumentationLocation.getName(location));
		}
	}

	public List<String> getAllIssueTypes() {
		return allIssueTypes;
	}

	public void setAllIssueTypes(List<String> allIssueTypes) {
		this.allIssueTypes = allIssueTypes;
	}

	public List<String> getIssueTypesMatchingFilter() {
		return issueTypesMatchingFilter;
	}

	public void setIssueTypesMatchingFilter(List<String> issueTypesMatchingFilter) {
		this.issueTypesMatchingFilter = issueTypesMatchingFilter;
	}

	public long getStartDate() {
		return startDate;
	}

	public void setStartDate(long startDate) {
		this.startDate = startDate;
	}

	public long getEndDate() {
		return endDate;
	}

	public void setEndDate(long endDate) {
		this.endDate = endDate;
	}

	public List<String> getDocumentationLocations() {return this.documentationLocations;}

	public void setDocumentationLocations(List<String> documentationLocations) {this.documentationLocations = documentationLocations;}
}