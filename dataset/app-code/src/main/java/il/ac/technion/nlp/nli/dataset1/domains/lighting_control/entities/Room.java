package il.ac.technion.nlp.nli.dataset1.domains.lighting_control.entities;

import il.ac.technion.nlp.nli.core.state.NliEntity;
import org.jetbrains.annotations.NotNull;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class Room implements NliEntity {

    private static final long serialVersionUID = -8413616707384188828L;

    public String roomName;

    public LightMode lightMode;

    /**
     * Always positive.
     */
    public int floor;

    public LightMode getLightMode() {
        return lightMode;
    }


    public Room(@NotNull String roomName, @NotNull LightMode lightMode, int floor) {
        this.roomName = roomName;
        this.lightMode = lightMode;
        this.floor = floor;
    }

}
