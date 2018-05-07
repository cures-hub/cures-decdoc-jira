package de.uhd.ifi.se.decision.documentation.jira.rest.DecisionRestTest;

import static org.junit.Assert.assertEquals;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.atlassian.activeobjects.test.TestActiveObjects;
import com.atlassian.jira.mock.servlet.MockHttpServletRequest;
import com.google.common.collect.ImmutableMap;

import de.uhd.ifi.se.decision.documentation.jira.ComponentGetter;
import de.uhd.ifi.se.decision.documentation.jira.TestSetUp;
import de.uhd.ifi.se.decision.documentation.jira.mocks.MockDefaultUserManager;
import de.uhd.ifi.se.decision.documentation.jira.mocks.MockTransactionTemplate;
import de.uhd.ifi.se.decision.documentation.jira.model.LinkImpl;
import de.uhd.ifi.se.decision.documentation.jira.rest.DecisionsRest;
import net.java.ao.EntityManager;
import net.java.ao.test.junit.ActiveObjectsJUnitRunner;

@RunWith(ActiveObjectsJUnitRunner.class)
public class TestPutLink extends TestSetUp {
	private EntityManager entityManager;
	private DecisionsRest decRest;
	private HttpServletRequest req;
	private LinkImpl link;

	@Before
	public void setUp() {
		decRest= new DecisionsRest();
		initialization();
		new ComponentGetter().init(new TestActiveObjects(entityManager), new MockTransactionTemplate(), new MockDefaultUserManager());

		req = new MockHttpServletRequest();
		req.setAttribute("WithFails", false);
		req.setAttribute("NoFails", true);

		link = new LinkImpl();
		link.setIngoingId(1);
		link.setOutgoingId(4);
	}

	@Test
	public void testactionTypeNullKeyNullReqNullLinkNull() {
		assertEquals(Response.status(Status.BAD_REQUEST).entity(ImmutableMap.of("error", "Creation of link failed.")).build().getEntity(),decRest.createLink(null, null, null).getEntity());
	}

	@Test
	public void testactionTypeNullKeyNullReqNullLinkIdZero() {
		link.setLinkType("Zero");
		assertEquals(Response.status(Status.BAD_REQUEST).entity(ImmutableMap.of("error", "Creation of link failed.")).build().getEntity(),decRest.createLink(null, null, link).getEntity());
	}
	public void testactionTypeNullKeyNullReqNullLinkIdFilled() {
		link.setLinkType("Ok");
		assertEquals(Response.status(Status.BAD_REQUEST).entity(ImmutableMap.of("error", "Creation of link failed.")).build().getEntity(),decRest.createLink(null, null, link).getEntity());
	}

	@Test
	public void testactionTypeNullKeyNullReqFilledLinkNull() {
		assertEquals(Response.status(Status.BAD_REQUEST).entity(ImmutableMap.of("error", "Creation of link failed.")).build().getEntity(),decRest.createLink(null, req, null).getEntity());
	}

	@Test
	public void testactionTypeNullKeyNullReqFilledLinkIdZero() {
		link.setLinkType("Zero");
		assertEquals(Response.status(Status.BAD_REQUEST).entity(ImmutableMap.of("error", "Creation of link failed.")).build().getEntity(),decRest.createLink(null, req, link).getEntity());
	}

	@Test
	public void testactionTypeNullKeyNullReqFilledLinkIdFilled() {
		link.setLinkType("Ok");
		assertEquals(Response.status(Status.BAD_REQUEST).entity(ImmutableMap.of("error", "Creation of link failed.")).build().getEntity(),decRest.createLink(null, req, link).getEntity());
	}

	@Test
	public void testactionTypeNullKeyFilledReqNullLinkNull() {
		assertEquals(Response.status(Status.BAD_REQUEST).entity(ImmutableMap.of("error", "Creation of link failed.")).build().getEntity(),decRest.createLink(null, null, null).getEntity());
	}

	@Test
	public void testactionTypeNullKeyFilledReqNullLinkIdZero() {
		link.setLinkType("Zero");
		assertEquals(Response.status(Status.BAD_REQUEST).entity(ImmutableMap.of("error", "Creation of link failed.")).build().getEntity(),decRest.createLink(null, null, link).getEntity());
	}

	@Test
	public void testactionTypeNullKeyFilledReqNullLinkIdFilled() {
		link.setLinkType("Ok");
		assertEquals(Response.status(Status.BAD_REQUEST).entity(ImmutableMap.of("error", "Creation of link failed.")).build().getEntity(),decRest.createLink(null, null, link).getEntity());
	}

	@Test
	public void testactionTypeNullKeyFilledReqFilledLinkNull() {
		assertEquals(Response.status(Status.BAD_REQUEST).entity(ImmutableMap.of("error", "Creation of link failed.")).build().getEntity(),decRest.createLink(null, req, null).getEntity());
	}

	@Test
	public void testactionTypeNullKeyFilledReqFilledLinkIdZero() {
		link.setLinkType("Zero");
		assertEquals(Response.status(Status.BAD_REQUEST).entity(ImmutableMap.of("error", "Creation of link failed.")).build().getEntity(),decRest.createLink(null, req, link).getEntity());
	}

	@Test
	public void testactionTypeNullKeyFilledReqFilledLinkIdFilled() {
		link.setLinkType("Ok");
		assertEquals(Response.status(Status.BAD_REQUEST).entity(ImmutableMap.of("error", "Creation of link failed.")).build().getEntity(),decRest.createLink(null, req, link).getEntity());
	}

	@Test
	public void testactionTypeCreateKeyNullReqNullLinkNull() {
		assertEquals(Response.status(Status.BAD_REQUEST).entity(ImmutableMap.of("error", "Creation of link failed.")).build().getEntity(),decRest.createLink("create",null, null).getEntity());
	}

	@Test
	public void testactionTypeCreateKeyNullReqNullLinkIdZero() {
		link.setLinkType("Zero");
		assertEquals(Response.status(Status.BAD_REQUEST).entity(ImmutableMap.of("error", "Creation of link failed.")).build().getEntity(),decRest.createLink("create", null, link).getEntity());
	}

	@Test
	public void testactionTypeCreateKeyNullReqNullLinkIdFilled() {
		link.setLinkType("Ok");
		assertEquals(Response.status(Status.BAD_REQUEST).entity(ImmutableMap.of("error", "Creation of link failed.")).build().getEntity(),decRest.createLink("create", null, link).getEntity());
	}

	@Test
	public void testactionTypeCreateKeyNullReqFilledLinkNull() {
		assertEquals(Response.status(Status.BAD_REQUEST).entity(ImmutableMap.of("error", "Creation of link failed.")).build().getEntity(),decRest.createLink("create", req, null).getEntity());
	}

	@Test
	public void testactionTypeCreateKeyFilledReqNullLinkNull() {
		assertEquals(Response.status(Status.BAD_REQUEST).entity(ImmutableMap.of("error", "Creation of link failed.")).build().getEntity(),decRest.createLink("create",null, null).getEntity());
	}

	@Test
	public void testactionTypeCreateKeyFilledReqNullLinkIdZero() {
		link.setLinkType("Zero");
		assertEquals(Response.status(Status.BAD_REQUEST).entity(ImmutableMap.of("error", "Creation of link failed.")).build().getEntity(),decRest.createLink("create", null, link).getEntity());
	}

	@Test
	public void testactionTypeCreateKeyFilledReqNullLinkIdFilled() {
		link.setLinkType("Ok");
		assertEquals(Response.status(Status.BAD_REQUEST).entity(ImmutableMap.of("error", "Creation of link failed.")).build().getEntity(),decRest.createLink("create", null, link).getEntity());
	}

	@Test
	public void testactionTypeCreateKeyFilledReqFilledLinkNull() {
		assertEquals(Response.status(Status.BAD_REQUEST).entity(ImmutableMap.of("error", "Creation of link failed.")).build().getEntity(),decRest.createLink("create", req, null).getEntity());
	}

	@Test
	public void testactionTypeCreateKeyFilledReqFilledLinkIdZero() {
		link.setLinkType("Zero");
		link.setIngoingId(3);
		assertEquals(Response.status(Status.INTERNAL_SERVER_ERROR).entity(ImmutableMap.of("error", "Creation of link failed.")).build().getEntity(),decRest.createLink("create", req, link).getEntity());
	}

	@Test
	public void testactionTypeCreateKeyFilledReqFilledLinkIdFilled() {
		link.setLinkType("Ok");
		assertEquals(Status.OK.getStatusCode(),decRest.createLink("create",  req, link).getStatus());
	}

	@Test
	public void testactionTypeNotCreateKeyNullReqNullLinkNull() {
		assertEquals(Response.status(Status.BAD_REQUEST).entity(ImmutableMap.of("error", "Creation of link failed.")).build().getEntity(),decRest.createLink("notCreate",  null, null).getEntity());
	}

	@Test
	public void testactionTypeNotCreateKeyNullReqNullLinkIdZero() {
		link.setLinkType("Zero");
		assertEquals(Response.status(Status.BAD_REQUEST).entity(ImmutableMap.of("error", "Creation of link failed.")).build().getEntity(),decRest.createLink("notCreate", null, link).getEntity());
	}

	@Test
	public void testactionTypeNotCreateKeyNullReqNullLinkIdFilled() {
		link.setLinkType("Ok");
		assertEquals(Response.status(Status.BAD_REQUEST).entity(ImmutableMap.of("error", "Creation of link failed.")).build().getEntity(),decRest.createLink("notCreate", null, link).getEntity());
	}

	@Test
	public void testactionTypeNotCreateKeyNullReqFilledLinkNull() {
		assertEquals(Response.status(Status.BAD_REQUEST).entity(ImmutableMap.of("error", "Creation of link failed.")).build().getEntity(),decRest.createLink("notCreate", req, null).getEntity());
	}

	@Test
	public void testactionTypeNotCreateKeyFilledReqNullLinkNull() {
		assertEquals(Response.status(Status.BAD_REQUEST).entity(ImmutableMap.of("error", "Creation of link failed.")).build().getEntity(),decRest.createLink("notCreate",  null, null).getEntity());
	}

	@Test
	public void testactionTypeNotCreateKeyFilledReqNullLinkIdZero() {
		link.setLinkType("Zero");
		assertEquals(Response.status(Status.BAD_REQUEST).entity(ImmutableMap.of("error", "Creation of link failed.")).build().getEntity(),decRest.createLink("notCreate",  null, link).getEntity());
	}

	@Test
	public void testactionTypeNotCreateKeyFilledReqNullLinkIdFilled() {
		link.setLinkType("Ok");
		assertEquals(Response.status(Status.BAD_REQUEST).entity(ImmutableMap.of("error", "Creation of link failed.")).build().getEntity(),decRest.createLink("notCreate",  null, link).getEntity());
	}

	@Test
	public void testactionTypeNotCreateKeyFilledReqFilledLinkNull() {
		assertEquals(Response.status(Status.BAD_REQUEST).entity(ImmutableMap.of("error", "Creation of link failed.")).build().getEntity(),decRest.createLink("notCreate",  req, null).getEntity());
	}
}
