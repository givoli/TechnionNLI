//NOTICE: this file was modified by Ofer Givoli (i.e. it's not identical to the matching file in the original Sempre package).
package edu.stanford.nlp.sempre.tables.lambdadcs;

import edu.stanford.nlp.sempre.*;
import fig.basic.Evaluation;
import fig.basic.LogInfo;
import fig.basic.Option;
import fig.basic.StopWatch;

/**
 * Execute a Formula on the given KnowledgeGraph instance.
 *
 * @author ppasupat
 */
public class LambdaDCSExecutor extends Executor {
  public static class Options {
    @Option(gloss = "Verbosity") public int verbose = 0;
    @Option(gloss = "If the result is empty, return an ErrorValue instead of an empty ListValue")
    public boolean failOnEmptyLists = false;
    @Option(gloss = "Return all ties on (argmax 1 1 ...) and (argmin 1 1 ...)")
    public boolean superlativesReturnAllTopTies = true;
    @Option(gloss = "Aggregates (sum, avg, max, min) throw an error on empty lists")
    public boolean aggregatesFailOnEmptyLists = false;
    @Option(gloss = "Superlatives (argmax, argmin) throw an error on empty lists")
    public boolean superlativesFailOnEmptyLists = false;
    @Option(gloss = "Arithmetics (+, -, *, /) throw an error when both operants have > 1 values")
    public boolean arithmeticsFailOnMultipleElements = false;
    @Option(gloss = "Use caching")
    public boolean useCache = true;
  }
  public static Options opts = new Options();

  public final Evaluation stats = new Evaluation();

  @Override
  public Response execute(Formula formula, ContextValue context) {
    LambdaDCSCoreLogic logic;
    if (opts.verbose < 3) {
      logic = new LambdaDCSCoreLogic(context, stats);
    } else {
      logic = new LambdaDCSCoreLogicWithVerbosity(context, stats);
    }
    StopWatch stopWatch = new StopWatch();
    stopWatch.start();
    Value answer = logic.execute(formula);
    stopWatch.stop();
    stats.addCumulative("execTime", stopWatch.ms);
    if (stopWatch.ms >= 10 && opts.verbose >= 1)
      LogInfo.logs("long time: %s %s", Formulas.betaReduction(formula), answer);
    /*///////// DEBUG! //////////
    if (answer instanceof ErrorValue) {
      if (!((ErrorValue) answer).type.startsWith("notUnary"))
        LogInfo.logs("%s %s", Formulas.betaReduction(formula), answer);
    }
    ////////// END DEBUG /////////*/
    return new Response(answer);
  }

  public void summarize() {
    LogInfo.begin_track("LambdaDCSExecutor: summarize");
    stats.logStats("LambdaDCSExecutor");
    LogInfo.end_track();
  }
}

// ============================================================
// Execution
// ============================================================

// Moved the LambdaDCSCoreLogic class to its own java file (so it could be public). --Ofer Givoli

// ============================================================
// Debug Print
// ============================================================

class LambdaDCSCoreLogicWithVerbosity extends LambdaDCSCoreLogic {

  public LambdaDCSCoreLogicWithVerbosity(ContextValue context, Evaluation stats) {
    super(context, stats);
  }

  @Override
  public UnaryDenotation computeUnary(Formula formula, UnaryTypeHint typeHint) {
    LogInfo.begin_track("UNARY %s [%s]", formula, typeHint);
    try {
      UnaryDenotation denotation = super.computeUnary(formula, typeHint);
      LogInfo.logs("%s", denotation);
      LogInfo.end_track();
      return denotation;
    } catch (Exception e) {
      LogInfo.end_track();
      throw e;
    }
  }

  @Override
  public BinaryDenotation computeBinary(Formula formula, BinaryTypeHint typeHint) {
    LogInfo.begin_track("BINARY %s [%s]", formula, typeHint);
    try {
      BinaryDenotation denotation = super.computeBinary(formula, typeHint);
      LogInfo.logs("%s", denotation);
      LogInfo.end_track();
      return denotation;
    } catch (Exception e) {
      LogInfo.end_track();
      throw e;
    }
  }

}
