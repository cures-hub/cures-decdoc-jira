package de.uhd.ifi.se.decision.documentation.jira.view.treants;

import static org.junit.Assert.assertEquals;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.mock.issue.MockIssue;

import de.uhd.ifi.se.decision.documentation.jira.mocks.MockIssueLink;
import de.uhd.ifi.se.decision.documentation.jira.persistence.PersistenceStrategy;
import de.uhd.ifi.se.decision.documentation.jira.persistence.StrategyProvider;
import de.uhd.ifi.se.decision.documentation.jira.rest.TreantRest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ofbiz.core.entity.GenericEntityException;

import com.atlassian.activeobjects.test.TestActiveObjects;
import com.google.common.collect.ImmutableMap;

import de.uhd.ifi.se.decision.documentation.jira.TestSetUp;
import de.uhd.ifi.se.decision.documentation.jira.mocks.MockDefaultUserManager;
import de.uhd.ifi.se.decision.documentation.jira.mocks.MockTransactionTemplate;
import de.uhd.ifi.se.decision.documentation.jira.model.DecisionKnowledgeElement;
import de.uhd.ifi.se.decision.documentation.jira.model.KnowledgeType;
import de.uhd.ifi.se.decision.documentation.jira.model.Link;
import de.uhd.ifi.se.decision.documentation.jira.util.ComponentGetter;
import net.java.ao.EntityManager;
import net.java.ao.test.junit.ActiveObjectsJUnitRunner;

import java.util.Collection;


@RunWith(ActiveObjectsJUnitRunner.class)
public class TestTreantRest extends TestSetUp {
	private EntityManager entityManager;  
	
	private TreantRest treantRest;
	
	@Before	
	public void setUp() {
		treantRest=new TreantRest();
		initialization();		
		new ComponentGetter().init(new TestActiveObjects(entityManager), new MockTransactionTemplate(), new MockDefaultUserManager());
	}
	
	@Test
	public void testProjectNullIssueKeyNullDepthNull() throws GenericEntityException {
		assertEquals(Response.status(Status.INTERNAL_SERVER_ERROR).entity(ImmutableMap.of("error", "Query parameter 'projectKey' is not provided, please add a valid projectKey")).build().getEntity(),treantRest.getMessage(null, null, null).getEntity());
	}
	
	@Test
	public void testProjectNullIssueKeyFilledDepthNull() throws GenericEntityException {
		assertEquals(Response.status(Status.INTERNAL_SERVER_ERROR).entity(ImmutableMap.of("error", "Query parameter 'projectKey' is not provided, please add a valid projectKey")).build().getEntity(),treantRest.getMessage(null, "3", null).getEntity());
	}
	
	@Test
	public void testProjectNullIssueKeyNullDepthFilled() throws GenericEntityException {
		assertEquals(Response.status(Status.INTERNAL_SERVER_ERROR).entity(ImmutableMap.of("error", "Query parameter 'projectKey' is not provided, please add a valid projectKey")).build().getEntity(),treantRest.getMessage(null, null, "3").getEntity());
	}
	
	@Test
	public void testProjectNullIssueKeyFilledDepthFilled() throws GenericEntityException {
		assertEquals(Response.status(Status.INTERNAL_SERVER_ERROR).entity(ImmutableMap.of("error", "Query parameter 'projectKey' is not provided, please add a valid projectKey")).build().getEntity(),treantRest.getMessage(null, "3", "1").getEntity());
	}
	
	@Test
	public void testProjectExistsIssueKeyNullDepthNull() throws GenericEntityException {
		assertEquals(Response.status(Status.INTERNAL_SERVER_ERROR).entity(ImmutableMap.of("error", "Query parameters 'projectKey' and 'issueKey' do not lead to a valid result")).build().getEntity(),treantRest.getMessage("TEST", null, null).getEntity());
	}
	
	@Test
	public void testProjectExistsIssueKeyFilledDepthNull() throws GenericEntityException {
		assertEquals(200,treantRest.getMessage("TEST", "3", null).getStatus());
	}
		
	@Test
	public void testProjectExistsIssueKeyNullDepthFilled() throws GenericEntityException {
		assertEquals(Response.status(Status.INTERNAL_SERVER_ERROR).entity(ImmutableMap.of("error", "Query parameters 'projectKey' and 'issueKey' do not lead to a valid result")).build().getEntity(),treantRest.getMessage("TEST", null, "1").getEntity());
	}

	@Test
	public void testProjectExistsIssueKeyFilledDepthFilled() throws GenericEntityException {
		assertEquals(200,treantRest.getMessage("TEST", "3", "1").getStatus());
	}

	@Test
	public void testProjectNotExistsIssueKeyNullDepthNull() throws GenericEntityException {
		assertEquals(Response.status(Status.INTERNAL_SERVER_ERROR).entity(ImmutableMap.of("error", "Cannot find project for the given query parameter 'projectKey'")).build().getEntity(),treantRest.getMessage("NotTEST", null, null).getEntity());
	}
	
	@Test
	public void testProjectNotExistsIssueKeyFilledDepthNull() throws GenericEntityException {
		assertEquals(Response.status(Status.INTERNAL_SERVER_ERROR).entity(ImmutableMap.of("error", "Cannot find project for the given query parameter 'projectKey'")).build().getEntity(),treantRest.getMessage("NotTEST", "3", null).getEntity());
	}
	
	@Test
	public void testProjectNotExistsIssueKeyNullDepthFilled() throws GenericEntityException {
		assertEquals(Response.status(Status.INTERNAL_SERVER_ERROR).entity(ImmutableMap.of("error", "Cannot find project for the given query parameter 'projectKey'")).build().getEntity(),treantRest.getMessage("NotTEST", null, "1").getEntity());
	}
	
	@Test
	public void testProjectNotExistsIssueKeyFilledDepthFilled() throws GenericEntityException {
		assertEquals(Response.status(Status.INTERNAL_SERVER_ERROR).entity(ImmutableMap.of("error", "Cannot find project for the given query parameter 'projectKey'")).build().getEntity(),treantRest.getMessage("NotTEST", "3", "1").getEntity());
	}
	
	@Test
	public void testProjectExistsIssueKeyFilledDepthNoInt() throws GenericEntityException {
		assertEquals(200,treantRest.getMessage("TEST", "3", "Test").getStatus());
	}

	@Test
	public void testProjectExistsIssueKeyFilledAllTypes() throws GenericEntityException {
		for(long i=2; i<= 16;i++){
			treantRest.getMessage("TEST", Long.toString(i), "3").getStatus();
		}
	}

	@Test
	public  void testProjectExistsIssueKeyFilledChildElements() throws GenericEntityException {
		StrategyProvider strategyProvider = new StrategyProvider();
		PersistenceStrategy strategy = strategyProvider.getStrategy("TEST");
		Issue issue1 = ComponentAccessor.getIssueManager().getIssueObject((long) 12);
		Issue issue2 = ComponentAccessor.getIssueManager().getIssueObject((long) 13);
		strategy.insertDecisionKnowledgeElement(new DecisionKnowledgeElement(issue1), ComponentAccessor.getUserManager().getUserByName("NoFails"));
		strategy.insertDecisionKnowledgeElement(new DecisionKnowledgeElement(issue2), ComponentAccessor.getUserManager().getUserByName("NoFails"));

		MockIssueLink issuelink = new MockIssueLink((long)100);
		Link link = new Link(issuelink);
		strategy.insertLink(link,ComponentAccessor.getUserManager().getUserByName("NoFails"));

		treantRest.getMessage("TEST","12", "3").getStatus();
	}
}
