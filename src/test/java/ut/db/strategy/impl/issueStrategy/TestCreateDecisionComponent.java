package ut.db.strategy.impl.issueStrategy;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.atlassian.DecisionDocumentation.rest.Decisions.model.DecisionRepresentation;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.MockApplicationUser;

public class TestCreateDecisionComponent extends TestIssueStrategySetUp {
	
	@Test
	(expected = NullPointerException.class)
	public void testCreateDecisionComponentRepresNullUserNull() {
		issueStrategy.createDecisionComponent(null, null);
	}
	
	@Test
	(expected = NullPointerException.class)
	public void testCreateDecisionComponentRepresFilledUserNull() {
		DecisionRepresentation dec = new DecisionRepresentation();
		issueStrategy.createDecisionComponent(dec, null);
	}
	
	@Test
	public void testCreateDecisionComponentRepresFilledUserFilledNoFails() {
		DecisionRepresentation dec = new DecisionRepresentation();
		dec.setProjectKey("TEST");
		dec.setType("Solution");
		ApplicationUser user = new MockApplicationUser("NoFails");
		assertNotNull(issueStrategy.createDecisionComponent(dec, user));
		
	}
	@Test
	public void testCreateDecisionComponentRepresFilledUserFilledWithFails() {
		DecisionRepresentation dec = new DecisionRepresentation();
		dec.setProjectKey("TEST");
		dec.setType("Solution");
		ApplicationUser user = new MockApplicationUser("WithFails");
		assertNull(issueStrategy.createDecisionComponent(dec, user));
		
	}
}
