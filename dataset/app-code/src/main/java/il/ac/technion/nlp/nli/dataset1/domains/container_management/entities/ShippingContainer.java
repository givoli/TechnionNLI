package il.ac.technion.nlp.nli.dataset1.domains.container_management.entities;

import il.ac.technion.nlp.nli.core.state.NliEntity;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class ShippingContainer implements NliEntity {

    private static final long serialVersionUID = -3703058121389969116L;

    public int length;
    public ContentState contentState;

    public ShippingContainer(int length, ContentState contentState) {
        this.length = length;
        this.contentState = contentState;
    }
}
