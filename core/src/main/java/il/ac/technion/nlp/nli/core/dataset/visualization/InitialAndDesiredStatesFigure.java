package il.ac.technion.nlp.nli.core.dataset.visualization;

import ofergivoli.olib.io.TextIO;
import ofergivoli.olib.io.binary_files.PngFileInMemo;
import il.ac.technion.nlp.nli.core.dataset.construction.HitConstructionSettings;
import il.ac.technion.nlp.nli.core.dataset.visualization.html.HtmlString;
import il.ac.technion.nlp.nli.core.dataset.visualization.html.HtmlVisualizer;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class InitialAndDesiredStatesFigure {

    private final HtmlString initialStateVis;
    private final HtmlString desiredStateVis;

    public InitialAndDesiredStatesFigure(HtmlString initialStateVis, HtmlString desiredStateVis) {
        this.initialStateVis = initialStateVis;
        this.desiredStateVis = desiredStateVis;
    }


    public PngFileInMemo createPngImage(HitConstructionSettings hitConstructionSettings) {

        String result = TextIO.readAllTextFromFileInStandardEncoding(hitConstructionSettings.moduleResourceDir.resolve(
                hitConstructionSettings.hitInitialAndDesiredStatesFigureHtmlTemplateRelativeToResourceDir).toFile());

        result = result.replace("{{{___InitialState___}}}", initialStateVis.getString());
        result = result.replace("{{{___DesiredState___}}}", desiredStateVis.getString());

        return new HtmlVisualizer().createImageFromHtml(new HtmlString(result), hitConstructionSettings.tempDirectory,
                hitConstructionSettings.wkhtmltoimageAppExecutable);
    }

}
