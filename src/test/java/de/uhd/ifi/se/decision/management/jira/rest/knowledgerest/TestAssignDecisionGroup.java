package de.uhd.ifi.se.decision.management.jira.rest.knowledgerest;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.mock.servlet.MockHttpServletRequest;
import de.uhd.ifi.se.decision.management.jira.TestSetUp;
import de.uhd.ifi.se.decision.management.jira.model.DocumentationLocation;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeElement;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeGraph;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeType;
import de.uhd.ifi.se.decision.management.jira.model.impl.KnowledgeElementImpl;
import de.uhd.ifi.se.decision.management.jira.persistence.CodeClassKnowledgeElementPersistenceManager;
import de.uhd.ifi.se.decision.management.jira.persistence.DecisionGroupManager;
import de.uhd.ifi.se.decision.management.jira.rest.KnowledgeRest;
import de.uhd.ifi.se.decision.management.jira.rest.impl.KnowledgeRestImpl;
import de.uhd.ifi.se.decision.management.jira.testdata.JiraUsers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestAssignDecisionGroup extends TestSetUp {

	private KnowledgeRest knowledgeRest;
	private KnowledgeElement decisionKnowledgeElementIss;
	// private KnowledgeElement decisionKnowledgeElemenDec;
	// private KnowledgeElement decisionKnowledgeElementAlt;
	// private KnowledgeElement decisionKnowledgeElementPro;
	// private KnowledgeElement decisionKnowledgeElementCon;
	private HttpServletRequest request;

	@Before
	public void setUp() {
		knowledgeRest = new KnowledgeRestImpl();
		init();

		Issue issue = ComponentAccessor.getIssueManager().getIssueByCurrentKey("TEST-3");
		decisionKnowledgeElementIss = new KnowledgeElementImpl(issue);
		decisionKnowledgeElementIss.setType(KnowledgeType.ISSUE);
		/*
		 * decisionKnowledgeElemenDec = new KnowledgeElementImpl(issue);
		 * decisionKnowledgeElemenDec.setType(KnowledgeType.DECISION);
		 * decisionKnowledgeElementAlt = new KnowledgeElementImpl(issue);
		 * decisionKnowledgeElementAlt.setType(KnowledgeType.ALTERNATIVE);
		 * decisionKnowledgeElementPro = new KnowledgeElementImpl(issue);
		 * decisionKnowledgeElementPro.setType(KnowledgeType.PRO);
		 * decisionKnowledgeElementCon = new KnowledgeElementImpl(issue);
		 * decisionKnowledgeElementCon.setType(KnowledgeType.CON);
		 */

		request = new MockHttpServletRequest();
		request.setAttribute("user", JiraUsers.SYS_ADMIN.getApplicationUser());
	}

	@Test
	public void testAssignDecisionGroupAddGroupEmpty() {
		Response resp = knowledgeRest.assignDecisionGroup(request, decisionKnowledgeElementIss.getId(),
				decisionKnowledgeElementIss.getDocumentationLocationAsString(), "High_Level", "Safety", "", "TEST");
		assertEquals(Response.Status.OK.getStatusCode(), resp.getStatus());
		assertTrue(DecisionGroupManager.getGroupsForElement(decisionKnowledgeElementIss).contains("Safety"));
		assertTrue(DecisionGroupManager.getGroupsForElement(decisionKnowledgeElementIss).size() == 2);
	}

	@Test
	public void testAssignDecisionGroupCurrentGroupEmpty() {
		Response resp = knowledgeRest.assignDecisionGroup(request, decisionKnowledgeElementIss.getId(),
				decisionKnowledgeElementIss.getDocumentationLocationAsString(), "High_Level", "", "Safety", "TEST");
		assertEquals(Response.Status.OK.getStatusCode(), resp.getStatus());
		assertTrue(DecisionGroupManager.getGroupsForElement(decisionKnowledgeElementIss).contains("Safety"));
		assertTrue(DecisionGroupManager.getGroupsForElement(decisionKnowledgeElementIss).size() == 2);
	}

	@Test
	public void testAssignDecisionGroupNoEmpties() {
		Response resp = knowledgeRest.assignDecisionGroup(request, decisionKnowledgeElementIss.getId(),
				decisionKnowledgeElementIss.getDocumentationLocationAsString(), "High_Level", "Property,TestGroup",
				"Safety", "TEST");
		assertEquals(Response.Status.OK.getStatusCode(), resp.getStatus());
		assertTrue(DecisionGroupManager.getGroupsForElement(decisionKnowledgeElementIss).contains("Safety"));
		assertTrue(DecisionGroupManager.getGroupsForElement(decisionKnowledgeElementIss).size() == 4);
	}

	@Test
	public void testAssignDecisionGroupElementLinkedToDocLocCommit() {
		KnowledgeElement element = new KnowledgeElementImpl();
		element.setDocumentationLocation(DocumentationLocation.COMMIT);
		element.setSummary("AbstractTestHandler.java");
		element.setDescription("TEST-3;");
		element.setProject("TEST");
		element.setType(KnowledgeType.OTHER);
		CodeClassKnowledgeElementPersistenceManager ccManager = new CodeClassKnowledgeElementPersistenceManager("TEST");
		KnowledgeElement newElement = ccManager.insertDecisionKnowledgeElement(element, JiraUsers.SYS_ADMIN.getApplicationUser());
		Response resp = knowledgeRest.assignDecisionGroup(request, decisionKnowledgeElementIss.getId(),
				decisionKnowledgeElementIss.getDocumentationLocationAsString(), "High_Level", "Property,TestGroup",
				"Safety", "TEST");
		assertEquals(Response.Status.OK.getStatusCode(), resp.getStatus());
		assertTrue(DecisionGroupManager.getGroupsForElement(decisionKnowledgeElementIss).contains("Safety"));
		assertTrue(DecisionGroupManager.getGroupsForElement(decisionKnowledgeElementIss).size() == 4);
		assertTrue(DecisionGroupManager.getGroupsForElement(newElement).contains("Safety"));
		assertTrue(DecisionGroupManager.getGroupsForElement(newElement).contains("Realization_Level"));
	}

	@After
	public void tearDown() {
		KnowledgeGraph.instances.clear();
	}
}
