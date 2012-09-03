package at.logic.transformations.ceres.clauseSchema

import at.logic.calculi.lk.base.FSequent
import at.logic.calculi.lk.propositionalRules.{Axiom, NegLeftRule}
import at.logic.calculi.occurrences.{FormulaOccurrence, defaultFormulaOccurrenceFactory}
import at.logic.language.hol.logicSymbols.ConstantStringSymbol
import at.logic.language.lambda.symbols.VariableStringSymbol
import at.logic.language.lambda.typedLambdaCalculus.Var
import at.logic.algorithms.shlk._
import java.io.File.separator
import scala.io._
import org.specs2.mutable._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.execute.Success
import at.logic.language.lambda.types._
import at.logic.language.hol._

@RunWith(classOf[JUnitRunner])
class clauseSchemaTest extends SpecificationWithJUnit {
  implicit val factory = defaultFormulaOccurrenceFactory
  import at.logic.language.schema._
  "clauseSchemaTest" should {
    "create a correct schema clause" in {
      println(Console.RED+"\n\n       clauseSchemaTest\n\n"+Console.RESET)
      val k = IntVar(new VariableStringSymbol("k"))
      val n1 = Succ(k); val n2 = Succ(n1); val n3 = Succ(n2)
      val k1 = Succ(k); val k2 = Succ(n1); val k3 = Succ(n2)
      val zero = IntZero(); val one = Succ(IntZero()); val two = Succ(Succ(IntZero())); val three = Succ(Succ(Succ(IntZero())))
      val four = Succ(three);val five = Succ(four); val six = Succ(Succ(four));val seven = Succ(Succ(five));       val A0 = IndexedPredicate(new ConstantStringSymbol("A"), IntZero())

      val Pk = IndexedPredicate(new ConstantStringSymbol("P"), k)
      val P0 = IndexedPredicate(new ConstantStringSymbol("P"), IntZero())
      val Q0 = IndexedPredicate(new ConstantStringSymbol("Q"), IntZero())
      val Pk1 = IndexedPredicate(new ConstantStringSymbol("P"), Succ(k))
      val c0 = nonVarSclause(List.empty[HOLFormula], P0::Nil)
      val ck1 = nonVarSclause(List.empty[HOLFormula], Pk1::Nil)
      val X = sClauseVar("X")
      val base: sClause = sClauseComposition(c0, X)
      val rec: sClause = sClauseComposition(ck1, X)
      val clause_schema = ClauseSchemaPair(base, rec)
      println(base)
      println(rec)
      val non = nonVarSclause(Q0::Nil, List.empty[HOLFormula])
      val map = Map[sClauseVar, sClause]() + Pair(X.asInstanceOf[sClauseVar], non)
      val l = Ti()::To()::Tindex()::Nil
      l.foldLeft(To().asInstanceOf[TA])((x,t) => ->(x, t))
      println("l = "+l)
//      val rez = clause_schema(two, map)
//      println("\nrez = "+rez)
      println("\n\n\n")
//      println(normalizeSClause(sClauseComposition(rez, X)))
//      println(Pred(two))
      println("\n\n\n\n")
      ok
    }


    "create a fist-order schema clause" in {
      println(Console.RED+"\n\n       clauseSchemaTest\n\n"+Console.RESET)
      val k = IntVar(new VariableStringSymbol("k"))
      val l = IntVar(new VariableStringSymbol("l"))
      val n1 = Succ(k); val n2 = Succ(n1); val n3 = Succ(n2)

      val zero = IntZero(); val one = Succ(IntZero()); val two = Succ(Succ(IntZero())); val three = Succ(Succ(Succ(IntZero())))
      val four = Succ(three);val five = Succ(four); val six = Succ(Succ(four));val seven = Succ(Succ(five));       val A0 = IndexedPredicate(new ConstantStringSymbol("A"), IntZero())

      val Pk1 = IndexedPredicate(new ConstantStringSymbol("P"), Succ(k))
      val X = sClauseVar("X")
      val x = HOLVar(new VariableStringSymbol("x"), ->(Tindex(), Ti()))
      val P = HOLConst(new ConstantStringSymbol("P"), ->(Ti(), To()))
      val g = HOLVar(new VariableStringSymbol("g"), ->(Ti(),Ti()))
      val sigma0x0 = sTermN("σ", zero::x::zero::Nil)
      val sigmaskxsk = sTermN("σ", Succ(k)::x::Succ(k)::Nil)
      val Psigma0x0 = Atom(P, sigma0x0::Nil)
      val Psigmaskxsk = Atom(P, sigmaskxsk::Nil)

      // --- trs sigma ---
      val sigma_base = sTermN("σ", zero::x::l::Nil)
      val sigma_rec = sTermN("σ", Succ(k)::x::l::Nil)
      val st = sTermN("σ", k::x::l::Nil)
      val rewrite_base = HOLApp(x, l)
      val rewrite_step = HOLApp(g, st)
      val trsSigma = dbTRSsTermN("σ", Pair(sigma_base, rewrite_base), Pair(sigma_rec, rewrite_step))

      // --- trs clause schema ---
      val c1 = clauseSchema("c", k::x::X::Nil)
      val ck = clauseSchema("c", Succ(k)::x::X::Nil)
      val c0 = clauseSchema("c", zero::x::X::Nil)
      val clauseSchBase: sClause = sClauseComposition(X, nonVarSclause(Nil, Psigma0x0::Nil))
      val clauseSchRec: sClause = sClauseComposition(c1, nonVarSclause(Nil, Psigmaskxsk::Nil))
      val trsClauseSch = dbTRSclauseSchema("c", Pair(c0, clauseSchBase), Pair(ck, clauseSchRec))
      // ----------

      val map = Map[Var, HOLExpression]() + Pair(k.asInstanceOf[Var], two.asInstanceOf[HOLExpression])
      val subst = new SchemaSubstitution3(map)

      val sig = subst(trsSigma.map.get("σ").get._2._1)
      println("sig = "+sig)
      val sigma3 = unfoldSTermN(sig, trsSigma, subst)
      println("\n\nsigma3 = "+sigma3)
//        println("unfold : "+unfoldSTermN())
//      println("\n"+applySubToSclause(subst, trsClauseSch.map.get("c").get._2._2))

      //      println(normalizeSClause(sClauseComposition(rez, X)))
      println("\n\n\n\n")
      ok
    }
  }
}

