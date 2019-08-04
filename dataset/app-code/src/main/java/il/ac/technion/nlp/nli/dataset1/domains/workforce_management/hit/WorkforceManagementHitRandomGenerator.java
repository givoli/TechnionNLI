package il.ac.technion.nlp.nli.dataset1.domains.workforce_management.hit;

import com.ofergivoli.ojavalib.data_structures.set.SafeHashSet;
import com.ofergivoli.ojavalib.data_structures.set.SafeSet;
import il.ac.technion.nlp.nli.core.dataset.DatasetDomains;
import il.ac.technion.nlp.nli.core.dataset.construction.hit_random_generation.HitRandomGenerator;
import il.ac.technion.nlp.nli.core.dataset.visualization.StateVisualizer;
import il.ac.technion.nlp.nli.core.method_call.MethodCall;
import il.ac.technion.nlp.nli.core.method_call.NonPrimitiveArgument;
import il.ac.technion.nlp.nli.core.method_call.PrimitiveArgument;
import il.ac.technion.nlp.nli.core.state.NliRootEntity;
import il.ac.technion.nlp.nli.core.state.State;
import il.ac.technion.nlp.nli.dataset1.CommonHitGenerationUtils;
import il.ac.technion.nlp.nli.dataset1.SettingsCommonForMultipleDomains;
import il.ac.technion.nlp.nli.dataset1.domains.workforce_management.entities.Employee;
import il.ac.technion.nlp.nli.dataset1.domains.workforce_management.entities.Position;
import il.ac.technion.nlp.nli.dataset1.domains.workforce_management.entities.WorkforceManagementSystem;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class WorkforceManagementHitRandomGenerator extends HitRandomGenerator
{


    private final WorkforceNliMethod nliMethod;
    private final SettingsCommonForMultipleDomains settingsCommonForMultipleDomains;

    public WorkforceManagementHitRandomGenerator(Random rand, WorkforceNliMethod nliMethod,
                                                 DatasetDomains datasetDomains,
                                                 SettingsCommonForMultipleDomains settingsCommonForMultipleDomains) {

        super(datasetDomains.getDomainByRootEntityClass(WorkforceManagementSystem.class), rand,
                WorkforceManagementStateVisualizer::new);
        this.nliMethod = nliMethod;
        this.settingsCommonForMultipleDomains = settingsCommonForMultipleDomains;
    }

    @Nullable
    @Override
    protected NliRootEntity generateRandomRootEntityForInitialState() {

        final int MIN_EMPLOYEES_NUM = 3;
        final int MAX_EMPLOYEES_NUM = 5;

        WorkforceManagementSystem workforce = new WorkforceManagementSystem();

        int workersNum = sampleUniformly(MIN_EMPLOYEES_NUM, MAX_EMPLOYEES_NUM + 1);

        workforce.employees = generateRandomEmployeesWithUniqueFirstNames(workersNum);

        return workforce;
    }

    private List<Employee> generateRandomEmployeesWithUniqueFirstNames(int employeesNum) {
        ArrayList<String> names = sampleSubsetUniformly(
                new CommonHitGenerationUtils(settingsCommonForMultipleDomains).getManyFirstNamesSortedByDescendingOccurrences(), employeesNum);
        ArrayList<Employee> result = new ArrayList<>();
        for (int i=0; i<employeesNum; i++) {
            /*  The manager the current employee we create may be any of the managers created so far (only the first
                employee created has no manager).*/
            @Nullable Employee manager = i==0 ? null : result.get(sampleUniformly(0, i));
            Position position = i==0 ? Position.MANAGER : sampleUniformly(Position.class);
            int salary = getRandomSalary();
            result.add(new Employee(names.get(i), position, manager, salary));
        }
        return result;
    }

    private int getRandomSalary() {
        final int MIN_SALARY = 40000;
        final int MAX_SALARY = 100000;
        return sampleUniformly(MIN_SALARY, MAX_SALARY+1);
    }


    @Nullable
    @Override
    protected MethodCall generateRandomFunctionCall(State initialState, StateVisualizer initialStateVisualizer,
                                                    StateVisualizer desiredStateVisualizer) {

        WorkforceManagementStateVisualizer initialSV = (WorkforceManagementStateVisualizer) initialStateVisualizer;
        WorkforceManagementStateVisualizer desiredSV = (WorkforceManagementStateVisualizer) desiredStateVisualizer;

        switch (nliMethod) {
            case ASSIGN_EMPLOYEES_TO_NEW_MANAGER:
                return generateRandomAssignEmployeesToNewManagerFunctionCall(initialState,  initialSV, desiredSV);
            case FIRE_EMPLOYEES:
                return generateRandomFireEmployeesFunctionCall(initialState,  initialSV);
            case REASSIGN_EMPLOYEE_TO_NEW_POSITION:
                return generateRandomAssignEmployeeToNewPositionFunctionCall(initialState,  initialSV, desiredSV);
            case UPDATE_SALARY:
                return generateRandomUpdateSalaryFunctionCall(initialState, initialSV, desiredSV);
        }
        throw new Error();
    }

    private @Nullable
    MethodCall generateRandomAssignEmployeeToNewPositionFunctionCall(
            State initialState, WorkforceManagementStateVisualizer initialSV,
            WorkforceManagementStateVisualizer desiredSV) {

        WorkforceManagementSystem workforce = (WorkforceManagementSystem) initialState.getRootEntity();

        Employee employee = sampleUniformly(new ArrayList<>(workforce.employees));
        if (employee.manager == null)
            return null; // we don't want to change the position of the "root manager".
        Position newPosition = sampleUniformly(Position.class);

        String employeeId = initialState.getEntityId(employee);

        initialSV.emphasizePosition(employeeId);
        desiredSV.emphasizePosition(employeeId);

        return new MethodCall(nliMethod.getMethodId(), initialState.getEntityId(workforce),
                new NonPrimitiveArgument(employeeId),
                new PrimitiveArgument(newPosition));
    }

    private  @Nullable
    MethodCall generateRandomAssignEmployeesToNewManagerFunctionCall(
            State initialState, WorkforceManagementStateVisualizer initialSV,
            WorkforceManagementStateVisualizer desiredSV) {

        final int MAX_EMPLOYEES_TO_BE_ASSIGNED = 3;

        WorkforceManagementSystem workforce = (WorkforceManagementSystem) initialState.getRootEntity();

        ArrayList<Employee> managers = workforce.employees.stream()
                .filter(e -> e.position == Position.MANAGER)
                .collect(Collectors.toCollection(ArrayList::new));
        Employee managerToAssignTo = sampleUniformly(managers);
        // candidates are the employees not under 'managerToAssignTo', and not the "root" manager.
        SafeSet<Employee> candidatesToBeAssigned = new SafeHashSet<>(workforce.employees);
        candidatesToBeAssigned.safeRemoveAll(workforce.getEmployeesRecursivelyUnderManager(managerToAssignTo));
        // we don't want to create cycles:
        candidatesToBeAssigned.safeRemoveAll(getAllEmployeesInChainOfCommandFromGivenOneToRootManager(managerToAssignTo));
        if (candidatesToBeAssigned.size() <= 1)
            return null;
        int numOfEmployeesToBeAssigned = sampleUniformly(1, Math.min(candidatesToBeAssigned.size(),
                MAX_EMPLOYEES_TO_BE_ASSIGNED));
        List<Employee> employeesToBeAssigned = sampleSubsetUniformly(candidatesToBeAssigned,
                numOfEmployeesToBeAssigned);

        List<String> idsOfEmployeesToBeAssigned =  initialState.getEntityIds(employeesToBeAssigned);

        idsOfEmployeesToBeAssigned.forEach(id->{
            initialSV.emphasizeManager(id);
            desiredSV.emphasizeManager(id);
        });

        return new MethodCall(nliMethod.getMethodId(), initialState.getEntityId(workforce),
                new NonPrimitiveArgument(idsOfEmployeesToBeAssigned),
                new NonPrimitiveArgument(initialState.getEntityId(managerToAssignTo)));

    }

    private MethodCall generateRandomUpdateSalaryFunctionCall(State initialState,
                                                              WorkforceManagementStateVisualizer initialSV,
                                                              WorkforceManagementStateVisualizer desiredSV) {
        WorkforceManagementSystem workforce = (WorkforceManagementSystem) initialState.getRootEntity();

        Employee employee = sampleUniformly(new ArrayList<>(workforce.employees));
        int salary = getRandomSalary();

        String employeeId = initialState.getEntityId(employee);

        initialSV.emphasizeSalary(employeeId);
        desiredSV.emphasizeSalary(employeeId);

        return new MethodCall(nliMethod.getMethodId(), initialState.getEntityId(workforce),
                new NonPrimitiveArgument(employeeId),
                new PrimitiveArgument(salary));
    }

    /**
     * Firing an employee along with all employees under him/her recursively.
     */
    private @Nullable
    MethodCall generateRandomFireEmployeesFunctionCall(
            State initialState, WorkforceManagementStateVisualizer initialSV) {

        WorkforceManagementSystem workforce = (WorkforceManagementSystem) initialState.getRootEntity();

        Employee employee = sampleUniformly(new ArrayList<>(workforce.employees));
        if (employee.manager == null)
            return null; // we don't want to fire the "root" manager.
        List<Employee> employeesToFire = workforce.getEmployeesRecursivelyUnderManager(employee);
        employeesToFire.add(employee);


        List<String> idsOfEmployeesToFire = initialState.getEntityIds(employeesToFire);
        idsOfEmployeesToFire.forEach(initialSV::emphasizeEntireEntity);

        return new MethodCall(nliMethod.getMethodId(), initialState.getEntityId(workforce),
                new NonPrimitiveArgument(idsOfEmployeesToFire));
    }

    public Collection<Employee> getAllEmployeesInChainOfCommandFromGivenOneToRootManager(Employee e) {
        List<Employee> result = new LinkedList<>();
        while(true) {
            result.add(e);
            if (e.manager == null)
                return result;
            e = e.manager;
        }
    }



}
