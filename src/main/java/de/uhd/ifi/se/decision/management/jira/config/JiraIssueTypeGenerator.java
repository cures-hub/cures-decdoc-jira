package de.uhd.ifi.se.decision.management.jira.config;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.ofbiz.core.entity.GenericValue;

import com.atlassian.configurable.ValuesGenerator;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.fields.config.manager.IssueTypeSchemeManager;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;

/**
 * Provides a list of JIRA issue types for the decision knowledge report. The
 * user needs to select one from this list.
 */
public class JiraIssueTypeGenerator implements ValuesGenerator<IssueType> {

	@Override
	@SuppressWarnings("rawtypes")
	// @issue: How can we get the project id for the selected project? Is the
	// projectId part of params?
	public Map<IssueType, String> getValues(Map params) {
		GenericValue valueProject = (GenericValue) params.get("project");
		long projectId = (long) valueProject.get("id");

		Collection<IssueType> jiraIssueTypesList = getJiraIssueTypes(projectId);
		Map<IssueType, String> jiraIssueTypes = new HashMap<IssueType, String>();

		// IssueTypeManager issueTypeManager =
		// ComponentAccessor.getComponent(IssueTypeManager.class);
		// Collection<IssueType> types = issueTypeManager.getIssueTypes();

		for (IssueType type : jiraIssueTypesList) {
			jiraIssueTypes.put(type, type.getName());
		}

		return jiraIssueTypes;
	}

	public static Collection<IssueType> getJiraIssueTypes(long projectId) {
		IssueTypeSchemeManager issueTypeSchemeManager = ComponentAccessor.getIssueTypeSchemeManager();
		Project project = ComponentAccessor.getProjectManager().getProjectObj(projectId);
		return issueTypeSchemeManager.getIssueTypesForProject(project);
	}

	public static String getJiraIssueTypeName(String typeId) {
		IssueType issueType = ComponentAccessor.getConstantsManager().getIssueType(typeId);
		return issueType.getName();
	}
}
