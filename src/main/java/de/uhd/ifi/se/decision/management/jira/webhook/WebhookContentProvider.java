package de.uhd.ifi.se.decision.management.jira.webhook;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.Set;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uhd.ifi.se.decision.management.jira.view.treant.Treant;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeElement;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeType;


/**
 * Creates the content submitted via the webhook. The content consists of a key
 * value pair. The key is an issue id. The value is the Treant JSON String.
 */
public class WebhookContentProvider {

	private String projectKey;
	private String rootElementKey;
	private String secret;
	private KnowledgeElement knowledgeElement;
	private String receiver;

	protected static final Logger LOGGER = LoggerFactory.getLogger(WebhookContentProvider.class);

	public WebhookContentProvider(String projectKey, String elementKey, String secret, String receiver) {
		this.projectKey = projectKey;
		this.rootElementKey = elementKey;
		this.secret = secret;
		this.receiver = receiver;

	}

	public WebhookContentProvider(String projectKey, KnowledgeElement knowledgeElement, String secret, String receiver) {
		this.projectKey = projectKey;
		this.rootElementKey = knowledgeElement.getKey();
		this.secret = secret;
		this.knowledgeElement = knowledgeElement;
		this.receiver = receiver;
	}

	/**
	 * Creates post method for a single tree of decision knowledge.
	 *
	 * @return post method ready to be posted
	 */
	public PostMethod createPostMethod() {
		PostMethod postMethod = new PostMethod();
		if (projectKey == null || rootElementKey == null || secret == null|| receiver == null) {
				System.out.println("createPostMethod null");
			return postMethod;
		}
		String webhookData = "";
		if(receiver == "Other"){
			LOGGER.info("receiver:  Other");
			System.out.println("createPostMethod:receiver= other");
			webhookData = createWebhookData();

		}
		if(receiver == "Slack"){
			LOGGER.info("receiver:  Slack");
			System.out.println("createPostMethod:receiver= slack");
			webhookData = createWebhookDataforSlack(this.knowledgeElement);
			System.out.println(webhookData);
		}
		try {
			StringRequestEntity requestEntity = new StringRequestEntity(webhookData, "application/json", "UTF-8");
			postMethod.setRequestEntity(requestEntity);
		} catch (UnsupportedEncodingException e) {
			LOGGER.error("Creating the post method failed. Message: " + e.getMessage());
		}
		Header header = new Header();
		header.setName("X-Hub-Signature");
		header.setValue("sha256=" + createHashedPayload(webhookData, secret));
		postMethod.setRequestHeader(header);
		return postMethod;
	}


	/**
	 * Creates the key value JSON String transmitted via webhook.
	 *
	 * @return JSON String with the following pattern: { "issueKey": {String},
	 *         "ConDecTree": {TreantJS JSON config and data} }
	 */
	private String createWebhookData() {
		String treantAsJson = createTreantJsonString();
		return "{\"issueKey\": \"" + this.rootElementKey + "\", \"ConDecTree\": " + treantAsJson + "}";
	}

	private String createWebhookDataforSlack(KnowledgeElement changedElement) {
		if(changedElement == null){
			return "";
		}
		String summary = knowledgeElement.getSummary();
		System.out.println("Summary is now: "+summary);

		if( summary.contains("{")){
			System.out.println("Summary contains {.");
			summary = cutSummary(changedElement.getSummary());
			System.out.println("Summary is now: "+summary);
		}
		String data = "{'blocks':[{'type':'section','text':{'type':'mrkdwn','text':'Folgendes Entscheidungswissen wurde angepasst'}},"+
		"{'type':'section','text':{'type':'mrkdwn','text':'*Typ:* :"+ changedElement.getType() + ": : " + changedElement.getType() +
		" \\n *Titel*: " + summary + "\\n'},"+
		"'accessory':{'type':'button','text':{'type':'plain_text','text':'Go to Jira'},'url' : '"+changedElement.getUrl()+"'}}]}";
		System.out.println("createPostMethod: data"+data);

		return data;
	}
	/**
	 * Creates the key value JSON String transmitted via webhook for Slack.
	 *(Differences: "text", mask \ and ")
	 * @return JSON String with the following pattern: {"text": " \"issueKey\": {String},
	 *         \"ConDecTree\": {TreantJS JSON config and data} " }
	 */
	public PostMethod createPostMethodforSlack(KnowledgeElement changedElement) {
		PostMethod postMethod = new PostMethod();
		if (projectKey == null || changedElement == null || receiver == null) {
				System.out.println("createPostMethod null");
			return postMethod;
		}
		String webhookData = "";
		if(receiver == "Other"){
			LOGGER.info("receiver:  Other");
			System.out.println("createPostMethodForSlack:receiver= other");
			return createPostMethod();

		}
		if(receiver == "Slack"){
			LOGGER.info("receiver:  Slack");
			System.out.println("createPostMethodForSlack:receiver= slack");
			webhookData = createWebhookDataforSlack(changedElement);
			System.out.println(webhookData);
		}
		try {
			StringRequestEntity requestEntity = new StringRequestEntity(webhookData, "application/json", "UTF-8");
			postMethod.setRequestEntity(requestEntity);
		} catch (UnsupportedEncodingException e) {
			LOGGER.error("Creating the post method failed. Message: " + e.getMessage());
		}
		Header header = new Header();
		header.setName("X-Hub-Signature");
		postMethod.setRequestHeader(header);
		return postMethod;
	}

/*
{'blocks':[{'type':'section','text':{'type':'mrkdwn','text':'Folgendes Entscheidungswissen wurde angepasst'}},
{'type':'divider'},{'type':'section','text':{'type':'mrkdwn','text':'*Typ:*:issue: knowledgetype \n *Titel*: summary\n'},
'accessory':{'type':'button','text':{'type':'plain_text','text':'Go to Jira'},'url':'https://google.com'}}]}
*/

	/**
	 * Creates the Treant JSON String (value transmitted via webhook).
	 *
	 * @return TreantJS JSON String including config and data
	 */
	private String createTreantJsonString() {
		Treant treant = new Treant(projectKey, rootElementKey, 4, true);
		ObjectMapper objectMapper = new ObjectMapper();
		String treantAsJson = "";
		try {
			treantAsJson = objectMapper.writeValueAsString(treant);
		} catch (IOException e) {
			LOGGER.error("Failed to create a treant json string for the webhook. Message: " + e.getMessage());
		}
		return treantAsJson;
	}

	/**
	 * Converts the webhook data String to a hexadecimal String using the secret
	 * key.
	 *
	 * @param data
	 *            String to be hashed
	 * @param key
	 *            secret key
	 *
	 * @return hexadecimal String
	 */
	public static String createHashedPayload(String data, String key) {
		final String hashingAlgorithm = "HMACSHA256";
		SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), hashingAlgorithm);
		String hexString = "";
		try {
			Mac mac = Mac.getInstance(hashingAlgorithm);
			mac.init(secretKeySpec);
			hexString = toHexString(mac.doFinal(data.getBytes("UTF-8")));
		} catch (NoSuchAlgorithmException | InvalidKeyException | UnsupportedEncodingException e) {
			LOGGER.error("Creating a hashed payload failed. Message: " + e.getMessage());
		}
		return hexString;
	}

	/**
	 * Converts an array of bytes to a hexadecimal String.
	 *
	 * @param bytes
	 *            array of bytes
	 *
	 * @return hexadecimal String
	 */
	private static String toHexString(byte[] bytes) {
		Formatter formatter = new Formatter();
		for (byte b : bytes) {
			formatter.format("%02x", b);
		}
		String formattedString = formatter.toString();
		formatter.close();
		return formattedString;
	}

/* if there is a decision knowledge element located in a comment there was a rare
 * bug: it puts "{*KnowledgeType*}" into the summary. that is bad for json format.
 * --> cutSummary
 */

	/**
	 * Remove all "{knowledgeType}"-parts from a string.
	 *
	 * @param toCut
	 *            String
	 *
	 * @return String without "{knowledgeType}"-parts
	 */
	private String cutSummary(String toCut){
		Set<KnowledgeType> types = knowledgeElement.getProject().getKnowledgeTypes();
		for (KnowledgeType knowledgeType : types) {
			String cut = "{"+knowledgeType.toString()+"}";
			toCut = toCut.split(cut)[0];
		}
		return toCut;
	}
}
