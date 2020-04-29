package de.uhd.ifi.se.decision.management.jira.webhook;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

import de.uhd.ifi.se.decision.management.jira.TestSetUp;
import net.java.ao.test.jdbc.NonTransactional;

import de.uhd.ifi.se.decision.management.jira.model.KnowledgeElement;
import de.uhd.ifi.se.decision.management.jira.model.impl.KnowledgeElementImpl;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeType;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestWebhookContentProvider extends TestSetUp {

	@Before
	public void setUp() {
		init();
	}

	@Test
	public void testCreatePostMethodForMissingProjectKeyAndMissingElementKeyAndMissingSecretAndMissingReceiver() {
		WebhookContentProvider provider = new WebhookContentProvider(null, (String) null, null, null);
		assertNull(provider.createPostMethod().getRequestEntity());
	}

	@Test
	public void testCreatePostMethodForMissingProjectKeyAndMissingElementKeyAndProvidedSecretAndMissingReceiver() {
		WebhookContentProvider provider = new WebhookContentProvider(null, (String) null, "1234IamASecretKey", null);
		assertNull(provider.createPostMethod().getRequestEntity());
	}

	@Test
	public void testCreatePostMethodForMissingProjectKeyAndProvidedElementKeyAndMissingSecretAndMissingReceiver() {
		WebhookContentProvider provider = new WebhookContentProvider(null, "TEST-14", null, null);
		assertNull(provider.createPostMethod().getRequestEntity());
	}

	@Test
	public void testCreatePostMethodForProvidedProjectKeyAndMissingElementKeyAndMissingSecretAndMissingReceiver() {
		WebhookContentProvider provider = new WebhookContentProvider("TEST", (String) null, null, null);
		assertNull(provider.createPostMethod().getRequestEntity());
	}

	@Test
	@NonTransactional
	public void testCreatePostMethodForProvidedProjectKeyAndProvidedElementKeyAndProvidedSecretAndOtherReceiver() {
		WebhookContentProvider provider = new WebhookContentProvider("TEST", "TEST-14", "1234IamASecretKey", "Other");
		assertNotNull(provider.createPostMethod().getRequestEntity());
	}

	@Test
	@NonTransactional
	public void testCreatePostMethodForSlackForMissingProjectKeyAndMissingElementKeyAndMissingSecretAndSlackReceiver() {
		WebhookContentProvider provider = new WebhookContentProvider(null, (String) null, null, "Slack");
		assertNull(provider.createPostMethodForSlack().getRequestEntity());
	}

	@Test
	@NonTransactional
	public void testCreatePostMethodForSlackForProvidedProjectKeyAndMissingElementKeyAndMissingSecretAndSlackReceiver() {
		WebhookContentProvider provider = new WebhookContentProvider("TEST", (String) null, null, "Slack");
		assertNull(provider.createPostMethodForSlack().getRequestEntity());
	}

	@Test
	@NonTransactional
	public void testCreatePostMethodForSlackForProvidedProjectKeyAndProvidedElementKeyAndMissingSecretAndSlackReceiver() {
		WebhookContentProvider provider = new WebhookContentProvider("TEST", "TEST-14", null, "Slack");
		assertNull(provider.createPostMethodForSlack().getRequestEntity());
	}

	@Test
	@NonTransactional
	public void testCreatePostMethodForSlackForProvidedProjectKeyAndProvidedElementAndMissingSecretAndSlackReceiver() {
		KnowledgeElement knowledgeElement = new KnowledgeElementImpl((long) 1, "TEST", "i");
		knowledgeElement.setSummary("Summary");
		knowledgeElement.setDescription("Description");
		knowledgeElement.setType(KnowledgeType.ISSUE);

		WebhookContentProvider provider = new WebhookContentProvider("TEST", knowledgeElement, null, "Slack");
		assertNotNull(provider.createPostMethodForSlack().getRequestEntity());
		}

	@Test
	@NonTransactional
	public void testCreatePostMethodForSlackForProvidedProjectKeyAndProvidedKnowledgeElementAndMissingSecretAndSlackReceiverCutSummary() {
		KnowledgeElement knowledgeElement1 = new KnowledgeElementImpl((long) 1, "TEST", "i");
		knowledgeElement1.setSummary("{issue}Summary");
		knowledgeElement1.setDescription("Description");
		knowledgeElement1.setType(KnowledgeType.ISSUE);

		WebhookContentProvider provider = new WebhookContentProvider("TEST", knowledgeElement1, null, "Slack");
		assertNotNull(provider.createPostMethodForSlack().getRequestEntity());
	}

	@Test
	@NonTransactional
	public void testCreatePostMethodForProvidedProjectKeyAndProvidedKnowledgeElementAndMissingSecretAndSlackReceiver() {
		KnowledgeElement knowledgeElement1 = new KnowledgeElementImpl((long) 1, "TEST", "i");
		knowledgeElement1.setSummary("Summary");
		knowledgeElement1.setDescription("Description");
		knowledgeElement1.setType(KnowledgeType.ISSUE);

		WebhookContentProvider provider = new WebhookContentProvider("TEST", knowledgeElement1, null, "Slack");
		assertNull(provider.createPostMethod().getRequestEntity());
	}

	@Test
	@NonTransactional
	public void testcreateWebhookDataForSlackNewElement(){
		KnowledgeElement knowledgeElement = new KnowledgeElementImpl((long) 1, "TEST", "i");
		knowledgeElement.setSummary("Summary");
		knowledgeElement.setDescription("Description");
		knowledgeElement.setType(KnowledgeType.ISSUE);

		WebhookContentProvider provider = new WebhookContentProvider("TEST", knowledgeElement, null, "Slack");

		String data = "{'blocks':[{'type':'section','text':{'type':'mrkdwn','text':'"+ "TEST : Neues Entscheidungswissen wurde in Jira dokumentiert:" +"'}},"+
		"{'type':'section','text':{'type':'mrkdwn','text':'*Typ:* :Issue:  Issue" +
		" \\n *Titel*: Summary\\n'},"+
		"'accessory':{'type':'button','text':{'type':'plain_text','text':'Go to Jira'},'url' : '"+knowledgeElement.getUrl()+"'}}]}";

		assertEquals(data, provider.createWebhookDataForSlack(knowledgeElement, "new"));
	}

	@Test
	@NonTransactional
	public void testcreateWebhookDataForSlackChangedElement(){
		KnowledgeElement knowledgeElement = new KnowledgeElementImpl((long) 1, "TEST", "i");
		knowledgeElement.setSummary("Summary");
		knowledgeElement.setDescription("Description");
		knowledgeElement.setType(KnowledgeType.ISSUE);


		WebhookContentProvider provider = new WebhookContentProvider("TEST", knowledgeElement, null, "Slack");

		String data = "{'blocks':[{'type':'section','text':{'type':'mrkdwn','text':'"+ "TEST : Dieses dokumentierte Entscheidungswissen wurde geändert:" +"'}},"+
		"{'type':'section','text':{'type':'mrkdwn','text':'*Typ:* :Issue:  Issue" +
		" \\n *Titel*: Summary\\n'},"+
		"'accessory':{'type':'button','text':{'type':'plain_text','text':'Go to Jira'},'url' : '"+knowledgeElement.getUrl()+"'}}]}";

		assertEquals(data, provider.createWebhookDataForSlack(knowledgeElement, "changed"));
	}



	@Test
	public void testCreateHashedPayload() {
		System.out.println("testCreateHashedPayload()");
		assertEquals(
				WebhookContentProvider.createHashedPayload("{\"issueKey\": \"CONDEC-1234\", \"ConDecTree\": "
						+ "{\"nodeStructure\":{\"children\":[],\"text\":{\"title\":\"Test Send\","
						+ "\"desc\":\"CONDEC-1234\"}},\"chart\":{\"container\":\"#treant-container\","
						+ "\"node\":{\"collapsable\":\"true\"},\"connectors\":{\"type\":\"straight\"},"
						+ "\"rootOrientation\":\"NORTH\",\"siblingSeparation\":30,\"levelSeparation\":30,"
						+ "\"subTreeSeparation\":30}}}", "03f90207-73bc-44d9-9848-d3f1f8c8254e"),
				"e7f0bb82f13286d1afea8cb59f07af829177e2bac8a7af4e883a074851152717");
	}

	@Test
	public void testCreateHashedPayloadWithUmlaut() {
		assertEquals(WebhookContentProvider.createHashedPayload(
				"{\"issueKey\":\"TEST-29\",\"ConDecTree\":{\"nodeStructure\":{\"children\":[{\"connectors\":{\"style\":{\"stroke\":\"#000000\"}},\"children\":[{\"connectors\":{\"style\":{\"stroke\":\"#000000\"}},\"children\":[{\"connectors\":{\"style\":{\"stroke\":\"#000000\"}},\"children\":[],\"htmlClass\":\"solution\",\"link\":{\"href\":\"http://localhost:2990/jira/browse/TEST-31\",\"target\":\"_blank\"},\"nodeContent\":{\"name\":\"Alternative\",\"title\":\"gfx\",\"desc\":\"TEST-31\"},\"htmlId\":10700}],\"htmlClass\":\"solution\",\"link\":{\"href\":\"http://localhost:2990/jira/browse/TEST-18\",\"title\":\"äundefinedfgreeygyrsehbnhzrdregsycgfanh\",\"target\":\"_blank\"},\"nodeContent\":{\"name\":\"Alternative\",\"title\":\"zweites22\",\"desc\":\"TEST-18\"},\"htmlId\":10601}],\"htmlClass\":\"decision\",\"link\":{\"href\":\"http://localhost:2990/jira/browse/TEST-17\",\"title\":\"undefined\",\"target\":\"_blank\"},\"nodeContent\":{\"name\":\"Decision\",\"title\":\"new decision12\",\"desc\":\"TEST-17\"},\"htmlId\":10600}],\"text\":{\"name\":\"Task\",\"title\":\"a\",\"desc\":\"TEST-29\"}},\"chart\":{\"container\":\"#treant-container\",\"node\":{\"collapsable\":\"true\"},\"connectors\":{\"type\":\"straight\"},\"rootOrientation\":\"NORTH\",\"siblingSeparation\":30,\"levelSeparation\":30,\"subTreeSeparation\":30}}}",
				"03f90207-73bc-44d9-9848-d3f1f8c8254e"),
				"dfb0bca5dbc85e926dd7bf519a1e4a1636bc534a1d13f623108919585aa26262");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCreateHashedPayloadEmptyKey() {
		WebhookContentProvider.createHashedPayload("{\"issueKey\": \"CONDEC-1234\", \"ConDecTree\": "
				+ "{\"nodeStructure\":{\"children\":[],\"text\":{\"title\":\"Test Send\","
				+ "\"desc\":\"CONDEC-1234\"}},\"chart\":{\"container\":\"#treant-container\","
				+ "\"node\":{\"collapsable\":\"true\"},\"connectors\":{\"type\":\"straight\"},"
				+ "\"rootOrientation\":\"NORTH\",\"siblingSeparation\":30,\"levelSeparation\":30,"
				+ "\"subTreeSeparation\":30}}}", "");
	}
}
