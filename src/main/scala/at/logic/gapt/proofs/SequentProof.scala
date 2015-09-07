package at.logic.gapt.proofs

import at.logic.gapt.expr.HOLFormula
import at.logic.gapt.proofs.lk.base.{ Sequent, SequentIndex }
import at.logic.gapt.proofs.lkNew.OccConnector

trait SequentProof[Formula <: HOLFormula, This <: SequentProof[Formula, This]] extends DagProof[This] { self: This =>
  /**
   * A list of SequentIndices denoting the main formula(s) of the rule.
   */
  def mainIndices: Seq[SequentIndex]

  /**
   * The list of main formulas of the rule.
   */
  def mainFormulas: Seq[Formula] = mainIndices map { conclusion( _ ) }

  /**
   * A list of lists of SequentIndices denoting the auxiliary formula(s) of the rule.
   * The first list contains the auxiliary formulas in the first premise and so on.
   */
  def auxIndices: Seq[Seq[SequentIndex]]

  /**
   * The conclusion of the rule.
   */
  def conclusion: Sequent[Formula]

  /**
   * The upper sequents of the rule.
   */
  def premises: Seq[Sequent[Formula]] = immediateSubProofs map ( _.conclusion )

  /**
   * A list of lists containing the auxiliary formulas of the rule.
   * The first list constains the auxiliary formulas in the first premise and so on.
   */
  def auxFormulas: Seq[Seq[Formula]] = for ( ( p, is ) <- premises zip auxIndices ) yield p( is )

  /**
   * A list of occurrence connectors, one for each immediate subproof.
   */
  def occConnectors: Seq[OccConnector]

  override protected def stepString( subProofLabels: Map[Any, String] ) =
    s"$conclusion    (${super.stepString( subProofLabels )})"
}
