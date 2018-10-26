package de.uhd.ifi.se.decision.management.jira.rest.knowledgerest;

import static org.junit.Assert.assertEquals;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.common.collect.ImmutableMap;

import de.uhd.ifi.se.decision.management.jira.TestSetUpWithIssues;
import de.uhd.ifi.se.decision.management.jira.extraction.model.Comment;
import de.uhd.ifi.se.decision.management.jira.extraction.model.TestComment;
import de.uhd.ifi.se.decision.management.jira.extraction.model.impl.GenericLinkImpl;
import de.uhd.ifi.se.decision.management.jira.model.Link;
import de.uhd.ifi.se.decision.management.jira.persistence.GenericLinkManager;
import net.java.ao.test.jdbc.Data;
import net.java.ao.test.jdbc.NonTransactional;
import net.java.ao.test.junit.ActiveObjectsJUnitRunner;

@RunWith(ActiveObjectsJUnitRunner.class)
@Data(TestSetUpWithIssues.AoSentenceTestDatabaseUpdater.class)
public class TestDeleteGenericLink extends TestKnowledgeRestSetUp {

	private final static String CREATION_ERROR = "Deletion of link failed.";

	private Link newGenericLink() {
		return new GenericLinkImpl("i1337", "s1337", "contain");
	}

	private GenericLinkImpl newGenericInverseLink() {
		return new GenericLinkImpl("s1337", "i1337", "contain");
	}

	@Test
	@NonTransactional
	public void testRequestNullElementNull() {
		assertEquals(Response.status(Status.BAD_REQUEST).entity(ImmutableMap.of("error", CREATION_ERROR)).build()
				.getEntity(), knowledgeRest.deleteGenericLink(null, null, null).getEntity());
	}

	@Test
	@NonTransactional
	public void testRequestNullElementFilled() {
		TestComment tc = new TestComment();
		Comment comment = tc.getComment("this is atest sentence");
		decisionKnowledgeElement = comment.getSentences().get(0);
		assertEquals(Response.status(Status.BAD_REQUEST).entity(ImmutableMap.of("error", CREATION_ERROR)).build()
				.getEntity(), knowledgeRest.deleteGenericLink("TEST", null, newGenericLink()).getEntity());
	}

	@Test
	@NonTransactional
	public void testRequestFilledElementNull() {
		request.setAttribute("WithFails", false);
		request.setAttribute("NoFails", true);
		assertEquals(Response.status(Status.BAD_REQUEST).entity(ImmutableMap.of("error", CREATION_ERROR)).build()
				.getEntity(), knowledgeRest.deleteGenericLink("TEST", request, null).getEntity());
	}

	@Test
	@NonTransactional
	public void testRequestFilledElementFilled() {
		request.setAttribute("WithFails", false);
		request.setAttribute("NoFails", true);
		GenericLinkManager.insertGenericLink(newGenericLink(), null);
		assertEquals(Status.OK.getStatusCode(),
				knowledgeRest.deleteGenericLink("TEST", request, newGenericLink()).getStatus());

		GenericLinkManager.insertGenericLink(newGenericLink(), null);
		assertEquals(Status.OK.getStatusCode(),
				knowledgeRest.deleteGenericLink("TEST", request, newGenericInverseLink()).getStatus());

		assertEquals(
				Response.status(Status.BAD_REQUEST).entity(ImmutableMap.of("error", CREATION_ERROR)).build()
						.getEntity(),
				knowledgeRest.deleteGenericLink("TEST", request, newGenericInverseLink()).getEntity());

	}

}
