package il.ac.technion.nlp.nli.dataset1.domains.file_manager.hit;

import il.ac.technion.nlp.nli.dataset1.domains.file_manager.entities.Directory;
import il.ac.technion.nlp.nli.dataset1.domains.file_manager.entities.File;
import il.ac.technion.nlp.nli.dataset1.domains.file_manager.entities.FileManager;
import il.ac.technion.nlp.nli.core.state.State;
import il.ac.technion.nlp.nli.core.dataset.visualization.StateVisualizer;
import il.ac.technion.nlp.nli.core.dataset.visualization.html.HtmlString;
import il.ac.technion.nlp.nli.core.dataset.visualization.html.TextColor;
import il.ac.technion.nlp.nli.core.dataset.visualization.html.TextFormatModification;
import il.ac.technion.nlp.nli.core.dataset.visualization.html.TextSizeModification;

import java.util.Comparator;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class FileManagerStateVisualizer extends StateVisualizer {

    private static final long serialVersionUID = 184542375011623094L;

    @Override
    public HtmlString getVisualRepresentation(State state) {

        FileManager fileManager = (FileManager) state.getRootEntity();


        if (!fileManager.cwd.childFiles.isEmpty())
            throw new RuntimeException("Currently does not support case where the cwd contains files");

        StringBuilder sb = new StringBuilder();

        sb.append("<div style=\"text-align: left;\">");


        boolean firstDir = true;
        for (Directory dir : fileManager.cwd.childDirectories) {
            if (!dir.childDirectories.isEmpty())
                throw new RuntimeException("Currently does not support file tree deeper than 2");
            if (!firstDir)
                sb.append("<br>");
            writeVisualRepresentationOfDirectory(state, sb, dir);
            firstDir = false;
        }

        sb.append("</div>");
        return new HtmlString(sb.toString());
    }



    private void writeVisualRepresentationOfDirectory(State s, StringBuilder sb, Directory dir) {

        if (!dir.childDirectories.isEmpty())
            throw new RuntimeException("Currently does not support visualizing such a deep tree");

        TextFormatModification format = new TextFormatModification();
        format.setBold(true);
        format.setSizeInCssEm(TextSizeModification.BIGGER_XXX);
        format.setColor(TextColor.GREEN);

        String str = new HtmlString("Files in directory \"" + dir.name + "\":", format,true).getString();
        sb.append(str);

        if (dir.childFiles.isEmpty()) {
            sb.append("<div>-- directory is empty --</div>");
            return;
        }


        sb.append("<table style=\"font-size: 2.0em;border-collapse:collapse;text-align:center;\">")
            .append("<tr style=\"background-color: #ccffff;\"><td style=\"border-style: solid;border-width: 0.1em;vertical-align:center;padding:0.12em\"><span style=\"font-size: 0.6em\">" +
                    "File Name" +
                    "</span></td>" +
                    "<td style=\"border-style: solid;border-width: 0.1em;vertical-align:center;padding:0.12em\"><span style=\"font-size: 0.6em\">" +
                    "File Type" +
                    "</span></td>" +
                    "<td style=\"border-style: solid;border-width: 0.1em;vertical-align:center;padding:0.12em\"><span style=\"font-size: 0.6em\">" +
                    "File Size (bytes)" +
                    "</span></td>" +
                    "</tr>");

        dir.childFiles.stream()
                .sorted(Comparator.comparing(f -> f.name))
                .forEach(file->writeVisualRepresentationOfFile(s, sb, file));

        sb.append("</table>");
    }

    private void writeVisualRepresentationOfFile(State s, StringBuilder sb, File file) {

        TextFormatModification format = new TextFormatModification();
        if (isEntityEntirelyEmphasized(s.getEntityId(file)))
            format.setColor(TextColor.RED);

        String str = "<tr>" +
                "<td style=\"border-style: solid;border-width: 0.1em;vertical-align:center;padding:0.12em\"><span style=\"font-size: 0.6em\">" +
                createHtmlFromStr(file.name, format) +
                "</span></td>" +
                "<td style=\"border-style: solid;border-width: 0.1em;vertical-align:center;padding:0.12em\"><span style=\"font-size: 0.6em\">" +
                createHtmlFromStr(file.type, format) +
                "</span></td>" +
                "<td style=\"border-style: solid;border-width: 0.1em;vertical-align:center;padding:0.12em\"><span style=\"font-size: 0.6em\">" +
                createHtmlFromStr(Integer.toString(file.sizeInBytes), format) +
                "</span></td>" +
                "</tr>";

        sb.append(str);
    }

}
