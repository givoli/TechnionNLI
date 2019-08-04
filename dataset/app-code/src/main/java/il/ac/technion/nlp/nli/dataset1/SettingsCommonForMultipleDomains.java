package il.ac.technion.nlp.nli.dataset1;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class SettingsCommonForMultipleDomains {

    public final Path moduleResourceDir;


    public SettingsCommonForMultipleDomains(Path moduleResourceDir) {
        this.moduleResourceDir = moduleResourceDir;
    }


    /**********************************************
     * General
     **********************************************/

    public ArrayList<String> someFirstNames = new ArrayList<>(Arrays.asList(
            "Jessica", "Robert", "Amanda", "Brian", "Sarah", "Andrew",  "Kevin", "Amy", "Stacy", "Adam", "Peter",
            "Anna"));


    /**********************************************
     * Resource directory structure.
     **********************************************/

    public String firstNamesFile_US_1980_DescendingOccurrences_relativeToResourceDir =
            "TBD/US_FirstNames__YearOfBirth_1980__DescendingOccurrences.txt";


}
