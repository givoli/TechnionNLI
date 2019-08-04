package il.ac.technion.nlp.nli.core.dataset.construction.mturk;

import com.google.common.base.Verify;
import com.ofergivoli.ojavalib.data_structures.map.SafeHashMap;
import com.ofergivoli.ojavalib.data_structures.map.SafeMap;
import com.ofergivoli.ojavalib.exceptions.UncheckedFileNotFoundException;
import com.ofergivoli.ojavalib.io.GeneralFileUtils;
import com.ofergivoli.ojavalib.io.TextIO;
import il.ac.technion.nlp.nli.core.dataset.Domain;
import il.ac.technion.nlp.nli.core.dataset.Example;
import il.ac.technion.nlp.nli.core.dataset.ExampleCorrectness;
import il.ac.technion.nlp.nli.core.dataset.construction.Hit;
import il.ac.technion.nlp.nli.core.dataset.construction.HitConstructionSettings;
import il.ac.technion.nlp.nli.core.dataset.visualization.html.HtmlString;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * In charge over exporting the necessary data for MTurk.
 *
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class MTurkManager {


    private final HitConstructionSettings hitConstructionSettings;




    public MTurkManager(HitConstructionSettings hitConstructionSettings) {
        this.hitConstructionSettings = hitConstructionSettings;
    }


    /**
     * Writes to 'outDir' a png file and an html file presenting that png file:
     * 		<ID>.png
     * 		<ID>.html
     */
    private void  exportHitResourceFiles(Hit hit, File outDir) {

        String imageFilename = getHitInitialDesiredImageFilename(hit);
        String htmlFilename = getHitInitialDesiredHtmlFilename(hit);


        hit.createInitialAndDesiredStatesFigure(hitConstructionSettings)
                .saveFileToDisk(new File (outDir,imageFilename), true);

        String largeImgHtml = TextIO.readAllTextFromFileInStandardEncoding(
                hitConstructionSettings.moduleResourceDir.resolve(
                        hitConstructionSettings.hitHtmlPageWithLargeImageTemplateRelativeToResourceDir).toFile())
                .replace("{{{___ImageUrl___}}}", imageFilename);

        TextIO.writeTextToFileInStandardEncoding( new File(outDir, htmlFilename), largeImgHtml, true);
    }

    @NotNull
    private static String getHitInitialDesiredHtmlFilename(Hit hit) {
        return hit.getId() + ".html";
    }

    @NotNull
    private static String getHitInitialDesiredImageFilename(Hit hit) {
        return hit.getId() + ".png";
    }


    /**
     * @param outResourceFilesDir if the directory does not exist, it is created (but its parent must exist).
     */
    public void writeHitEntireDescriptionsCsvAndDependentResourceFiles(
            List<Hit> hits, File outputCsv, File outResourceFilesDir) {

        if(!outResourceFilesDir.exists())
            Verify.verify(outResourceFilesDir.mkdir());

        hits.forEach(h -> exportHitResourceFiles(h,outResourceFilesDir));

        HitEntireDescriptionCsvRow.writeRowsToCsv(outputCsv,
                hits.stream().map(this::createEntireDescriptionCsvRowFromHit).collect(Collectors.toList()));
    }

    private HitEntireDescriptionCsvRow createEntireDescriptionCsvRowFromHit(Hit hit) {

        File templateFile = GeneralFileUtils.getHomeDir().resolve(
                hitConstructionSettings.entireHitDescriptionTemplateRelativeToResourceDir).toFile();


        String entireHitDescription = TextIO.readAllTextFromFileInStandardEncoding(templateFile)
                .replace("{{{exampleDomainWithBoxes_InitialAndDesiredStatesImageUrl}}}",
                        getDomainExampleImageUrl(hitConstructionSettings.exampleDomainId))
                .replace("{{{DomainDescriptionHtml}}}",
                        getDomainDescriptionHtml(hit.getDomain()).getString())
                .replace("{{{hit_InitialAndDesiredStatesImageUrl}}}",
                        getOnlineHitInitialDesiredImageUrl(hit))
                .replace("{{{hit_LargeImagePageUrl}}}", getOnlineHitInitialDesiredHtmlUrl(hit));

        return new HitEntireDescriptionCsvRow(hit.getId(), entireHitDescription);
    }

    private CharSequence getOnlineHitInitialDesiredHtmlUrl(Hit hit) {
        return hitConstructionSettings.onlineHitResourceWebDirUrl + "/" + getHitInitialDesiredHtmlFilename(hit);
    }

    private String getOnlineHitInitialDesiredImageUrl(Hit hit) {
        return hitConstructionSettings.onlineHitResourceWebDirUrl + "/" + getHitInitialDesiredImageFilename(hit);
    }

    private String getDomainExampleImageUrl(String domainId) {
        return hitConstructionSettings.onlineDomainExamplesDirUrl + "/" + domainId + ".png";
    }


    private HtmlString getDomainDescriptionHtml(Domain domain) {

        File domainDescriptionTemplate = hitConstructionSettings.moduleResourceDir.resolve(
                hitConstructionSettings.domainDescriptionHtmlTemplatesDirRelativeToResourceDir)
                .resolve(getDomainDescriptionHtmlTemplateFilename(domain)).toFile();

        String $ = TextIO.readAllTextFromFileInStandardEncoding(domainDescriptionTemplate)
                .replace("{{{domainExampleImageUrl}}}", hitConstructionSettings.onlineDomainExamplesDirUrl + "/" +
                        getDomainExampleImageFilename(domain));
        return new HtmlString($);
    }

    private static String getDomainExampleImageFilename(Domain domain) {
        return domain.getId() + ".png";
    }

    @NotNull
    private static String getDomainDescriptionHtmlTemplateFilename(Domain domain) {
        return domain.getId() + ".html";
    }



    /**
     * @param idToHit should contain an entry for every Hit in referred to from the csv.
     * @param readCorrectAndProblematicColumns when this is false, the "Correct" and "Problematic" columns may either
     *                                         appear in the CSV or not, and the effect is as if all the cells of those
     *                                         two columns are empty.
     * @param csv the csv results file from MTurk, after the relevant columns were added in case
     *            'readCorrectAndProblematicColumns' is true.
     *            Additional columns may be added, they have no effect.
     * @return a map from Hit id to created example.
     * @throws RuntimeException In case a HIT in csv is missing from idToHit.
     */
    public static SafeMap<String,Pair<Example, ExampleCorrectness>> createExamplesFromMTurkCsvResultFiles(
            SafeMap<String, Hit> idToHit, boolean readCorrectAndProblematicColumns, File csv) {

        SafeMap<String,Pair<Example, ExampleCorrectness>> result = new SafeHashMap<>();

        final String WORKER_ID_HEADER = "WorkerId";
        final String HIT_ID_HEADER = "Input.hitId";
        final String INSTRUCTION_HEADER = "Answer.instruction";
        final String CORRECT_HEADER = "Correct";
        final String PROBLEMATIC_HEADER = "Problematic";

        // TODO: factor out this useful CSV logic to ojavalib (each record should be a map, null values not allowed).
        CSVFormat csvFormat = CSVFormat.DEFAULT.withHeader().withSkipHeaderRecord();
        try (CSVParser csvParser = csvFormat.parse(new FileReader(csv))) {
            for (CSVRecord record : csvParser) {
                String workerId = record.get(WORKER_ID_HEADER);
                Verify.verify(workerId != null);
                String hitId = record.get(HIT_ID_HEADER);
                Verify.verify(hitId != null);
                String instruction = record.get(INSTRUCTION_HEADER);
                Verify.verify(instruction != null);

                @Nullable Boolean correct = null;
                @Nullable Boolean problematic = null;
                if (readCorrectAndProblematicColumns) {
                    correct = zeroOneStrToNullableBool(record.get(CORRECT_HEADER));
                    problematic = zeroOneStrToNullableBool(record.get(PROBLEMATIC_HEADER));
                }

                Hit hit = idToHit.safeGet(hitId);
                if (hit == null)
                    throw new RuntimeException("HIT is missing from argument. Missing HIT id: " + hitId);

                result.put(hit.getId(), new ImmutablePair<>(new Example(hit, instruction),
                        new ExampleCorrectness(correct, problematic)));
            }
        } catch (FileNotFoundException e) {
            throw new UncheckedFileNotFoundException(e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return result;
    }

    /**
     * @param str either empty string (for which null is returned), or 0 or 1.
     */
    private static @Nullable Boolean zeroOneStrToNullableBool(String str) {
        if (str.isEmpty())
            return null;
        if (str.equals("0"))
            return false;
        if (str.equals("1"))
            return true;
        throw new RuntimeException("Expected either empty string or \"0\" or \"1\", but got: " + str);
    }


}