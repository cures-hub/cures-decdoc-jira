package de.uhd.ifi.se.decision.management.jira.rest.webhookrest;

import static org.junit.Assert.assertEquals;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;

import com.atlassian.jira.mock.servlet.MockHttpServletRequest;

import de.uhd.ifi.se.decision.management.jira.TestSetUp;
import de.uhd.ifi.se.decision.management.jira.persistence.ConfigPersistenceManager;
import de.uhd.ifi.se.decision.management.jira.rest.WebhookRest;
import de.uhd.ifi.se.decision.management.jira.testdata.JiraUsers;
import de.uhd.ifi.se.decision.management.jira.webhook.WebhookConfiguration;

public class TestSendTestPost extends TestSetUp {

	private HttpServletRequest request;
	private WebhookRest webhookRest;

	@Before
	public void setUp() {
		init();
		webhookRest = new WebhookRest();
		request = new MockHttpServletRequest();
		request.setAttribute("user", JiraUsers.SYS_ADMIN.getApplicationUser());
	}

	@Test
	public void testRequestNullProjectKeyNull() {
		assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), webhookRest.sendTestPost(null, null).getStatus());
	}

	@Test
	public void testRequestValidProjectKeyNull() {
		assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), webhookRest.sendTestPost(request, null).getStatus());
	}

	@Test
	public void testRequestNullProjectKeyValid() {
		assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), webhookRest.sendTestPost(null, "TEST").getStatus());
	}

	@Test
	public void testRequestValidProjectKeyValidButNonExistingReceiver() {
		WebhookConfiguration config = ConfigPersistenceManager.getWebhookConfiguration("TEST");
		config.setWebhookUrl("https://hooks.slack.com/nowhere");
		config.setWebhookSecret("42");
		ConfigPersistenceManager.saveWebhookConfiguration("TEST", config);
		assertEquals(Response.Status.BAD_REQUEST.getStatusCode(),
				webhookRest.sendTestPost(request, "TEST").getStatus());
	}
}
