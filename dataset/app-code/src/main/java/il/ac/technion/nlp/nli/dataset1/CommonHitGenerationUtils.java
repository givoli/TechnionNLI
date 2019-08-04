package il.ac.technion.nlp.nli.dataset1;

import com.ofergivoli.ojavalib.io.TextIO;
import com.ofergivoli.ojavalib.string.StringManager;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class CommonHitGenerationUtils {

    private final SettingsCommonForMultipleDomains settingsCommonForMultipleDomains;



    public CommonHitGenerationUtils(SettingsCommonForMultipleDomains settingsCommonForMultipleDomains) {
        this.settingsCommonForMultipleDomains = settingsCommonForMultipleDomains;
    }

    public ArrayList<String> getManyFirstNamesSortedByDescendingOccurrences(){
        List<String> result = StringManager.splitStringIntoNonEmptyLines(
                TextIO.readAllTextFromFileInStandardEncoding(settingsCommonForMultipleDomains.moduleResourceDir.resolve(
                        settingsCommonForMultipleDomains.firstNamesFile_US_1980_DescendingOccurrences_relativeToResourceDir)
                        .toFile())).subList(0, 3000);
        return new ArrayList<>(result);
    }




}
