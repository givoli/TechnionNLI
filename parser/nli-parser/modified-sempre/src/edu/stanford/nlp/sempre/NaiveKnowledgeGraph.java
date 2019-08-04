//NOTICE: this file was modified by Ofer Givoli (i.e. it's not identical to the matching file in the original Sempre package).
package edu.stanford.nlp.sempre;

import fig.basic.LispTree;
import fig.basic.MapUtils;
import fig.basic.Pair;

import java.util.*;

/**
 * Represent a knowledge graph explicitly as triples (e1, r, e2).
 *
 * The graph is immutable. Once the graph is initialized, we precompute several mappings
 * (e.g., list of all outgoing edges from each entity e).
 *
 * @author ppasupat
 */
public class NaiveKnowledgeGraph extends KnowledgeGraph {

  // Represent a triple (entity, relation, entity)
  public static class KnowledgeGraphTriple {
    public final Value e1, r, e2;

    public KnowledgeGraphTriple(Value e1, Value r, Value e2) {
      this.e1 = e1;
      this.r = r;
      this.e2 = e2;
    }

    public KnowledgeGraphTriple(String e1, String r, String e2) {
      this.e1 = new StringValue(e1);
      this.r = new StringValue(r);
      this.e2 = new StringValue(e2);
    }

    public KnowledgeGraphTriple(LispTree tree) {
      if (tree.children.size() != 3)
        throw new RuntimeException("Invalid triple size (" + tree.children.size() + " != 3): "
            + tree);
      this.e1 = valueFromLispTree(tree.child(0));
      this.r = valueFromLispTree(tree.child(1));
      this.e2 = valueFromLispTree(tree.child(2));
    }

    protected static Value valueFromLispTree(LispTree tree) {
      if (tree.isLeaf()) return new NameValue(tree.value, null);
      return Values.fromLispTree(tree);
    }

    public LispTree toLispTree() {
      LispTree tree = LispTree.proto.newList();
      tree.addChild(e1.toLispTree());
      tree.addChild(r.toLispTree());
      tree.addChild(e2.toLispTree());
      return tree;
    }

    @Override
    public String toString() {
      return "<" + e1 + ", " + r + ", " + e2 + ">";
    }
  }

  // Simplest graph representation: triples of values
  public final List<KnowledgeGraphTriple> triples;

  // ============================================================
  // Constructor / Precomputation
  // ============================================================

  public Map<Value, List<KnowledgeGraphTriple>> relationToTriples;
  public Map<Value, List<KnowledgeGraphTriple>> firstToTriples;
  public Map<Value, List<KnowledgeGraphTriple>> secondToTriples;

  public NaiveKnowledgeGraph(Collection<KnowledgeGraphTriple> triples) {
    this.triples = new ArrayList<>(triples);
    precomputeMappings();
  }

  public void precomputeMappings() {
    relationToTriples = new HashMap<>();
    firstToTriples = new HashMap<>();
    secondToTriples = new HashMap<>();
    for (KnowledgeGraphTriple triple : triples) {
      MapUtils.addToList(relationToTriples, triple.r, triple);
      MapUtils.addToList(firstToTriples, triple.e1, triple);
      MapUtils.addToList(secondToTriples, triple.e2, triple);
    }
  }

  // Changed return type from Collection to List (see note in the abstract method). --Ofer Givoli
  @Override
  public LinkedList<Formula> getFuzzyMatchedFormulas(String term, FuzzyMatchFn.FuzzyMatchFnMode mode) {
    throw new RuntimeException("Not implemented yet");
  }

  // Changed return type from Collection to List (see note in the abstract method). --Ofer Givoli
  @Override
  public LinkedList<Formula> getAllFormulas(FuzzyMatchFn.FuzzyMatchFnMode mode) {
    throw new RuntimeException("Not implemented yet");
  }

  // ============================================================
  // Queries
  // ============================================================

  @Override
  public List<Value> joinFirst(Value r, Collection<Value> firsts) {
    Value reversed = isReversedRelation(r);
    if (reversed != null)
      return joinSecond(reversed, firsts);
    List<Value> seconds = new ArrayList<>();
    List<KnowledgeGraphTriple> relationTriples = relationToTriples.get(r);
    if (relationTriples != null) {
      for (KnowledgeGraphTriple triple : relationTriples) {
        if (firsts.contains(triple.e1))
          seconds.add(triple.e2);
      }
    }
    return seconds;
  }

  @Override
  public List<Value> joinSecond(Value r, Collection<Value> seconds) {
    Value reversed = isReversedRelation(r);
    if (reversed != null)
      return joinFirst(reversed, seconds);
    List<Value> firsts = new ArrayList<>();
    List<KnowledgeGraphTriple> relationTriples = relationToTriples.get(r);
    if (relationTriples != null) {
      for (KnowledgeGraphTriple triple : relationTriples) {
        if (seconds.contains(triple.e2))
          firsts.add(triple.e1);
      }
    }
    return firsts;
  }

  @Override
  public List<Pair<Value, Value>> filterFirst(Value r, Collection<Value> firsts) {
    Value reversed = isReversedRelation(r);
    if (reversed != null)
      return getReversedPairs(filterSecond(reversed, firsts));
    List<Pair<Value, Value>> pairs = new ArrayList<>();
    List<KnowledgeGraphTriple> relationTriples = relationToTriples.get(r);
    if (relationTriples != null) {
      for (KnowledgeGraphTriple triple : relationTriples) {
        if (firsts.contains(triple.e1))
          pairs.add(new Pair<>(triple.e1, triple.e2));
      }
    }
    return pairs;
  }

  @Override
  public List<Pair<Value, Value>> filterSecond(Value r, Collection<Value> seconds) {
    Value reversed = isReversedRelation(r);
    if (reversed != null)
      return getReversedPairs(filterFirst(reversed, seconds));
    List<Pair<Value, Value>> pairs = new ArrayList<>();
    List<KnowledgeGraphTriple> relationTriples = relationToTriples.get(r);
    if (relationTriples != null) {
      for (KnowledgeGraphTriple triple : relationTriples) {
        if (seconds.contains(triple.e2))
          pairs.add(new Pair<>(triple.e1, triple.e2));
      }
    }
    return pairs;
  }

  // ============================================================
  // LispTree conversion
  // ============================================================

  /**
   * Convert LispTree to KnowledgeGraph
   *
   * The |tree| should look like
   *
   * (graph NaiveKnowledgeGraph
   *        ((string Obama) (string "born in") (string Hawaii))
   *        ((string Einstein) (string "born in") (string Ulm))
   *        ...)
   */
  public static KnowledgeGraph fromLispTree(LispTree tree) {
    List<KnowledgeGraphTriple> triples = new ArrayList<>();
    for (int i = 2; i < tree.children.size(); i++) {
      triples.add(new KnowledgeGraphTriple(tree.child(i)));
    }
    return new NaiveKnowledgeGraph(triples);
  }

  @Override
  public LispTree toLispTree() {
    LispTree tree = LispTree.proto.newList();
    tree.addChild("graph");
    tree.addChild("NaiveKnowledgeGraph");
    for (KnowledgeGraphTriple triple : triples) {
      tree.addChild(triple.toLispTree());
    }
    return tree;
  }
}
