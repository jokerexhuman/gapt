package at.logic.gapt.proofs.lk

import at.logic.gapt.proofs.lk.reductions.Reduction
import at.logic.gapt.proofs.{ Context, SequentConnector }

trait ReductionStrategy {
  def run( proof: LKProof ): LKProof
}

/**
 * Applies the given reduction exhaustively to uppermost redexes.
 * @param reduction The reduction to be used by this reduction strategy.
 */
class UppermostFirstStrategy( reduction: Reduction ) extends ReductionStrategy {
  def run( proof: LKProof ): LKProof = {
    new LKVisitor[Unit] {
      override def recurse( proof: LKProof, u: Unit ): ( LKProof, SequentConnector ) = {
        val ( intermediaryProof, intermediaryConnector ): ( LKProof, SequentConnector ) = super.recurse( proof, u )
        reduction.reduce( intermediaryProof ) match {
          case Some( intermediaryProof2 ) => {
            val ( finalProof, _ ): ( LKProof, SequentConnector ) = recurse( intermediaryProof2, u )
            ( finalProof, SequentConnector.guessInjection(
              fromLower = proof.conclusion, toUpper = finalProof.conclusion ).inv )
          }
          case None => ( intermediaryProof, intermediaryConnector )
        }
      }
    }.apply( proof, () )
  }
}

/**
 * Applies the given reduction exhaustively to lowermost redexes.
 * @param reduction The reduction to be used by this reduction strategy.
 */
class IterativeParallelStrategy( reduction: Reduction ) extends ReductionStrategy {
  private var foundRedex = false
  override def run( proof: LKProof ): LKProof = {
    var intermediaryProof = proof
    val reducer = ( new LowerMostRedexReducer( reduction ) )
    do {
      reducer.foundRedex = false
      intermediaryProof = reducer.apply( intermediaryProof, () )
      if ( reducer.foundRedex ) foundRedex = true
    } while ( reducer.foundRedex )
    intermediaryProof
  }
  def appliedReduction: Boolean = foundRedex
}

/**
 * Describes objects that can apply a reduction to redexes.
 */
trait RedexReducer {
  def foundRedex: Boolean
}

/**
 * Applies a given reduction to the lowermost redexes.
 * @param reduction The reduction to be applied to the lowermost redexes.
 */
class LowerMostRedexReducer( reduction: Reduction ) extends LKVisitor[Unit] with RedexReducer {

  var foundRedex: Boolean = false

  override def recurse( proof: LKProof, u: Unit ): ( LKProof, SequentConnector ) = {
    reduction.reduce( proof ) match {
      case Some( finalProof ) =>
        foundRedex = true
        ( finalProof, SequentConnector.guessInjection(
          fromLower = proof.conclusion, toUpper = finalProof.conclusion ).inv )
      case _ => super.recurse( proof, u )
    }
  }
}

trait Selector {
  def createSelectorReduction( proof: LKProof ): Option[Reduction]
}

class IterativeSelectiveStrategy( selector: Selector ) extends ReductionStrategy {
  override def run( proof: LKProof ): LKProof = {
    var intermediaryProof = proof
    var continue = false
    do {
      continue = false
      selector.createSelectorReduction( intermediaryProof ) match {
        case Some( selectorReduction ) =>
          continue = true
          intermediaryProof = ( new LowerMostRedexReducer( selectorReduction ) ).apply( intermediaryProof, () )
        case None =>
      }
    } while ( continue )
    intermediaryProof
  }
}