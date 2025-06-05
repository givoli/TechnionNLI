package il.ac.technion.nlp.nli.parser.experiment.analysis;

import com.google.common.base.Verify;
import ofergivoli.olib.data_structures.map.SafeHashMap;
import ofergivoli.olib.data_structures.map.SafeMap;
import ofergivoli.olib.data_structures.set.SafeHashSet;
import ofergivoli.olib.data_structures.set.SafeSet;
import ofergivoli.olib.io.GeneralFileUtils;
import ofergivoli.olib.io.IOUtils;
import ofergivoli.olib.io.TextIO;
import ofergivoli.olib.io.csv.CsvContent;
import ofergivoli.olib.io.csv.CsvDataCell;
import ofergivoli.olib.io.serialization.xml.XStreamSerialization;
import ofergivoli.olib.string.StringManager;
import il.ac.technion.nlp.nli.core.dataset.DatasetDomains;
import il.ac.technion.nlp.nli.core.dataset.Domain;
import il.ac.technion.nlp.nli.parser.experiment.ExperimentDirectory;
import il.ac.technion.nlp.nli.parser.experiment.ExperimentSettings;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class FeatureGeneralityTools {


    /**
     * @param featureGeneralityScoresDirectory the directory containing the xml files.
     */
    public static Path getFeatureGeneralityScoresXml(Path featureGeneralityScoresDirectory,
                                                     Domain domainToExclude){

        return featureGeneralityScoresDirectory.resolve(getFeatureGeneralityScoresXmlFilenameWithoutPath(
                domainToExclude));
    }

    /**
     * @param numberOfFeaturesToReturn In case this argument is greater than the number of features in the relevant
     *                                 input file, all the features in the input file are returned.
     * @return the features with highest generality scores. Deterministic.
     */
    public static SafeSet<String> getFeaturesByGeneralityScoreRank(int numberOfFeaturesToReturn,
                                                                   Path featureGeneralityScoresXml) {
        SafeMap<String,Double> featureToWeight = XStreamSerialization.readObjectFromTrustedXmlFile(
                false, featureGeneralityScoresXml.toFile());

        numberOfFeaturesToReturn = Math.min(numberOfFeaturesToReturn, featureToWeight.size());

        SafeSet<String> result = new SafeHashSet<>();
        //noinspection RedundantStreamOptionalCall
        result.addAll(featureToWeight.entrySet().stream()
                // This sort promises deterministic result even if multiple features have the same weight (because the
                // second sort is stable):
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .sorted(Comparator.comparing(featureAndWeight->-Math.abs(featureAndWeight.getValue())))
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(ArrayList::new))
                .subList(0,numberOfFeaturesToReturn));
        return result;
    }



    /**
     * @param inDomainExperimentsParentDirectory can contain multiple experiments for the same domain (e.g. for the two
     *                                           train/dev splits).
     * @param outputDirectory must not already exist.
     */
    public static void writeFeatureGeneralityScores(DatasetDomains datasetDomains,
                                                    Path inDomainExperimentsParentDirectory, Path outputDirectory){

        Verify.verify(!Files.exists(outputDirectory));
        GeneralFileUtils.createDirectories(outputDirectory);


        SafeMap<Pair<String,Domain>,Double> averageWeights = getAverageWeightsOfFeaturesInEachDomainFromInDomainExperiments(
                inDomainExperimentsParentDirectory, datasetDomains);


        SafeMap<Domain, SafeMap<String, Double>> domainExcludedToFeatureToWeight =
                getWeightSecondLargestInAbsValueExcludingSingleDomain(averageWeights);
        domainExcludedToFeatureToWeight.keySet().forEach(domainExcluded-> {
            SafeMap<String,Double> featureToWeight = domainExcludedToFeatureToWeight.safeGet(domainExcluded);
            Path outputXmlFile = outputDirectory.resolve(getFeatureGeneralityScoresXmlFilenameWithoutPath(domainExcluded));
            Path outputTextFile = outputDirectory.resolve(getFeatureGeneralityScoresTxtFilenameWithoutPath(domainExcluded));
            XStreamSerialization.writeObjectToXmlFile(featureToWeight, outputXmlFile);
            IOUtils.doAndClose(TextIO.getStandardStreamWriter(outputTextFile.toFile()), writer->
                    featureToWeight.entrySet().stream()
                            .sorted(Comparator.comparing(featureAndWeight->-featureAndWeight.getValue()))
                            .forEach(featureAndWeight-> IOUtils.write(writer,featureAndWeight.getKey() + "\t" +
                                    featureAndWeight.getValue() + "\n")));
        });

    }

    @NotNull
    private static String getFeatureGeneralityScoresTxtFilenameWithoutPath(Domain domainExcluded) {
        return "excluding__" + domainExcluded.getId() + ".txt";
    }

    @NotNull
    private static String getFeatureGeneralityScoresXmlFilenameWithoutPath(Domain domainExcluded) {
        return "excluding__" + domainExcluded.getId() + ".xml";
    }


    /**
     * @param inDomainExperimentsParentDirectory can contain multiple experiments for the same domain (e.g. for the two
     *                                   train/dev splits).
     * @return Key is a pair: [feature name, domain]. Value: the average weight of the feature in all experiments for
     * that domain.
     */
    private static SafeMap<Pair<String,Domain>,Double> getAverageWeightsOfFeaturesInEachDomainFromInDomainExperiments(
            Path inDomainExperimentsParentDirectory, DatasetDomains datasetDomains){

        SafeMap<Pair<String,Domain>,Double> result = new SafeHashMap<>();

        // how many times did we encountered this key.
        SafeMap<Pair<String,Domain>,Integer> counters = new SafeHashMap<>();

        ExperimentDirectory.getAllExperimentDirectoriesInDirectory(inDomainExperimentsParentDirectory).forEach(expDir->{
            ExperimentSettings settings = expDir.readExperimentSettings(false);
            Verify.verify(settings.setupType == ExperimentSettings.SetupType.IN_DOMAIN);
            Path weightsFile = expDir.getWeightsFile(null,settings.iterationsNum);
            StringManager.splitStringIntoNonEmptyLines(TextIO.readAllTextFromFileInStandardEncoding(
                    weightsFile.toFile())).forEach(line->{
                        String[] parts = line.split("\t");
                        Verify.verify(parts.length==2);
                        String featureName = parts[0];
                        double weight = Double.parseDouble(parts[1]);

                        Pair<String,Domain> pair = new ImmutablePair<>(featureName,
                                datasetDomains.getDomainById(settings.testDomainId));

                        if (!result.safeContainsKey(pair))
                            result.put(pair, 0.0);
                        if (!counters.safeContainsKey(pair))
                            counters.put(pair,0);

                        result.put(pair, result.safeGet(pair)+weight);
                        counters.put(pair, counters.safeGet(pair)+1);
                    });
        });

        result.forEach((pair,weight)->{
            Verify.verify(counters.safeGet(pair) <= 2);
            result.put(pair, weight/counters.safeGet(pair));
        });

        return result;

    }


    /**
     *
     * @return key - domain d; value - the output of {@link #getWeightSecondLargestInAbsValue(SafeMap) when excluding
     * from the input entries with domain d.
     */
    static SafeMap<Domain,SafeMap<String,Double>> getWeightSecondLargestInAbsValueExcludingSingleDomain(
            SafeMap<Pair<String,Domain>,Double> featureAndDomainToWeight) {

        SafeMap<Domain,SafeMap<String,Double>> result = new SafeHashMap<>();

        HashSet<Domain> domains = featureAndDomainToWeight.keySet().stream()
                .map(Pair::getRight)
                .collect(Collectors.toCollection(HashSet::new));


        domains.forEach(domainToExclude-> {
            SafeMap<Pair<String, Domain>, Double> featureAndDomainToWeightExcludingDomain = new SafeHashMap<>();
            featureAndDomainToWeight.keySet().stream()
                    .filter(featureAndDomain->!featureAndDomain.getRight().equals(domainToExclude))
                    .forEach(featureAndDomain->featureAndDomainToWeightExcludingDomain.put(
                            featureAndDomain, featureAndDomainToWeight.safeGet(featureAndDomain)));

            result.put(domainToExclude, getWeightSecondLargestInAbsValue(featureAndDomainToWeightExcludingDomain));
        });

        return result;
    }


    /**
     * @return Key: feature name; value: the weight of the feature in the domain in which it got the second largest
     * weight in absolute value - among all weights with the that sign.
     * There are no entries for features that did not appear in more than one domain with the same weight sign.
     * If there's a value to return for both weight signs, the value larger in absolute value is returned.
     */
    static SafeMap<String,Double> getWeightSecondLargestInAbsValue(
            SafeMap<Pair<String, Domain>, Double> featureAndDomainToWeight) {

        // Starting with the second largest (in abs value) weights among the negative weights, and then amending with
        // the second largest weights among the positive ones:
        SafeMap<String,Double> result = getWeightSecondLargestInAbsValue(featureAndDomainToWeight, -1);


        SafeMap<String,Double> positiveSecondLargestWeights =
                getWeightSecondLargestInAbsValue(featureAndDomainToWeight, 1);


        positiveSecondLargestWeights.forEach((feature,weight)->{
            if (!result.safeContainsKey(feature) || weight>-result.safeGet(feature))
                result.put(feature, weight);
        });

        return result;
    }


    /**
     * Like {@link #getWeightSecondLargestInAbsValueExcludingSingleDomain(SafeMap)}, but weights whose sigh is not 'sign' are ignored.
     * @param sign 1 or -1.
     */
    private static SafeMap<String,Double> getWeightSecondLargestInAbsValue(
            SafeMap<Pair<String,Domain>,Double> featureAndDomainToWeight, int sign) {

        Verify.verify(sign != 0);

        SafeMap<String,Double> result = new SafeHashMap<>();

        SafeMap<String,List<Double>> featureToWeights = new SafeHashMap<>();
        featureAndDomainToWeight.forEach((featureAndDomain,weight)->{
            String feature = featureAndDomain.getLeft();
            if (!featureToWeights.safeContainsKey(feature))
                featureToWeights.put(feature, new ArrayList<>());
            if (weight*sign > 0)
                featureToWeights.safeGet(feature).add(weight);
        });

        featureToWeights.entrySet().stream()
                .filter(featureAndWeights->featureAndWeights.getValue().size()>=2)
                .forEach(featureAndWeights->{

                    String feature = featureAndWeights.getKey();
                    List<Double> weights = featureAndWeights.getValue();

                    double secondLargest = weights.stream()
                            .sorted(Comparator.comparing(Math::abs))
                            .collect(Collectors.toCollection(ArrayList::new))
                            .get(weights.size()-2);

                    Double previousValue = result.put(feature, secondLargest);
                    Verify.verify(previousValue == null);
                });
        return result;
    }


    /**
     * Writes to 'outputCsvFile' an overall analysis about the generality of all features: For each subset of domains,
     * we get a list of features that appear in that subset.
     * @param outputCsvFile directories on path may either already exist or not.
     */
    public static void writeFeatureGeneralityAnalysis(SafeMap<Pair<String, Domain>, Double> featureAndDomainToWeight,
                                                      DatasetDomains datasetDomains, Path outputCsvFile){


        final String featuresHeaderCell = "feature";
        final String domainsNumAppearingInHeaderCell = "domainsNumAppearingIn";
        final String weightSecondLargestInAbsValueHeaderCell = "weight - second largest in abs value";
        final String absWeightSecondLargestInAbsValueHeaderCell = "abs weight - second largest";
        final String allDomainsHeaderCell = "allDomains";


        List<Domain> orderedDomains = getDomainsOrderedByDeterministicOrder(datasetDomains.getDomains());

        List<List<CsvDataCell>> rows = new LinkedList<>();




        SafeMap<SafeSet<Domain>,SafeSet<String>> domainSetToFeatures =
                getMapFromDomainSetToFeatures(featureAndDomainToWeight.keySet());

        SafeMap<String,Double>  featureToWeightSecondLargestInAbsValue =
                getWeightSecondLargestInAbsValue(featureAndDomainToWeight);

        List<String> header = new LinkedList<>();


        header.add(featuresHeaderCell);

        header.add(domainsNumAppearingInHeaderCell);
        header.add(weightSecondLargestInAbsValueHeaderCell);
        header.add(absWeightSecondLargestInAbsValueHeaderCell);
        header.add(allDomainsHeaderCell);
        orderedDomains.forEach(d->header.add(getHeaderCellShowingIfFeatureAppearsInDomain(d)));
        orderedDomains.forEach(d->header.add(getHeaderCellShowingFeatureAverageWeightInDomain(d)));


        //noinspection RedundantStreamOptionalCall
        domainSetToFeatures.entrySet().stream()
                //the following sort is determinism and for having sorted entries for each domains number.
                .sorted(Comparator.comparing(domainsSetAndFeatures->
                        domainSetToString(domainsSetAndFeatures.getKey())))
                // this sort is stable (which is important).
                .sorted(Comparator.comparing(domainsSetAndFeatures->-domainsSetAndFeatures.getKey().size()))
                .forEach(domainsSetAndFeatures-> {
                    SafeSet<Domain> domains = domainsSetAndFeatures.getKey();
                    domainsSetAndFeatures.getValue().forEach(feature -> {
                        List<CsvDataCell> row = new LinkedList<>();
                        rows.add(row);
                        row.add(new CsvDataCell(featuresHeaderCell, feature));
                        row.add(new CsvDataCell(domainsNumAppearingInHeaderCell, Integer.toString(domains.size())));
                        row.add(new CsvDataCell(allDomainsHeaderCell, domainSetToString(domains)));
                        Double weightSecondLargestInAbsValue = featureToWeightSecondLargestInAbsValue.safeGet(feature);
                        if (weightSecondLargestInAbsValue != null) {
                            row.add(new CsvDataCell(weightSecondLargestInAbsValueHeaderCell,
                                    Double.toString(weightSecondLargestInAbsValue)));
                            row.add(new CsvDataCell(absWeightSecondLargestInAbsValueHeaderCell,
                                    Double.toString(Math.abs(weightSecondLargestInAbsValue))));
                        }
                        domains.forEach(domain->{
                            row.add(new CsvDataCell(getHeaderCellShowingIfFeatureAppearsInDomain(domain), "1"));
                            row.add(new CsvDataCell(getHeaderCellShowingFeatureAverageWeightInDomain(domain),
                                    Double.toString(featureAndDomainToWeight.safeGet(
                                            new ImmutablePair<>(feature, domain)))));
                        });
                    });
                });

        GeneralFileUtils.createDirectories(outputCsvFile.getParent());
        CsvContent.createFromValuePairs(header, rows).writeEntireCsv(outputCsvFile);

    }


    /**
     * @see #writeFeatureGeneralityAnalysis(SafeMap, DatasetDomains, Path)
     */
    public static void writeFeatureGeneralityAnalysis(Path experimentsParentDirectory, DatasetDomains datasetDomains,
                                                      Path outputCsvFile){

        SafeMap<Pair<String, Domain>, Double> featureAndDomainToWeight =
                getAverageWeightsOfFeaturesInEachDomainFromInDomainExperiments(experimentsParentDirectory,
                        datasetDomains);
        writeFeatureGeneralityAnalysis(featureAndDomainToWeight, datasetDomains, outputCsvFile);
    }

    private static String getHeaderCellShowingFeatureAverageWeightInDomain(Domain d) {
        return d.getId() + " - average weight";
    }

    @NotNull
    private static String getHeaderCellShowingIfFeatureAppearsInDomain(Domain d) {
        return d.getId() + " - appears in";
    }



    /**
     * output is deterministic (does not depend on order of 'domains').
     */
    private static String domainSetToString(Collection<Domain> domains){
        return StringManager.collectionToStringWithDelimiter(
                getDomainsOrderedByDeterministicOrder(domains), ",");
    }

    private static List<Domain> getDomainsOrderedByDeterministicOrder(Collection<Domain> domains) {
        return domains.stream()
                .sorted(Comparator.comparing(Domain::getId))
                .collect(Collectors.toList());
    }


    /**
     * @return key: set of domains D. value: the set of features that the set of domains in which they appear is D.
     */
    private static SafeMap<SafeSet<Domain>,SafeSet<String>> getMapFromDomainSetToFeatures(
            Collection<Pair<String, Domain>> featureAndDomainPairs){


        SafeMap<String,SafeSet<Domain>> featureToDomains = new SafeHashMap<>();
        featureAndDomainPairs.forEach(featureAndDomain->{
            if (!featureToDomains.safeContainsKey(featureAndDomain.getLeft()))
                featureToDomains.put(featureAndDomain.getLeft(), new SafeHashSet<>());
            featureToDomains.safeGet(featureAndDomain.getLeft()).add(featureAndDomain.getRight());
        });


        SafeMap<SafeSet<Domain>,SafeSet<String>> result = new SafeHashMap<>();
        featureToDomains.forEach((feature,domains)-> {
            if (!result.safeContainsKey(domains))
                result.put(domains, new SafeHashSet<>());
            result.safeGet(domains).add(feature);
        });

        return result;
    }

}
