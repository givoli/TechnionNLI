package il.ac.technion.nlp.nli.dataset1;

import il.ac.technion.nlp.nli.core.dataset.DatasetDomains;
import il.ac.technion.nlp.nli.dataset1.domains.calendar.entities.Calendar;
import il.ac.technion.nlp.nli.dataset1.domains.file_manager.entities.FileManager;
import il.ac.technion.nlp.nli.dataset1.domains.lighting_control.entities.LightingControlSystem;
import il.ac.technion.nlp.nli.dataset1.domains.list.entities.SpecialList;
import il.ac.technion.nlp.nli.dataset1.domains.container_management.entities.ContainerManagementSystem;
import il.ac.technion.nlp.nli.dataset1.domains.messenger.Messenger;
import il.ac.technion.nlp.nli.dataset1.domains.workforce_management.entities.WorkforceManagementSystem;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class General {

    public static DatasetDomains getDatasetDomains() {
        DatasetDomains result = new DatasetDomains();
        result.addDomain("lighting_control", LightingControlSystem.class);
        result.addDomain("calendar", Calendar.class);
        result.addDomain("file_manager", FileManager.class);
        result.addDomain("messenger", Messenger.class);
        result.addDomain("workforce_management", WorkforceManagementSystem.class);
        result.addDomain("container_management", ContainerManagementSystem.class);
        result.addDomain("list", SpecialList.class);

        return result;
    }

}
