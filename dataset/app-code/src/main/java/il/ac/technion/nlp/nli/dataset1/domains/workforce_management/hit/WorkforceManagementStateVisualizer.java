package il.ac.technion.nlp.nli.dataset1.domains.workforce_management.hit;

import com.ofergivoli.ojavalib.data_structures.set.SafeHashSet;
import com.ofergivoli.ojavalib.data_structures.set.SafeSet;
import il.ac.technion.nlp.nli.dataset1.domains.workforce_management.entities.Employee;
import il.ac.technion.nlp.nli.dataset1.domains.workforce_management.entities.WorkforceManagementSystem;
import il.ac.technion.nlp.nli.core.state.State;
import il.ac.technion.nlp.nli.core.dataset.visualization.StateVisualizer;
import il.ac.technion.nlp.nli.core.dataset.visualization.html.HtmlString;
import il.ac.technion.nlp.nli.core.dataset.visualization.html.TextColor;
import il.ac.technion.nlp.nli.core.dataset.visualization.html.TextFormatModification;
import il.ac.technion.nlp.nli.core.dataset.visualization.html.TextSizeModification;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class WorkforceManagementStateVisualizer extends StateVisualizer {

    private static final long serialVersionUID = 7774549385090549119L;

    private SafeSet<String> employeeIdsToEmphasizeManagerFor = new SafeHashSet<>();
    private SafeSet<String> employeeIdsToEmphasizeSalaryFor = new SafeHashSet<>();
    private SafeSet<String> employeeIdsToEmphasizePositionFor = new SafeHashSet<>();


    @Override
    public HtmlString getVisualRepresentation(State state) {

        WorkforceManagementSystem workforce = (WorkforceManagementSystem) state.getRootEntity();

        StringBuilder sb = new StringBuilder();

        sb.append("<div style=\"text-align: left;\">");

        TextFormatModification format = new TextFormatModification();
        format.setBold(true);
        format.setSizeInCssEm(TextSizeModification.BIGGER_XXX);
        format.setColor(TextColor.GREEN);
        String str = createHtmlFromStr("Employees:", format);
        sb.append(str);

        sb.append("<table style=\"font-size: 2.0em;border-collapse:collapse;text-align:center;\">")
                .append("<tr style=\"background-color: #ccffff;\">" +
                        "<td style=\"border-style: solid;border-width: 0.1em;vertical-align:center;padding:0.12em\"><span style=\"font-size: 0.6em\">" +
                        "Name" +
                        "</span></td>" +
                        "<td style=\"border-style: solid;border-width: 0.1em;vertical-align:center;padding:0.12em\"><span style=\"font-size: 0.6em\">" +
                        "Position" +
                        "</span></td>" +
                        "<td style=\"border-style: solid;border-width: 0.1em;vertical-align:center;padding:0.12em\"><span style=\"font-size: 0.6em\">" +
                        "Manager" +
                        "</span></td>" +
                        "<td style=\"border-style: solid;border-width: 0.1em;vertical-align:center;padding:0.12em\"><span style=\"font-size: 0.6em\">" +
                        "Salary" +
                        "</span></td>" +
                        "</tr>");

        workforce.employees.forEach(e-> writeVisualRepresentationOfEmployee(state, sb, e));
        sb.append("</table>");
        sb.append("</div>");
        return new HtmlString(sb.toString());
    }


    private void writeVisualRepresentationOfEmployee(State s, StringBuilder sb, Employee employee) {

        TextFormatModification format = new TextFormatModification();

        if (isEntityEntirelyEmphasized(s.getEntityId(employee)))
            format.setColor(TextColor.RED);

        String id = s.getEntityId(employee);

        String nameStr = createHtmlFromStr(employee.name, format);
        String positionStr = createHtmlFromStr(employee.position.toString(), format,
                employeeIdsToEmphasizePositionFor.safeContains(id), true);
        String managerStr = employee.manager == null ? "" :
                createHtmlFromStr(employee.manager.name, format, employeeIdsToEmphasizeManagerFor.safeContains(id), true);
        String salaryStr = createHtmlFromStr(NumberFormat.getNumberInstance(Locale.US).format(employee.salary),
                format, employeeIdsToEmphasizeSalaryFor.safeContains(id), true);

        String str = "<tr>" +
                "<td style=\"border-style: solid;border-width: 0.1em;vertical-align:center;padding:0.12em\"><span style=\"font-size: 0.6em\">" +
                nameStr +
                "</span></td>" +
                "<td style=\"border-style: solid;border-width: 0.1em;vertical-align:center;padding:0.12em\"><span style=\"font-size: 0.6em\">" +
                positionStr +
                "</span></td>" +
                "<td style=\"border-style: solid;border-width: 0.1em;vertical-align:center;padding:0.12em\"><span style=\"font-size: 0.6em\">" +
                managerStr +
                "</span></td>" +
                "<td style=\"border-style: solid;border-width: 0.1em;vertical-align:center;padding:0.12em\"><span style=\"font-size: 0.6em\">" +
                salaryStr +
                "</span></td>" +
                "</tr>";
        sb.append(str);
    }

    public void emphasizeManager(String employeeId) {
        employeeIdsToEmphasizeManagerFor.add(employeeId);
    }

    public void emphasizePosition(String employeeId) {
        employeeIdsToEmphasizePositionFor.add(employeeId);
    }

    public void emphasizeSalary(String employeeId) {
        employeeIdsToEmphasizeSalaryFor.add(employeeId);
    }
}
