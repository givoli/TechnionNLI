//NOTICE: this file was modified by Ofer Givoli (i.e. it's not identical to the matching file in the original Sempre package).
package edu.stanford.nlp.sempre;

import il.ac.technion.nlp.nli.parser.lexicon.SingleDerivationStreamThatAddsToPhraseAssociation;

/**
 * Maps a string to a Date.
 *
 * @author Percy Liang
 */
public class DateFn extends SemanticFn {
  public DerivationStream call(final Example ex, final Callable c) {
    // Modified the next line. --Ofer Givoli
    return new SingleDerivationStreamThatAddsToPhraseAssociation(ex.languageInfo.phrase(c.getStart(), c.getEnd())) {
      @Override
      public Derivation createDerivation() {
        // modified this method to support derivations for DateValue objects where only the day field is known. --Ofer Givoli
        DateValue dateValue = null;
        String value = ex.languageInfo.getNormalizedNerSpan("DATE", c.getStart(), c.getEnd());
        if (value != null) {
          dateValue = DateValue.parseDateValue(value);
        } else {
          value = ex.languageInfo.getNormalizedNerSpan("ORDINAL", c.getStart(), c.getEnd());
          if (value != null) {
            // parsing the phrase assuming it refers to a day-of-month (e.g. "4th").
            try {
              dateValue = new DateValue(-1, -1, Integer.parseInt(value));
            } catch (NumberFormatException e){
              return null;
            }
          }
        }
        if (dateValue == null)
          return null;
        return new Derivation.Builder()
                .withCallable(c)
                .formula(new ValueFormula<>(dateValue))
                .type(SemType.dateType)
                .createDerivation();
      }
    };
  }
}
