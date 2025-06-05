package il.ac.technion.nlp.nli.dataset1.domains.workforce_management.entities;

import com.google.common.base.Verify;
import ofergivoli.olib.data_structures.set.SafeHashSet;
import ofergivoli.olib.data_structures.set.SafeSet;
import il.ac.technion.nlp.nli.core.EnableNli;
import il.ac.technion.nlp.nli.core.NliDescriptions;
import il.ac.technion.nlp.nli.core.method_call.InvalidNliMethodInvocation;
import il.ac.technion.nlp.nli.core.state.NliRootEntity;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

/**
 * The graph defined by the "manager" relation between workers is a directed tree, rooted in some "root manager" (the CEO).
 *
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class WorkforceManagementSystem implements NliRootEntity {

    private static final long serialVersionUID = 2186922415927528888L;

    public List<Employee> employees = new LinkedList<>();

    public List<Employee> getEmployeesRecursivelyUnderManager(Employee manager) {
        List <Employee> result = new LinkedList<>();
        Stack<Employee> pending = new Stack<>();
        pending.add(manager);
        while (!pending.isEmpty()) {
            Employee current = pending.pop();
            employees.stream()
                    .filter(e->e.manager== current)
                    .forEach(e->{
                        pending.push(e);
                        result.add(e);
                    });
        }
        return result;
    }

    /**
     * @return the manager that all other employees are under.
     */
    public Employee getTopManager() {
        List<Employee> result = employees.stream()
                .filter(e->e.manager == null)
                .collect(Collectors.toList());
        Verify.verify(result.size() == 1);
        return result.get(0);
    }

    @NliDescriptions(descriptions = {"assign", "manager"})
    @EnableNli
    public void assignEmployeesToNewManager(Collection<Employee> employees, Employee newManager) {
        if (newManager.position != Position.MANAGER)
            throw new InvalidNliMethodInvocation();
        employees.forEach(e->e.manager = newManager);
    }

    /**
     * @throws InvalidNliMethodInvocation in case there's an employee that's not fired that has a manager that is fired.
     */
    @NliDescriptions(descriptions = {"fire", "terminate", "remove"})
    @EnableNli
    public void fireEmployees(Collection<Employee> c) {
        SafeSet<Employee> employeesNotFired = new SafeHashSet<>(employees);
        c.forEach(employeesNotFired::safeRemove);

        if (employeesNotFired.stream().anyMatch(e->
                e.manager != null && !employeesNotFired.safeContains(e.manager))) {
            //  there's an employee that's not fired that has a manager that is fired.
            throw new InvalidNliMethodInvocation();
        }
        this.employees.removeAll(c);
    }


    @NliDescriptions(descriptions = {"reassign", "position"})
    @EnableNli
    public void assignEmployeeToNewPosition(Employee employee, Position newPosition) {
        employee.position = newPosition;
    }

    @NliDescriptions(descriptions = {"salary", "update", "set"})
    @EnableNli
    public void updateSalary(Employee employee, int newSalary) {
        employee.salary = newSalary;
    }


}
