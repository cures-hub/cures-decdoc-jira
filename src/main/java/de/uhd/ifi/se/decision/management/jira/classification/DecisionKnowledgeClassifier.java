package de.uhd.ifi.se.decision.management.jira.classification;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.util.JiraHome;
import de.uhd.ifi.se.decision.management.jira.classification.implementation.BinaryClassifierImplementation;
import de.uhd.ifi.se.decision.management.jira.classification.implementation.FineGrainedClassifierImpl;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Interface to identify decision knowledge in natural language texts using a
 * binary and fine grained supervised classifiers.
 */
public interface DecisionKnowledgeClassifier {

	/**
	 * @issue What is the best place to store the supervised text classifier related
	 *        data?
	 * @decision Clone git repo to JIRAHome/data/condec-plugin/classifier!
	 */
	public static final String DEFAULT_DIR = ComponentAccessor.getComponentOfType(JiraHome.class).getDataDirectory()
			.getAbsolutePath() + File.separator + "condec-plugin" + File.separator + "classifier" + File.separator;

	static final Logger LOGGER = LoggerFactory.getLogger(DecisionKnowledgeClassifier.class);

	/**
	 * Determines for a list of strings whether each string is relevant decision
	 * knowledge or not. The classifier needs a list of strings not just one string.
	 * 
	 * @param stringsToBeClassified
	 *            list of strings to be checked for relevance.
	 * @return list of boolean values in the same order as the input strings. Each
	 *         value indicates whether a string is relevant (true) or not (false).
	 */
	List<Boolean> makeBinaryPredictions(List<String> stringsToBeClassified);

	void trainBinaryClassifier(List<List<Double>> features, List<Integer> labels);

	List<KnowledgeType> makeFineGrainedPredictions(List<String> stringsToBeClassified);

	void trainFineGrainedClassifier(List<List<Double>> features, List<Integer> labels);

	List<List<Double>> preprocess(String stringsToBePreprocessed);

	Map<String, List> preprocess(List<String> stringsToBePreprocessed, List labels);

	BinaryClassifierImplementation getBinaryClassifier();

	FineGrainedClassifierImpl getFineGrainedClassifier();

}