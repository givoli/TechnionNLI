package il.ac.technion.nlp.nli.parser.general;

import com.google.common.base.Verify;
import com.ofergivoli.ojavalib.data_structures.map.SafeHashMap;
import com.ofergivoli.ojavalib.data_structures.map.SafeMap;
import il.ac.technion.nlp.nli.core.dataset.Domain;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class ConditionalWeightUpdater {


    private @Nullable SafeMap<String,Double> featureToSumOfAbsUpdatesSoFar;
    private @Nullable SafeMap<String,Double> featureToSumOfAbsUnconditionalUpdatesSoFar;


    /**
     * Pending updates with smaller absolute value than this will get clipped.
     */
    private final double MIN_PENDING_UPDATE_ABS_VALUE_TO_KEEP = 1e-10;


    private final SafeMap<String, SafeMap<Domain, Double>> featureToDomainToPendingUpdate = new SafeHashMap<>();
    private final double unconditionalWeightUpdateFraction;
    private final int domainsNumberRequired;


    public ConditionalWeightUpdater(double unconditionalWeightUpdateFraction, int domainsNumberRequired,
                                    boolean collectAggregatedAnalysisData) {

        Verify.verify(domainsNumberRequired>0);

        this.unconditionalWeightUpdateFraction = unconditionalWeightUpdateFraction;
        this.domainsNumberRequired = domainsNumberRequired;

        if(collectAggregatedAnalysisData){
            featureToSumOfAbsUpdatesSoFar = new SafeHashMap<>();
            featureToSumOfAbsUnconditionalUpdatesSoFar = new SafeHashMap<>();
        }
    }



    /**
     * @param domain the domain of the current example (i.e. the example triggering this update).
     * @param requestedUpdate the update that Sempre originally intended to add to the weight.
     * @return the actual update to be added to the weight.
     */
    public double calcWeightUpdate(String featureName, Domain domain, double requestedUpdate) {

        double unconditionalUpdate = requestedUpdate * unconditionalWeightUpdateFraction;
        addUpdateToAggregationMap(featureToSumOfAbsUnconditionalUpdatesSoFar, featureName, unconditionalUpdate);

        SafeMap<Domain, Double> domainToPendingUpdate = featureToDomainToPendingUpdate.safeGet(featureName);
        if (domainToPendingUpdate == null) {
            domainToPendingUpdate = new SafeHashMap<>();
            featureToDomainToPendingUpdate.put(featureName, domainToPendingUpdate);
        }
        Double previousPendingUpdate = domainToPendingUpdate.safeGet(domain);
        if (previousPendingUpdate == null)
            previousPendingUpdate = 0.0;
        double newPendingUpdate = previousPendingUpdate + requestedUpdate;
        domainToPendingUpdate.put(domain, newPendingUpdate);

        double result = unconditionalUpdate;

        if (Math.signum(requestedUpdate) == Math.signum(newPendingUpdate) &&
                domainToPendingUpdate.size() >= domainsNumberRequired) {

            // Checking if 'result' should be modified by 'domainToPendingUpdate'.

            ArrayList<Double> pendingUpdatesOfAllDomains =
                    domainToPendingUpdate.values().stream()
                            .sorted()
                            .collect(Collectors.toCollection(ArrayList::new));

            int indexOfUpdateToApply = requestedUpdate > 0 ?
                    pendingUpdatesOfAllDomains.size() - domainsNumberRequired : domainsNumberRequired - 1;
            double pendingUpdateToApply = pendingUpdatesOfAllDomains.get(indexOfUpdateToApply);

            if (Math.signum(requestedUpdate) == Math.signum(pendingUpdateToApply) &&
                    Math.abs(pendingUpdateToApply) > Math.abs(unconditionalUpdate)) {
                result = pendingUpdateToApply;
            }
        }



        // Reducing 'result' from pending updates:
        // For 'domain' we need to reduce the entire 'result' (regardless of signs):
        domainToPendingUpdate.put(domain, newPendingUpdate - result);
        // For all other domains, we reduce 'result' from the pending update (and clip to zero if sign flips), but only
        // if the signs of the two are the same.
        double signOfResult = Math.signum(result);
        double finalResult = result;
        domainToPendingUpdate.entrySet().stream()
                .filter(entry->!entry.getKey().equals(domain))
                .filter(entry->Math.signum(entry.getValue()) == signOfResult)
                .forEach(entry->entry.setValue(reduceOrClipToZero(entry.getValue(), finalResult)));



        // removing from the 'featureToDomainToPendingUpdate' data structure the objects that should no longer exist:
        List<Domain> domainsToRemove = domainToPendingUpdate.entrySet().stream()
                .filter(entry->Math.abs(entry.getValue()) < MIN_PENDING_UPDATE_ABS_VALUE_TO_KEEP)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        for (Domain d : domainsToRemove)
            domainToPendingUpdate.safeRemove(d);
        if (domainToPendingUpdate.isEmpty())
            featureToDomainToPendingUpdate.safeRemove(featureName);


        addUpdateToAggregationMap(featureToSumOfAbsUpdatesSoFar, featureName, result);
        return result;
    }

    /**
     * @param update may be negative (this method uses the abs value).
     */
    private void addUpdateToAggregationMap(@Nullable SafeMap<String,Double> map, String featureName,
                                           double update) {
        if (map == null)
            return;
        if (!map.safeContainsKey(featureName))
            map.putNewKey(featureName, 0.0);
        map.put(featureName, map.getExisting(featureName)+ Math.abs(update));
    }


    /**
     * @return either (valueToReduceFrom-valueToReduce) or 0 if that value has a different sign than valueToReduceFrom.
     */
    private double reduceOrClipToZero(double valueToReduceFrom, double valueToReduce) {
        double result = valueToReduceFrom-valueToReduce;
        if (Math.signum(result) != Math.signum(valueToReduceFrom))
            return 0;
        return result;
    }


    /**
     * All possible arguments are valid (0 is returned in case the data structure does not contain an entry for the
     * arguments).
     */
    double getPendingUpdate(String feature, Domain domain) {
        SafeMap<Domain, Double> domainToPendingUpdate = featureToDomainToPendingUpdate.safeGet(feature);
        if (domainToPendingUpdate == null)
            return 0;
        if (!domainToPendingUpdate.safeContainsKey(domain))
            return 0;
        return domainToPendingUpdate.getExisting(domain);
    }

    /**
     *  @throws RuntimeException in case the user of this class chose to not collect this data.
     */
    public String getAggregatedAnalysisDataString(){

        StringBuilder sb = new StringBuilder();

        Verify.verify(featureToSumOfAbsUpdatesSoFar != null);
        Verify.verify(featureToSumOfAbsUnconditionalUpdatesSoFar != null);

        sb.append("feature\tsum of abs updates\tfraction of sum of abs unconditional updates\n");
        featureToSumOfAbsUpdatesSoFar.entrySet().stream()
                .sorted(Comparator.comparing(entry->-entry.getValue()))
                .forEach(entry->{
                    String feature = entry.getKey();
                    double totalUpdate = entry.getValue();
                    double unconditionalUpdate = featureToSumOfAbsUnconditionalUpdatesSoFar.getExisting(feature);
                    String unconditionalFractionStr;
                    if (totalUpdate==0)
                        unconditionalFractionStr = "NA";
                    else
                        unconditionalFractionStr = "" + unconditionalUpdate / totalUpdate;
                    sb.append(feature + "\t" + totalUpdate + "\t" + unconditionalFractionStr + "\n");
                });
        return sb.toString();
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        featureToDomainToPendingUpdate.forEach((feature,domainToPendingUpdate)->{
            sb.append("feature: [" + feature + "]\n");
            domainToPendingUpdate.forEach((domain,pendingUpdate)->
                    sb.append("\t" + domain.getId() + "\t" + pendingUpdate + "\n"));
        });
        return sb.toString();
    }

}
