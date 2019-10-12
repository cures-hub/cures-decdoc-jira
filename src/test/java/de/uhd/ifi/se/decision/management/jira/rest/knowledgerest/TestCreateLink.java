package de.uhd.ifi.se.decision.management.jira.rest.knowledgerest;

import static org.junit.Assert.assertEquals;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.junit.Before;
import org.junit.Test;

import com.atlassian.jira.mock.servlet.MockHttpServletRequest;
import com.google.common.collect.ImmutableMap;

import de.uhd.ifi.se.decision.management.jira.TestSetUp;
import de.uhd.ifi.se.decision.management.jira.rest.KnowledgeRest;
import de.uhd.ifi.se.decision.management.jira.testdata.JiraUsers;
import net.java.ao.test.jdbc.NonTransactional;

public class TestCreateLink extends TestSetUp {
	private KnowledgeRest knowledgeRest;
	private HttpServletRequest request;

	private final static String CREATION_ERROR = "Link could not be created due to a bad request.";

	@Before
	public void setUp() {
		knowledgeRest = new KnowledgeRest();
		init();

		request = new MockHttpServletRequest();
		request.setAttribute("user", JiraUsers.SYS_ADMIN.getApplicationUser());
	}

	@Test
	public void testRequestFilledProjectKeyFilledChildElementFilledParentElementFilledLinkTypeNull() {
		assertEquals(Status.OK.getStatusCode(),
				knowledgeRest.createLink(request, "TEST", "Decision", 4, "i", 1, "i", null).getStatus());
	}

	@Test
	public void testRequestFilledProjectKeyFilledChildKnowledgeTypeNullParentElementFilledLinkTypeNull() {
		assertEquals(Status.OK.getStatusCode(),
				knowledgeRest.createLink(request, "TEST", null, 4, "i", 1, "i", null).getStatus());
	}

	@Test
	public void testRequestFilledProjectKeyFilledChildElementFilledParentElementFilledDocumentationLocationUnknownLinkTypeNull() {
		assertEquals(Status.OK.getStatusCode(),
				knowledgeRest.createLink(request, "TEST", "Decision", 4, "", 1, "", null).getStatus());
	}

	@Test
	@NonTransactional
	public void testRequestFilledProjectKeyFilledChildElementFilledParentElementFilledDocumentationLocationJiraIssueCommentsLinkTypeNull() {
		assertEquals(Status.OK.getStatusCode(),
				knowledgeRest.createLink(request, "TEST", "Decision", 4, "s", 1, "s", null).getStatus());
	}

	@Test
	@NonTransactional
	public void testRequestFilledProjectKeyFilledChildElementFilledParentElementFilledDocumentationLocationDifferLinkTypeNull() {
		assertEquals(Status.OK.getStatusCode(),
				knowledgeRest.createLink(request, "TEST", "Decision", 4, "i", 3, "s", null).getStatus());
	}

	@Test
	public void testRequestNullProjectKeyNullChildElementFilledParentElementFilledLinkTypeNull() {
		assertEquals(Response.status(Status.BAD_REQUEST).entity(ImmutableMap.of("error", CREATION_ERROR)).build()
				.getEntity(), knowledgeRest.createLink(null, null, "Decision", 4, "i", 1, "i", null).getEntity());
	}

	@Test
	public void testRequestFilledProjectKeyNullChildElementFilledParentElementFilledLinkTypeNull() {
		assertEquals(Response.status(Status.BAD_REQUEST).entity(ImmutableMap.of("error", CREATION_ERROR)).build()
				.getEntity(), knowledgeRest.createLink(request, null, "Decision", 4, "i", 1, "i", null).getEntity());
	}

	@Test
	public void testRequestNullProjectKeyFilledChildElementFilledParentElementFilledLinkTypeNull() {
		assertEquals(Response.status(Status.BAD_REQUEST).entity(ImmutableMap.of("error", CREATION_ERROR)).build()
				.getEntity(), knowledgeRest.createLink(null, "TEST", "Decision", 4, "i", 1, "i", null).getEntity());
	}

	@Test
	public void testRequestFilledProjectKeyFilledChildElementIdZeroParentElementFilledLinkTypeNull() {
		assertEquals(
				Response.status(Status.BAD_REQUEST).entity(ImmutableMap.of("error", CREATION_ERROR)).build()
						.getEntity(),
				knowledgeRest.createLink(request, "TEST", "Decision", 0, "i", 1, "i", null).getEntity());
	}

	@Test
	public void testRequestFilledProjectKeyFilledChildElementFilledParentElementIdZeroLinkTypeNull() {
		assertEquals(
				Response.status(Status.BAD_REQUEST).entity(ImmutableMap.of("error", CREATION_ERROR)).build()
						.getEntity(),
				knowledgeRest.createLink(request, "TEST", "Decision", 4, "i", 0, "i", null).getEntity());
	}

	@Test
	public void testRequestFilledProjectKeyFilledChildElementFilledParentElementFilledLinkTypeFilled() {
		assertEquals(Status.OK.getStatusCode(),
				knowledgeRest.createLink(request, "TEST", "Decision", 4, "i", 1, "i", "Decision").getStatus());
	}
}
