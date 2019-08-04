package il.ac.technion.nlp.nli.dataset1.domains.workforce_management.hit;

import il.ac.technion.nlp.nli.dataset1.domains.workforce_management.entities.Employee;
import il.ac.technion.nlp.nli.dataset1.domains.workforce_management.entities.Position;
import il.ac.technion.nlp.nli.dataset1.domains.workforce_management.entities.WorkforceManagementSystem;
import il.ac.technion.nlp.nli.core.method_call.MethodId;
import il.ac.technion.nlp.nli.core.dataset.NliMethod;

import java.util.Collection;

public enum WorkforceNliMethod implements NliMethod{

    ASSIGN_EMPLOYEES_TO_NEW_MANAGER,
    FIRE_EMPLOYEES,
    REASSIGN_EMPLOYEE_TO_NEW_POSITION,
    UPDATE_SALARY;


    @Override
    public MethodId getMethodId() {
        switch (this) {
            case ASSIGN_EMPLOYEES_TO_NEW_MANAGER:
                return new MethodId(WorkforceManagementSystem.class, "assignEmployeesToNewManager",
                        Collection.class, Employee.class);
            case FIRE_EMPLOYEES:
                return new MethodId(WorkforceManagementSystem.class, "fireEmployees", Collection.class);
            case REASSIGN_EMPLOYEE_TO_NEW_POSITION:
                return new MethodId(WorkforceManagementSystem.class, "assignEmployeeToNewPosition",
                        Employee.class, Position.class);
            case UPDATE_SALARY:
                return new MethodId(WorkforceManagementSystem.class, "updateSalary",
                        Employee.class, Integer.TYPE);
        }
        throw new Error();
    }

}
