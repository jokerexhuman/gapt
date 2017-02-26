package at.logic.gapt.cutintro
import at.logic.gapt.expr._
import at.logic.gapt.proofs.{ Context, Sequent }
import at.logic.gapt.proofs.gaptic._
import at.logic.gapt.proofs.gaptic.{ Lemma, guessLabels, tactics }
import at.logic.gapt.proofs.gaptic.tactics._
import at.logic.gapt.proofs.lk.LKProof

/**
 * Created by root on 08.02.17.
 */
object proveWithPi2Cut {

  def apply(
    endSequent:                Sequent[FOLFormula],
    seHs:                      Pi2SeHs,
    nameOfExistentialVariable: FOLVar              = fov"yCut",
    nameOfUniversalVariable:   FOLVar              = fov"xCut"
  ): ( Option[LKProof] ) = {

    val ( cutFormulaWithoutQuantifiers: Option[FOLFormula], nameOfExVa: FOLVar, nameOfUnVa: FOLVar ) = introducePi2Cut( seHs, nameOfExistentialVariable, nameOfUniversalVariable )

    cutFormulaWithoutQuantifiers match {
      case Some( t ) => giveProof( t, seHs, endSequent, nameOfExVa, nameOfUnVa )
      case None => {
        println( "No cut formula has been found." )
        None
      }
    }
  }

  private def giveProof(
    cutFormulaWithoutQuantifiers: FOLFormula,
    seHs:                         Pi2SeHs,
    endSequent:                   Sequent[FOLFormula],
    nameOfExVa:                   FOLVar,
    nameOfUnVa:                   FOLVar
  ): ( Option[LKProof] ) = {

    var ctx = Context.default
    ctx += Context.Sort( "i" )
    for ( c <- constants( seHs.reducedRepresentation.antecedent ++: endSequent :++ seHs.reducedRepresentation.succedent ) ) ctx += c
    for ( c <- constants( seHs.substitutionsForBetaWithAlpha ); if !ctx.constants.exists( t => t == c ) ) ctx += c
    for ( c <- constants( seHs.substitutionsForAlpha ); if !ctx.constants.exists( t => t == c ) ) ctx += c
    for ( c <- constants( seHs.existentialEigenvariables ); if !ctx.constants.exists( t => t == c ) ) ctx += c
    for ( c <- constants( seHs.universalEigenvariable ); if !ctx.constants.exists( t => t == c ) ) ctx += c

    var state = ProofState( guessLabels( endSequent ) )
    state += cut( "Cut", fof"!$nameOfUnVa ?$nameOfExVa ($cutFormulaWithoutQuantifiers )" )
    state += allR( "Cut", seHs.universalEigenvariable )
    for ( t <- seHs.substitutionsForBetaWithAlpha ) { state += exR( "Cut", t ) }
    state += haveInstances( seHs.reducedRepresentation )
    state += prop
    for ( i <- 0 until seHs.multiplicityOfAlpha ) {
      state += allL( "Cut", seHs.substitutionsForAlpha( i ) )
      state += exL( "Cut_" + i.toString, seHs.existentialEigenvariables( i ) )
    }
    state += haveInstances( seHs.reducedRepresentation )
    state += prop
    val proof = state.result

    Some( proof )
  }

}