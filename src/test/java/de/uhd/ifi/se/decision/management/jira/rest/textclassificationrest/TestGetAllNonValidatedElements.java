package de.uhd.ifi.se.decision.management.jira.rest.textclassificationrest;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.mock.servlet.MockHttpServletRequest;
import com.google.common.collect.ImmutableMap;
import de.uhd.ifi.se.decision.management.jira.TestSetUp;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeElement;
import de.uhd.ifi.se.decision.management.jira.rest.TextClassificationRest;
import de.uhd.ifi.se.decision.management.jira.testdata.JiraIssues;
import de.uhd.ifi.se.decision.management.jira.testdata.JiraUsers;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class TestGetAllNonValidatedElements extends TestSetUp {

	private HttpServletRequest request;
	private TextClassificationRest classificationRest;
	private List<Issue> jiraIssues;

	@Before
	public void setUp() {
		init();
		classificationRest = new TextClassificationRest();
		request = new MockHttpServletRequest();
		request.setAttribute("user", JiraUsers.SYS_ADMIN.getApplicationUser());
		jiraIssues = JiraIssues.getTestJiraIssues();

	}

	@Test
	public void testValidRequestNullProjectKeyNullIssueKey() {
		assertEquals(Response.Status.BAD_REQUEST.getStatusCode(),
			classificationRest.getAllNonValidatedElements(request, null).getStatus());

	}

	@Test
	public void testValidWithNoNonValidatedElements() {
		Response response = classificationRest.getAllNonValidatedElements(request, jiraIssues.get(0).getProjectObject().getKey());

		ImmutableMap<String, List<KnowledgeElement>> expected = ImmutableMap.of("nonValidatedElements", new ArrayList<>());

		assertEquals(expected, response.getEntity());
	}

//	@Test
//	@NonTransactional
//	public void testValidWithNonValidatedElements() {
//		Issue issue = jiraIssues.get(1);
//
//	}
////
//	@Test
//	@NonTransactional
//	public void testValidWithNonValidatedAndValidatedElements() {
//		Issue issue = jiraIssues.get(0);
//
//
//		PartOfJiraIssueText nonValidatedElement1 = JiraIssues.addElementToDataBase(12237, KnowledgeType.ARGUMENT);
//		nonValidatedElement1.setJiraIssue(JiraIssuePersistenceManager.getJiraIssue("TEST-4"));
//		nonValidatedElement1.setValidated(false);
//
//		PartOfJiraIssueText nonValidatedElement2 = JiraIssues.addElementToDataBase(12238, KnowledgeType.ARGUMENT);
//		nonValidatedElement2.setJiraIssue(JiraIssuePersistenceManager.getJiraIssue("TEST-4"));
//		nonValidatedElement2.setValidated(false);
//
//		PartOfJiraIssueText validatedElement = JiraIssues.addElementToDataBase(12239, KnowledgeType.ARGUMENT);
//		validatedElement.setJiraIssue(JiraIssuePersistenceManager.getJiraIssue("TEST-4"));
//		validatedElement.setValidated(true);
//
//		JiraIssueTextPersistenceManager manager = new JiraIssueTextPersistenceManager(issue.getProjectObject().getKey());
//		manager.updateInDatabase(nonValidatedElement1);
//		manager.updateInDatabase(nonValidatedElement2);
//		manager.updateInDatabase(validatedElement);
//		// manager.updateElementsOfDescriptionInDatabase(JiraIssuePersistenceManager.getJiraIssue("TEST-4"););
//
//		List<KnowledgeElement> expectedElements = Arrays.asList(nonValidatedElement1, nonValidatedElement2);
//
//		Response response = classificationRest.getAllNonValidatedElements(request, issue.getProjectObject().getKey());
//
//		ImmutableMap<String, List<KnowledgeElement>> expected = ImmutableMap.of("nonValidatedElements", expectedElements);
//		assertEquals(expected, response.getEntity());
//
//	}

}
