package il.ac.technion.nlp.nli.dataset1.domains.container_management.hit;

import il.ac.technion.nlp.nli.dataset1.domains.container_management.entities.ContainerManagementSystem;
import il.ac.technion.nlp.nli.core.method_call.MethodId;
import il.ac.technion.nlp.nli.core.dataset.NliMethod;

import java.util.Collection;

public enum ContainerManagementNliMethod implements NliMethod {

    LOAD,
    UNLOAD,
    REMOVE;


    @Override
    public MethodId getMethodId() {
        switch (this) {
            case LOAD:
                return new MethodId(ContainerManagementSystem.class, "loadContainers", Collection.class);
            case UNLOAD:
                return new MethodId(ContainerManagementSystem.class, "unloadContainers", Collection.class);
            case REMOVE:
                return new MethodId(ContainerManagementSystem.class, "removeContainers", Collection.class);
        }
        throw new Error();
    }

}
