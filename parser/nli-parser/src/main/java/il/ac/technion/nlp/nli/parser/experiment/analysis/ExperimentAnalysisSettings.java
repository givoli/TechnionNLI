package il.ac.technion.nlp.nli.parser.experiment.analysis;

import java.io.Serializable;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class ExperimentAnalysisSettings implements Serializable {

    private static final long serialVersionUID = -8935934564571350265L;


    public boolean saveLogicalFormsOfTopPredictedDerivationsForTestInferences = false;
    public boolean saveFunctionCallsOfTopPredictedDerivationsForTestInferences = false;
    public boolean buildFeatureAndDomainToGradientSumMap = false;
    public boolean copyToAnalysisDirWeightFiles = false;
    /**
     * If this is true, copyToAnalysisDirWeightFiles should also be true.
     */
    public boolean copyToAnalysisDirNonFinalWeightFiles = false;

    /**
     * This should be true only when the number of inferences is very low.
     */
    public boolean saveAllCandidateLogicalFormsWithTheirFeatureData = false;

    /**
     * TODO: delete this - not used (appears in XMLs).
     * This should be true only when the number of inferences is very low.
     */
    public boolean saveFeatureDataForCandidateLogicalFormsInAllTestExamples = false;

    public boolean saveAllDerivations = false;

    public boolean logMemoryUsage = true;


    // Specifically for CWU (value not relevant in case CWU not used):
    public boolean collectAggregatedAnalysisDataForCWU = false;

    public boolean logDerivationDepthOrSize = false;

}

