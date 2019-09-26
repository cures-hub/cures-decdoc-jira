package de.uhd.ifi.se.decision.management.jira.classification.implementation;

import com.atlassian.gzipfilter.org.apache.commons.lang.ArrayUtils;
import de.uhd.ifi.se.decision.management.jira.classification.ClassificationTrainerARFF;
import de.uhd.ifi.se.decision.management.jira.classification.DecisionKnowledgeClassifier;
import de.uhd.ifi.se.decision.management.jira.model.DecisionKnowledgeElement;
import de.uhd.ifi.se.decision.management.jira.model.KnowledgeType;
import de.uhd.ifi.se.decision.management.jira.model.text.PartOfJiraIssueText;
import de.uhd.ifi.se.decision.management.jira.persistence.JiraIssueTextPersistenceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smile.validation.*;
import weka.core.Instance;
import weka.core.Instances;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.Math.round;
import static java.util.stream.Collectors.toList;

/**
 * Class responsible to train the supervised text classifier. For this purpose,
 * the project admin needs to create and select an ARFF file.
 */
public class OnlineClassificationTrainerImpl extends ClassificationTrainerARFF {

    private DecisionKnowledgeClassifier classifier;

    //private static OnlineClassificationTrainerImpl instance;

    protected static final Logger LOGGER = LoggerFactory.getLogger(OnlineClassificationTrainerImpl.class);

    public OnlineClassificationTrainerImpl() {
        this.classifier = new DecisionKnowledgeClassifierImpl();
        super.directory = new File(DecisionKnowledgeClassifier.DEFAULT_DIR);
        directory.mkdirs();
    }

    public OnlineClassificationTrainerImpl(String projectKey) {
        this();
        this.projectKey = projectKey;
    }

    public OnlineClassificationTrainerImpl(String projectKey, String fileName) {
        this(projectKey);
        if ((fileName == null || fileName.isEmpty())) {
            return;
        }
        this.instances = super.getInstancesFromArffFile(fileName);
    }

    public OnlineClassificationTrainerImpl(String projectKey, List<DecisionKnowledgeElement> trainingElement) {
        this(projectKey);
        this.setTrainingData(trainingElement);
    }

    @Override
    //is called after setting training-file
    public boolean train() {
        boolean isTrained = false;
        try {
            Map<String, List> trainingData = this.extractTrainingData(super.instances);
            Map preprocessedIsRelevantSentences = this.classifier.preprocess(trainingData.get("sentences"),
                    trainingData.get("labelsIsRelevant"));

            this.classifier.trainBinaryClassifier((List<List<Double>>) preprocessedIsRelevantSentences.get("features"),
                    (List<Integer>) preprocessedIsRelevantSentences.get("labels"));

            this.classifier.trainFineGrainedClassifier((List<List<Double>>) preprocessedIsRelevantSentences.get("features"),
                    (List<Integer>) preprocessedIsRelevantSentences.get("labels"));

            //this.evaluateTraining();

            this.classifier.getBinaryClassifier().saveToFile();
            this.classifier.getFineGrainedClassifier().saveToFile();

            isTrained = true;
        } catch (Exception e) {
            LOGGER.error("The classifier could not be trained. Message:" + e.getMessage());
        }
        return isTrained;
    }


    public boolean update(PartOfJiraIssueText sentence) {
        List<List<Double>> features = this.classifier.preprocess(sentence.getDescription());
        // classifier needs numerical value
        Integer labelIsRelevant = sentence.isRelevant() ? 1 : 0;
        for (List<Double> feature : features) {
            this.classifier.getBinaryClassifier().train(feature.toArray(Double[]::new), labelIsRelevant);

            KnowledgeType labelKnowledgeType = sentence.getType();
            this.classifier.getFineGrainedClassifier().train(feature.toArray(Double[]::new), labelKnowledgeType);
        }
        return true;
    }

    @Override
    public DecisionKnowledgeClassifier getClassifier() {
        return classifier;
    }

    public Instances getInstances() {
        return instances;
    }

    private Map<String, List> extractTrainingData(Instances trainingData) {
        Map extractedTrainingData = new HashMap();
        List sentences = new ArrayList();
        List labelsIsRelevant = new ArrayList();
        List labelsKnowledgeType = new ArrayList();
        //TODO: can we use the names instead of indices?
        //iterate over all instances
        for (int i = 0; i < trainingData.size(); i++) {
            Instance currInstance = trainingData.get(i);
            // last attribute is the sentence that needs to be classified
            sentences.add(currInstance.stringValue(currInstance.numAttributes() - 1));

            Integer isRelevant = 0;
            Integer fineGrainedLabel = -1;
            // iterate over the binary attributes for each possible class
            for (int j = 0; j < currInstance.numAttributes() - 1; j++) {
                if (round(currInstance.value(j)) == 1) {
                    isRelevant = 1;
                    fineGrainedLabel = j;
                }
            }
            labelsIsRelevant.add(isRelevant);
            labelsKnowledgeType.add(fineGrainedLabel);
        }

        extractedTrainingData.put("sentences", sentences);
        extractedTrainingData.put("labelsIsRelevant", labelsIsRelevant);
        extractedTrainingData.put("labelsKnowledgeType", labelsKnowledgeType);


        return extractedTrainingData;
    }

    /*
    public Map<ClassificationMeasure, Double> evaluateClassifier(Classifier classifier, List<ClassificationMeasure> measurements, List<PartOfJiraIssueText> sentences) throws Exception {
        Map measurementResults = new HashMap();
        // load validated data from database
        Instances sentences = loadMekaTrainingDataFromJiraIssueText(true);
        //predict classes
        //int[] predictions = classifier.predictProbabilities(features);
        //calculate measurements for each ClassificationMeasure in measurements
        for(ClassificationMeasure measurement : measurements){
         //   measurement.measure(truths, predictions);
        }
        //return results
        return measurementResults;
    }

    public Map<ClassificationMeasure, Double> evaluateClassifier(Classifier classifier, List<ClassificationMeasure> measurements) throws Exception {
        sente
        return evaluateClassifier(classifier, measurements, sentences);
    }


    public Map<ClassificationMeasure, Double> evaluateClassifier(Classifier classifier) throws Exception {
        //return results
        List<ClassificationMeasure> defaultMeasurements = new ArrayList<>();
        defaultMeasurements.add(new FMeasure());
        return evaluateClassifier(classifier, defaultMeasurements);
    }

     */

    private List<String> extractStringsFromDke(List<DecisionKnowledgeElement> sentences) {
        List<String> extractedStringsFromPoji = new ArrayList<String>();
        for (DecisionKnowledgeElement sentence : sentences) {
            extractedStringsFromPoji.add(sentence.getDescription());
        }
        return extractedStringsFromPoji;
    }

    public Map<String, Double> evaluateClassifier() throws Exception {
        // create and initialize default measurements list
        List<ClassificationMeasure> defaultMeasurements = new ArrayList<>();
        defaultMeasurements.add(new FMeasure());
        defaultMeasurements.add(new Precision());
        defaultMeasurements.add(new Accuracy());
        defaultMeasurements.add(new Recall());

        // load validated Jira Issue texts
        JiraIssueTextPersistenceManager manager = new JiraIssueTextPersistenceManager(projectKey);
        List<DecisionKnowledgeElement> partsOfText = manager.getUserValidatedPartsOfText(projectKey);
        return evaluateClassifier(defaultMeasurements, partsOfText);
    }

    public Map<String, Double> evaluateClassifier(List<ClassificationMeasure> measurements, List<DecisionKnowledgeElement> partOfJiraIssueTexts) throws Exception {
        Map<String, Double> resultsMap = new HashMap();
        List<DecisionKnowledgeElement> relevantPartOfJiraIssueTexts = partOfJiraIssueTexts
                .stream()
                .filter(x -> !x.getType().equals(KnowledgeType.OTHER))
                .collect(toList());

        // format data
        List<String> sentences = this.extractStringsFromDke(partOfJiraIssueTexts);
        List<String> relevantSentences = this.extractStringsFromDke(relevantPartOfJiraIssueTexts);
        // extract true values
        Integer[] binaryTruths = partOfJiraIssueTexts
                .stream()
                // when type equals other then it is irrelevant
                .map(x -> x.getType().equals(KnowledgeType.OTHER) ? 0 : 1)
                .collect(toList())
                .toArray(new Integer[partOfJiraIssueTexts.size()]);

        Integer[] fineGrainedTruths = relevantPartOfJiraIssueTexts
                .stream()
                .map(x -> classifier.getFineGrainedClassifier().mapKnowledgeTypeToIndex(x.getType()))
                .collect(toList())
                .toArray(new Integer[relevantPartOfJiraIssueTexts.size()]);

        //predict classes
        Integer[] binaryPredictions = this.classifier.makeBinaryPredictions(sentences)
                .stream()
                .map(x -> x ? 1 : 0)
                .collect(toList())
                .toArray(new Integer[sentences.size()]);

        Integer[] fineGrainedPredictions = this.classifier.makeFineGrainedPredictions(relevantSentences)
                .stream()
                .map(x -> this.classifier.getFineGrainedClassifier().mapKnowledgeTypeToIndex(x))
                .collect(toList())
                .toArray(new Integer[relevantSentences.size()]);

        //calculate measurements for each ClassificationMeasure in measurements
        for (ClassificationMeasure measurement : measurements) {
            String binaryKey = measurement.getClass().getName() + "_binary";
            Double binaryMeasurement = measurement.measure(ArrayUtils.toPrimitive(binaryTruths), ArrayUtils.toPrimitive(binaryPredictions));
            resultsMap.put(binaryKey, binaryMeasurement);

            String fineGrainedKey = measurement.getClass().getName() + "_fineGrained";
            Double fineGrainedMeasurement = measurement.measure(ArrayUtils.toPrimitive(fineGrainedTruths), ArrayUtils.toPrimitive(fineGrainedPredictions));
            resultsMap.put(fineGrainedKey, fineGrainedMeasurement);

        }
        //return results
        return resultsMap;
    }

}
