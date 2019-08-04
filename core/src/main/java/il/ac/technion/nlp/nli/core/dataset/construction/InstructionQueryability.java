package il.ac.technion.nlp.nli.core.dataset.construction;

import il.ac.technion.nlp.nli.core.dataset.construction.hit_random_generation.HitConstructionInfo;

/**
 * Defines the category of the instruction in terms of possible ways to query for the matching arguments of the NLI
 * function.
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public enum InstructionQueryability {
    NOT_QUERYABLE_BY_PRIMITIVE_REL_AND_NO_ARG_QUERYABLE_BY_SUPERLATIVE_OR_COMPARISON,
    QUERYABLE_BY_PRIMITIVE_REL_AND_ARG_QUERYABLE_BY_SUPERLATIVE,
    QUERYABLE_BY_PRIMITIVE_REL_AND_ARG_QUERYABLE_BY_COMPARISON_AND_NO_ARG_QUERYABLE_BY_SUPERLATIVE,
    QUERYABLE_BY_PRIMITIVE_REL_AND_NO_ARG_QUERYABLE_BY_COMPARISON_OR_SUPERLATIVE,
    NOT_QUERYABLE_BY_PRIMITIVE_REL_AND_OTHER_METHODS,
    OTHER;

    public static InstructionQueryability getByHitConstructionInfo(HitConstructionInfo info) {

        if (info.queryableByPrimitiveRelations) {
            if (info.someArgQueryableBySuperlative) {
                // we don't care in this case about info.someArgQueryableByComparison (either way is ok).
                return QUERYABLE_BY_PRIMITIVE_REL_AND_ARG_QUERYABLE_BY_SUPERLATIVE;
            } else {
                return info.someArgQueryableByComparison ?
                        QUERYABLE_BY_PRIMITIVE_REL_AND_ARG_QUERYABLE_BY_COMPARISON_AND_NO_ARG_QUERYABLE_BY_SUPERLATIVE :
                        QUERYABLE_BY_PRIMITIVE_REL_AND_NO_ARG_QUERYABLE_BY_COMPARISON_OR_SUPERLATIVE;
            }
        } else {
            // not queryableByPrimitiveRelations
            if (!info.someArgQueryableBySuperlative && !info.someArgQueryableByComparison)
                return NOT_QUERYABLE_BY_PRIMITIVE_REL_AND_NO_ARG_QUERYABLE_BY_SUPERLATIVE_OR_COMPARISON;
        }

        return OTHER;
    }

}
