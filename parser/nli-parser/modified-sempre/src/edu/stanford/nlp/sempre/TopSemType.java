//NOTICE: this file was modified by Ofer Givoli (i.e. it's not identical to the matching file in the original Sempre package).
package edu.stanford.nlp.sempre;

import fig.basic.LispTree;

// Represents any possible value.
public class TopSemType extends SemType {
  public boolean isValid() { return true; }
  public SemType meet(SemType that) { return that; }
  public SemType apply(SemType that) { return this; }
  public SemType reverse() { return this; }
  public LispTree toLispTree() { return LispTree.proto.newLeaf("top"); }

  /**
   * Added. --Ofer Givoli
   */
  @Override
  public boolean equals(Object obj) {
    return obj != null && getClass() == obj.getClass();
  }

  /**
   * Added. --Ofer Givoli
   */
  @Override
  public int hashCode() {
    return 0;
  }
}
