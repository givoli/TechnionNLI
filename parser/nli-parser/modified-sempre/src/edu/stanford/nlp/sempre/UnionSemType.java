//NOTICE: this file was modified by Ofer Givoli (i.e. it's not identical to the matching file in the original Sempre package).
package edu.stanford.nlp.sempre;

import fig.basic.LispTree;
import java.util.*;
import java.util.stream.Collectors;

// Represents the union of a set of base types.
public class UnionSemType extends SemType {
  public final List<SemType> baseTypes;
  public boolean isValid() { return baseTypes.size() > 0; }

  // Constructors
  public UnionSemType() {
    this.baseTypes = new ArrayList<SemType>();
  }
  public UnionSemType(SemType... baseTypes) {
    this.baseTypes = new ArrayList<SemType>();
    for (SemType baseType : baseTypes)
      if (baseType.isValid())
        this.baseTypes.add(baseType);
  }
  public UnionSemType(Collection<SemType> baseTypes) {
    this.baseTypes = new ArrayList<SemType>();
    for (SemType baseType : baseTypes)
      if (baseType.isValid())
        this.baseTypes.add(baseType);
  }

  public SemType meet(SemType that) {
    if (that instanceof TopSemType) return this;
    List<SemType> result = new ArrayList<>();
    for (SemType baseType : baseTypes)
      result.add(baseType.meet(that));
    return new UnionSemType(result).simplify();
  }

  public SemType apply(SemType that) {
    List<SemType> result = new ArrayList<>();
    for (SemType baseType : baseTypes)
      result.add(baseType.apply(that));
    return new UnionSemType(result).simplify();
  }

  public SemType reverse() {
    List<SemType> result = new ArrayList<>();
    for (SemType baseType : baseTypes)
      result.add(baseType.reverse());
    return new UnionSemType(result).simplify();
  }

  public LispTree toLispTree() {
    LispTree result = LispTree.proto.newList();
    result.addChild("union");
    for (SemType baseType : baseTypes)
      result.addChild(baseType.toLispTree());
    return result;
  }

  public SemType simplify() {
    if (baseTypes.size() == 0) return SemType.bottomType;
    if (baseTypes.size() == 1) return baseTypes.get(0);
    if (baseTypes.contains(SemType.topType)) return SemType.topType;
    return this;
  }

  /**
   * Added. --Ofer Givoli
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    UnionSemType that = (UnionSemType) o;
    return Objects.equals(getSortedBaseTypes(), that.getSortedBaseTypes());
  }

  /**
   * Added. --Ofer Givoli
   */
  @Override
  public int hashCode() {

    return Objects.hash(getSortedBaseTypes());
  }

  /**
   * Added. --Ofer Givoli
   */
  private List<SemType> getSortedBaseTypes(){
    return baseTypes.stream().sorted(Comparator.comparing(x->x.toLispTree().toString())).collect(Collectors.toList());
  }
}
