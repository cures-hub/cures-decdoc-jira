package de.uhd.ifi.se.decision.management.jira.quality.commonmetriccalculator;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.uhd.ifi.se.decision.management.jira.model.KnowledgeType;

public class TestGetNumberOfLinkToOtherElementCommon extends SetupCommonCalculator {

	@Test
	public void testLinkFromNullLinkToNull() {
		assertEquals(0, calculator.getNumberOfLinksToOtherElement(null, null).size());
	}

	@Test
	public void testLinkFromFilledLinkToNull() {
		assertEquals(0, calculator.getNumberOfLinksToOtherElement(KnowledgeType.DECISION, null).size());
	}

	@Test
	public void testLinkFromNullLinkToFilled() {
		assertEquals(0, calculator.getNumberOfLinksToOtherElement(null, KnowledgeType.ISSUE).size());
	}

	@Test
	public void testLinkFromFilledLinkToFilled() {
		assertEquals(2,
				calculator.getNumberOfLinksToOtherElement(KnowledgeType.ARGUMENT, KnowledgeType.DECISION).size());
	}
}
