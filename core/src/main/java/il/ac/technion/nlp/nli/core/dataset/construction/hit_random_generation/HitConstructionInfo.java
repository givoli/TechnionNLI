package il.ac.technion.nlp.nli.core.dataset.construction.hit_random_generation;

import il.ac.technion.nlp.nli.core.method_call.MethodId;

import java.io.Serializable;
import java.time.ZonedDateTime;

/**
 * Settings used during constructing and filtering a specific HIT, and some additional info for organization sake.
 * The train/test split should be stratified over some of these settings.
 *
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class HitConstructionInfo implements Serializable {

    private static final long serialVersionUID = -5890528538978151614L;

    public final boolean someArgQueryableByComparison;
    public final boolean someArgQueryableBySuperlative;
    public final boolean queryableByPrimitiveRelations;
    public final MethodId nliMethod;

    /**
     * Set by the constructor of this class (to the time of the construction of this object).
     * Make sure to commit in git just before creating HITs, so that this field could be useful.
     */
    public final ZonedDateTime creationTime;


    public HitConstructionInfo(boolean someArgQueryableByComparison, boolean someArgQueryableBySuperlative,
                               boolean queryableByPrimitiveRelations, MethodId nliMethod, ZonedDateTime creationTime) {


        this.someArgQueryableByComparison = someArgQueryableByComparison;
        this.someArgQueryableBySuperlative = someArgQueryableBySuperlative;
        this.queryableByPrimitiveRelations = queryableByPrimitiveRelations;
        this.nliMethod = nliMethod;

        this.creationTime = creationTime;
    }


}
