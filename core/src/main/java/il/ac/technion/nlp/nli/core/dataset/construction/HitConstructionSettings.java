package il.ac.technion.nlp.nli.core.dataset.construction;

import java.nio.file.Path;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class HitConstructionSettings {

    /**
     * @see #HitConstructionSettings(Path, Path, String, String, Path)
     */
    public final Path moduleResourceDir;
    /**
     * @see #HitConstructionSettings(Path, Path, String, String, Path)
     */
    public final Path tempDirectory;
    /**
     * @see #HitConstructionSettings(Path, Path, String, String, Path)
     */
    public final String onlineHitResourceWebDirUrl;
    /**
     * @see #HitConstructionSettings(Path, Path, String, String, Path)
     */
    public final String onlineDomainExamplesDirUrl;
    public final Path wkhtmltoimageAppExecutable;


    /**********************************************
     * Resource directory structure.
     **********************************************/


    public String hitInitialAndDesiredStatesFigureHtmlTemplateRelativeToResourceDir =
            "TBD/template.html";


    public String hitHtmlPageWithLargeImageTemplateRelativeToResourceDir =
            "TBD/template.html";

    public String entireHitDescriptionTemplateRelativeToResourceDir =
            "TBD/template.html";

    /**
     * Contains files of the format: [domain id].html
     */
    public String domainDescriptionHtmlTemplatesDirRelativeToResourceDir =
            "TBD/SpecificDomainHtmlTemplates";


    public String exampleDomainId = "exampleDomainWithBoxes";


    /**
     * Any argument that won't be used can be null.
     * @param moduleResourceDir          the resource directory of this module.
     * @param tempDirectory              a temporary directory will be created (and then deleted) in this directory.
     * @param onlineHitResourceWebDirUrl URL of a web directory containing all the files (html & png) defining hits
     *                                   (flat hierarchy - no subdirectories).
     * @param onlineDomainExamplesDirUrl In this web directory, for every domain id X there's a X.png - which is an
     *                                   initial & desired figure example for that domain.
     */
    public HitConstructionSettings(Path moduleResourceDir, Path tempDirectory, String onlineHitResourceWebDirUrl,
                                   String onlineDomainExamplesDirUrl, Path wkhtmltoimageAppExecutable) {
        this.moduleResourceDir = moduleResourceDir;
        this.tempDirectory = tempDirectory;
        this.onlineHitResourceWebDirUrl = onlineHitResourceWebDirUrl;
        this.onlineDomainExamplesDirUrl = onlineDomainExamplesDirUrl;
        this.wkhtmltoimageAppExecutable = wkhtmltoimageAppExecutable;
    }

}
