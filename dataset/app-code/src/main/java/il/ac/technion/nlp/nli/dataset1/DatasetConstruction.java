package il.ac.technion.nlp.nli.dataset1;

import com.ofergivoli.ojavalib.io.GeneralFileUtils;
import com.ofergivoli.ojavalib.io.serialization.xml.XStreamSerialization;
import com.ofergivoli.ojavalib.time.TemporalFormat;
import il.ac.technion.nlp.nli.core.dataset.Dataset;
import il.ac.technion.nlp.nli.core.dataset.construction.Hit;
import il.ac.technion.nlp.nli.core.dataset.construction.HitConstructionSettings;
import il.ac.technion.nlp.nli.core.dataset.construction.InstructionQueryability;
import il.ac.technion.nlp.nli.core.dataset.construction.hit_random_generation.HitConstructionInfo;
import il.ac.technion.nlp.nli.core.dataset.construction.hit_random_generation.HitRandomGenerator;
import il.ac.technion.nlp.nli.core.dataset.construction.mturk.MTurkManager;
import il.ac.technion.nlp.nli.dataset1.domains.list.hit.ListHitRandomGenerator;
import il.ac.technion.nlp.nli.dataset1.domains.list.hit.ListNliMethod;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class DatasetConstruction
{

    /******************************************************************************************/
    /********************************** Settings **********************************************/
    /******************************************************************************************/


    // URLs:

    /**
     * In this web directory, for every domain id X there's a X.png - which is an initial & desired figure example for
     * that domain.
     */
    public static final String onlineDomainExamplesWebDirUrl =
            "TBD";

    /**
     * URL of a web directory containing all the files (html & png) defining hits (flat hierarchy, no sub-directories).
     */
    public static final String onlineHitResourceWebDirUrl =
            "TBD";



    // relative to home directory:

    private static final Path tempDirectory = GeneralFileUtils.getHomeDir().resolve("tmp");

    private static final Path datasetXmlFile  = GeneralFileUtils.getHomeDir().resolve(
            "tmp\\Dataset.xml");


    private static final Path targetDirForExistingDatasetXmlFileAfterRenamingToTimestamp =
            GeneralFileUtils.getHomeDir().resolve("old");

    private static final Path nliCoreResourceDir = GeneralFileUtils.getHomeDir().resolve("nli-core");

    private static Path wkhtmltoimageAppExecutable = GeneralFileUtils.getHomeDir().resolve(
            "wkhtmltopdf/bin/wkhtmltoimage.exe");




    // relative to the temp directory:

    private static final Path outputHitsCsvFile = tempDirectory.resolve("hitData/hits.csv");

    /**
     * All the files (html & png) defining hits are generated into this directory (flat hierarchy, no sub-directories).
     */
    private static final Path outputHitsResourceDir = tempDirectory.resolve("hitData/resource");



    /******************************************************************************************/
    /********************************** Rest of class ******************************************/
    /******************************************************************************************/



    public static List<Hit> createBatch(Dataset dataset, int hitsNumToGeneratePerFunction) {


        int SEED = 0;
        String BATCH_ID_PREFIX = "TBD";


        Random rand = new Random(SEED);
        List<Hit> hits = new LinkedList<>();

        HitRandomGenerator hitGenerator;
        ListNliMethod nliMethod;

//        hitGenerator = new LightingControlHitRandomGenerator(rand, LightMode.ON);
//        hitGenerator = new CalendarHitRandomGenerator(rand, REMOVE_EVENT);
//        hitGenerator = new FileManagerHitRandomGenerator(rand, REMOVE_FILE, dataset.getDatasetDomains());
//        hitGenerator = new MessengerHitRandomGenerator(rand, nliMethod, dataset.getDatasetDomains());
//        hitGenerator = new WorkforceManagementHitRandomGenerator(rand, nliMethod, dataset.getDatasetDomains());
//        hitGenerator = new ContainerManagementHitRandomGenerator(rand, nliMethod, dataset.getDatasetDomains());

        nliMethod = ListNliMethod.REMOVE;
        hitGenerator = new ListHitRandomGenerator(rand, nliMethod, dataset.getDatasetDomains());
        hits.addAll(generateHits(hitGenerator, BATCH_ID_PREFIX + "_" + nliMethod.getMethodId().getName() +"_",
                0,
                (int) Math.ceil(1.0/2* hitsNumToGeneratePerFunction),
                (int) Math.ceil(1.0/2* hitsNumToGeneratePerFunction),
                0));

        nliMethod = ListNliMethod.MOVE_TO_BEGINNING;
        hitGenerator = new ListHitRandomGenerator(rand, nliMethod, dataset.getDatasetDomains());
        hits.addAll(generateHits(hitGenerator, BATCH_ID_PREFIX + "_" + nliMethod.getMethodId().getName() +"_",
                0,
                (int) Math.ceil(1.0/1* hitsNumToGeneratePerFunction),
                0,
                0));

        nliMethod = ListNliMethod.MOVE_TO_END;
        hitGenerator = new ListHitRandomGenerator(rand, nliMethod, dataset.getDatasetDomains());
        hits.addAll(generateHits(hitGenerator, BATCH_ID_PREFIX + "_" + nliMethod.getMethodId().getName() +"_",
                0,
                (int) Math.ceil(1.0/1* hitsNumToGeneratePerFunction),
                0,
                0));


        Collections.shuffle(hits, rand);
        HitConstructionSettings hitConstructionSettings = new HitConstructionSettings(nliCoreResourceDir, tempDirectory,
                onlineHitResourceWebDirUrl, onlineDomainExamplesWebDirUrl, wkhtmltoimageAppExecutable);

        new MTurkManager(hitConstructionSettings).writeHitEntireDescriptionsCsvAndDependentResourceFiles(hits,
                outputHitsCsvFile.toFile(),outputHitsResourceDir.toFile());

        return hits;
    }

    private static void writeNewDatasetXmlFileMovingAsideOldOne(Dataset dataset) {
        try {
            // moving existing dataset xml (giving filename timestamp)
            String targetFileName = TemporalFormat.temporalToString(ZonedDateTime.now(), "yyyy-MM-dd__HH_mm_ss")
                    + "___Dataset.xml";
            Path targetPath = targetDirForExistingDatasetXmlFileAfterRenamingToTimestamp.resolve(targetFileName);
            Files.move(datasetXmlFile, targetPath);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        XStreamSerialization.writeObjectToXmlFile(dataset, datasetXmlFile.toFile());
    }




    /**
     * Note: The HITs that we generate here have
     * {@link HitConstructionInfo#queryableByPrimitiveRelations},
     * {@link HitConstructionInfo#someArgQueryableByComparison} and
     * {@link HitConstructionInfo#someArgQueryableBySuperlative} values as we choose, except for the
     * {@link HitConstructionInfo#someArgQueryableByComparison} values of the HITs counted for the argument
     * 'argQueryableBySuperlativeNum'. We allow those values to vary because all superlative operations denoting more
     * than one entity have a comparison operation with the same denotation.
     *
     * @param notQueryableByPrimitiveRelationsNum number of hits to generate
     * @param argQueryableBySuperlativeNum number of hits to generate
     * @param argQueryableByComparisonNum number of hits to generate
     * @param othersNum number of hits to generate
     */
    private static List<Hit> generateHits(
            HitRandomGenerator hitGenerator, String batchLabelForId,
            int notQueryableByPrimitiveRelationsNum,
            int argQueryableBySuperlativeNum,
            int argQueryableByComparisonNum,
            int othersNum) {

        List<Hit> res = new LinkedList<>();


        for (int i=0; i<notQueryableByPrimitiveRelationsNum; i++) {
            res.add(hitGenerator.generateHitObeyingPredicate(batchLabelForId + "NotQueryableByPrimitiveRel_",
                    info-> InstructionQueryability.getByHitConstructionInfo(info) ==
                            InstructionQueryability.NOT_QUERYABLE_BY_PRIMITIVE_REL_AND_NO_ARG_QUERYABLE_BY_SUPERLATIVE_OR_COMPARISON));
        }

        for (int i=0; i<argQueryableBySuperlativeNum; i++) {
            res.add(hitGenerator.generateHitObeyingPredicate(batchLabelForId + "QueryableBySuperlative_",
                    info->InstructionQueryability.getByHitConstructionInfo(info) ==
                                    InstructionQueryability.QUERYABLE_BY_PRIMITIVE_REL_AND_ARG_QUERYABLE_BY_SUPERLATIVE));
        }

        for (int i=0; i<argQueryableByComparisonNum; i++) {
            res.add(hitGenerator.generateHitObeyingPredicate(batchLabelForId + "QueryableByComparison_",
                    info->InstructionQueryability.getByHitConstructionInfo(info) ==
                                    InstructionQueryability.QUERYABLE_BY_PRIMITIVE_REL_AND_ARG_QUERYABLE_BY_COMPARISON_AND_NO_ARG_QUERYABLE_BY_SUPERLATIVE
                    ));
        }

        for (int i=0; i<othersNum; i++) {
            res.add(hitGenerator.generateHitObeyingPredicate(batchLabelForId + "queryableOnlyByPrimitiveRel_",
                    info->InstructionQueryability.getByHitConstructionInfo(info) ==
                                    InstructionQueryability.QUERYABLE_BY_PRIMITIVE_REL_AND_NO_ARG_QUERYABLE_BY_COMPARISON_OR_SUPERLATIVE
                    ));
        }

        return res;
    }

}
