//NOTICE: this file was modified by Ofer Givoli (i.e. it's not identical to the matching file in the original Sempre package).
package edu.stanford.nlp.sempre;

import fig.basic.LispTree;

import java.util.Objects;

/**
 * FuncSemType really is used to represent a pair type (t1, t2) (despite its name).
 * The lisp tree representation is (-> retType argType).
 */
public class FuncSemType extends SemType {
  public final SemType argType;
  public final SemType retType;
  public FuncSemType(SemType argType, SemType retType) {
    if (argType == null) throw new RuntimeException("Null argType");
    if (retType == null) throw new RuntimeException("Null retType");
    this.argType = argType;
    this.retType = retType;
  }
  public FuncSemType(String argType, String retType) {
    this(new AtomicSemType(argType), new AtomicSemType(retType));
  }
  public boolean isValid() { return true; }

  public SemType meet(SemType that) {
    if (that instanceof TopSemType) return this;
    if (!(that instanceof FuncSemType)) return SemType.bottomType;
    // Perform the meet elementwise (remember, treat this as a pair type).
    FuncSemType thatFunc = (FuncSemType) that;
    SemType newArgType = argType.meet(thatFunc.argType);
    if (!newArgType.isValid()) return SemType.bottomType;
    SemType newRetType = retType.meet(thatFunc.retType);
    if (!newRetType.isValid()) return SemType.bottomType;
    return new FuncSemType(newArgType, newRetType);
  }

  public SemType apply(SemType that) {
    if (argType.meet(that).isValid()) return retType;
    return SemType.bottomType;
  }
  public FuncSemType reverse() { return new FuncSemType(retType, argType); }
  public LispTree toLispTree() {
    LispTree tree = LispTree.proto.newList();
    tree.addChild("->");
    tree.addChild(argType.toLispTree());
    tree.addChild(retType.toLispTree());
    return tree;
  }

  /**
   * Added. --Ofer Givoli
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    FuncSemType that = (FuncSemType) o;
    return Objects.equals(argType, that.argType) &&
            Objects.equals(retType, that.retType);
  }

  /**
   * Added. --Ofer Givoli
   */
  @Override
  public int hashCode() {

    return Objects.hash(argType, retType);
  }
}
