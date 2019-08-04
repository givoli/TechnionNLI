package il.ac.technion.nlp.nli.core.dataset;

import il.ac.technion.nlp.nli.core.dataset.construction.Hit;
import il.ac.technion.nlp.nli.core.state.State;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class Example implements Serializable{
	private static final long serialVersionUID = -7652387838447774927L;
	
	private final String id; // Used for equals() and hashCode().
	private String instructionUtterance;
	private final State initialState;
	private final State destinationState;
	private final Domain domain;

	/**
	 * @param id - used for equals() and hashCode().
	 */
	public Example(String id, String instruction, State initialState, State destinationState,
				   Domain domain) {
		this.id = id;
		this.instructionUtterance = instruction;
		this.initialState = initialState;
		this.destinationState = destinationState;
		this.domain = domain;
	}


    /**
     * Like {@link #Example(String, String, State, State, Domain)}, but the id used is randomly generated via
     * {@link UUID}.
     */
    public Example(Hit hit, String instruction) {
        this(UUID.randomUUID().toString(), instruction,
                hit.getInitialState(), hit.getDestinationState(), hit.getDomain());
    }


    /**
     * Based on {@link #id}.
     */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Example example = (Example) o;
		return Objects.equals(id, example.id);
	}

	/**
	 * Based on {@link #id}.
	 */
	@Override
	public int hashCode() {

		return Objects.hash(id);
	}

	public String getId() {
		return id;
	}

	public String getInstructionUtterance() {
		return instructionUtterance;
	}

	public void setInstructionUtterance(String instructionUtterance) {
		this.instructionUtterance = instructionUtterance;
	}

	public State getInitialState() {
		return initialState;
	}


	public Domain getDomain() {
		return domain;
	}

	public State getDestinationState() {
		return destinationState;
	}

	public boolean isMultiSentenceInstruction() {
		int fromIndex = 0;
		int dotInd;
		while ((dotInd = instructionUtterance.indexOf(".", fromIndex)) >= 0){

			if (!instructionUtterance.substring(dotInd + 1).trim().isEmpty() && // The dot isn't the end of the utterance.
					StringUtils.isBlank(instructionUtterance.substring(dotInd + 1,dotInd + 2))) // the character after the dot is a whitespace.
				return true;
			fromIndex =dotInd+1;
		}
		return false;
	}

	@Override
	public String toString() {
		return id;
	}

}
