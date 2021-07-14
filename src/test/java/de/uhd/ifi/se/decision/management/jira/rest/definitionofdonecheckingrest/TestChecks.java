package de.uhd.ifi.se.decision.management.jira.rest.definitionofdonecheckingrest;

import static org.junit.Assert.assertEquals;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import de.uhd.ifi.se.decision.management.jira.git.model.ChangedFile;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeType;
import de.uhd.ifi.se.decision.management.jira.testdata.CodeFiles;
import org.junit.Before;
import org.junit.Test;

import com.atlassian.jira.mock.servlet.MockHttpServletRequest;

import de.uhd.ifi.se.decision.management.jira.TestSetUp;
import de.uhd.ifi.se.decision.management.jira.filtering.FilterSettings;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeElement;
import de.uhd.ifi.se.decision.management.jira.rest.DefinitionOfDoneCheckingRest;
import de.uhd.ifi.se.decision.management.jira.testdata.JiraUsers;
import de.uhd.ifi.se.decision.management.jira.testdata.KnowledgeElements;
import net.java.ao.test.jdbc.NonTransactional;

public class TestChecks extends TestSetUp {

	protected HttpServletRequest request;
	protected DefinitionOfDoneCheckingRest dodCheckingRest;
	protected FilterSettings settings;

	@Before
	public void setUp() {
		init();
		dodCheckingRest = new DefinitionOfDoneCheckingRest();
		request = new MockHttpServletRequest();
		request.setAttribute("user", JiraUsers.SYS_ADMIN.getApplicationUser());
		settings = new FilterSettings("TEST", "");

	}

	@Test
	@NonTransactional
	public void testGetFailedDefinitionOfDoneCriteria() {
		KnowledgeElement knowledgeElement = KnowledgeElements.getTestKnowledgeElement();
		settings = new FilterSettings("TEST", "");
		settings.setSelectedElement(knowledgeElement);
		Response response = dodCheckingRest.getQualityProblems(request, settings);
		assertEquals(200, response.getStatus());

		settings.setSelectedElement((KnowledgeElement) null);
		response = dodCheckingRest.getQualityProblems(request, settings);
		assertEquals(400, response.getStatus());
		settings.setSelectedElement(knowledgeElement);

		response = dodCheckingRest.getQualityProblems(request, null);
		assertEquals(400, response.getStatus());

		settings.setProjectKey(null);
		response = dodCheckingRest.getQualityProblems(request, settings);
		assertEquals(400, response.getStatus());
	}

	@Test
	@NonTransactional
	public void testGetFailedDefinitionOfDoneCheckCriteriaIssue() {
		KnowledgeElement knowledgeElement = KnowledgeElements.getTestKnowledgeElement();
		knowledgeElement.setType(KnowledgeType.ISSUE);
		settings = new FilterSettings("TEST", "");
		settings.setSelectedElement(knowledgeElement);
		Response response = dodCheckingRest.getQualityProblems(request, settings);
		assertEquals(200, response.getStatus());
	}

	@Test
	@NonTransactional
	public void testGetFailedDefinitionOfDoneCheckCriteriaDecision() {
		KnowledgeElement knowledgeElement = KnowledgeElements.getTestKnowledgeElement();
		knowledgeElement.setType(KnowledgeType.DECISION);
		settings = new FilterSettings("TEST", "");
		settings.setSelectedElement(knowledgeElement);
		Response response = dodCheckingRest.getQualityProblems(request, settings);
		assertEquals(200, response.getStatus());
	}

	@Test
	@NonTransactional
	public void testGetFailedDefinitionOfDoneCheckCriteriaAlternative() {
		KnowledgeElement knowledgeElement = KnowledgeElements.getTestKnowledgeElement();
		knowledgeElement.setType(KnowledgeType.ALTERNATIVE);
		settings = new FilterSettings("TEST", "");
		settings.setSelectedElement(knowledgeElement);
		Response response = dodCheckingRest.getQualityProblems(request, settings);
		assertEquals(200, response.getStatus());
	}

	@Test
	@NonTransactional
	public void testGetFailedDefinitionOfDoneCheckCriteriaArgument() {
		KnowledgeElement knowledgeElement = KnowledgeElements.getTestKnowledgeElement();
		knowledgeElement.setType(KnowledgeType.ARGUMENT);
		settings = new FilterSettings("TEST", "");
		settings.setSelectedElement(knowledgeElement);
		Response response = dodCheckingRest.getQualityProblems(request, settings);
		assertEquals(200, response.getStatus());
	}

	@Test
	@NonTransactional
	public void testGetFailedDefinitionOfDoneCheckCriteriaPro() {
		KnowledgeElement knowledgeElement = KnowledgeElements.getTestKnowledgeElement();
		knowledgeElement.setType(KnowledgeType.PRO);
		settings = new FilterSettings("TEST", "");
		settings.setSelectedElement(knowledgeElement);
		Response response = dodCheckingRest.getQualityProblems(request, settings);
		assertEquals(200, response.getStatus());
	}

	@Test
	@NonTransactional
	public void testGetFailedDefinitionOfDoneCheckCriteriaCon() {
		KnowledgeElement knowledgeElement = KnowledgeElements.getTestKnowledgeElement();
		knowledgeElement.setType(KnowledgeType.CON);
		settings = new FilterSettings("TEST", "");
		settings.setSelectedElement(knowledgeElement);
		Response response = dodCheckingRest.getQualityProblems(request, settings);
		assertEquals(200, response.getStatus());
	}

	@Test
	@NonTransactional
	public void testGetFailedDefinitionOfDoneCheckCriteriaCode() {
		ChangedFile knowledgeElement = CodeFiles.getTestCodeFileDone();
		knowledgeElement.setType(KnowledgeType.CODE);
		settings = new FilterSettings("TEST", "");
		settings.setSelectedElement(knowledgeElement);
		Response response = dodCheckingRest.getQualityProblems(request, settings);
		assertEquals(200, response.getStatus());
	}

	@Test
	@NonTransactional
	public void testGetCoverageOfJiraIssue() {
		KnowledgeElement knowledgeElement = KnowledgeElements.getTestKnowledgeElement();
		settings = new FilterSettings("TEST", "");
		settings.setSelectedElement(knowledgeElement);
		Response response = dodCheckingRest.getCoverageOfJiraIssue(request, settings);
		assertEquals(200, response.getStatus());
	}

	@Test
	@NonTransactional
	public void testGetCoverageOfJiraIssueFilterSettingsInvalid() {
		Response response = dodCheckingRest.getCoverageOfJiraIssue(request, null);
		assertEquals(400, response.getStatus());
	}

	@Test
	@NonTransactional
	public void testGetCoverageOfJiraIssueProjectKeyInvalid() {
		KnowledgeElement knowledgeElement = KnowledgeElements.getTestKnowledgeElement();
		settings = new FilterSettings(null, "");
		settings.setSelectedElement(knowledgeElement);
		Response response = dodCheckingRest.getCoverageOfJiraIssue(request, settings);
		assertEquals(400, response.getStatus());
	}

	@Test
	@NonTransactional
	public void testGetCoverageOfJiraIssueSelectedElementInvalid() {
		settings = new FilterSettings("TEST", "");
		settings.setSelectedElement((KnowledgeElement) null);
		Response response = dodCheckingRest.getCoverageOfJiraIssue(request, settings);
		assertEquals(400, response.getStatus());
	}
}
