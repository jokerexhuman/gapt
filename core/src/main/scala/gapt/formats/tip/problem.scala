package gapt.formats.tip

import gapt.proofs.context.State
import gapt.proofs.context.update.Update
import gapt.expr._
import gapt.expr.formula.{ All, Atom, Bottom, Eq, Formula, Iff, Imp, Neg, Top }
import gapt.expr.formula.hol.{ existentialClosure, universalClosure }
import gapt.expr.subst.Substitution
import gapt.expr.ty.FunctionType
import gapt.expr.ty.TBase
import gapt.expr.ty.To
import gapt.expr.util.{ LambdaPosition, freeVariables, syntacticMatching, variables }
import gapt.proofs.Sequent
import gapt.proofs.context.Context
import gapt.proofs.context.facet.{ ConditionalReductions, Reductions }
import gapt.proofs.context.immutable.ImmutableContext
import gapt.proofs.context.update.InductiveType
import gapt.provers.viper.spin.Positions

case class TipConstructor( constr: Const, projectors: Seq[Const] ) {
  val FunctionType( datatype, fieldTypes ) = constr.ty
  require( fieldTypes.size == projectors.size )
  projectors foreach { case Const( _, FunctionType( to, from ), _ ) => require( from == Seq( datatype ) ) }

  def arity = projectors size

  def projectorDefinitions: Seq[Formula] = {
    val fieldVars = fieldTypes.zipWithIndex.map { case ( t, i ) => Var( s"x$i", t ) }
    ( projectors, fieldVars ).zipped map { ( p, f ) => p( constr( fieldVars: _* ) ) === f }
  }

  def projectReductionRules: Seq[ReductionRule] = {
    val fieldVars = fieldTypes.zipWithIndex.map { case ( t, i ) => Var( s"x$i", t ) }
    val constructorTerm = Apps( constr, fieldVars )
    projectors.zipWithIndex.map {
      case ( p, i ) => ReductionRule( App( p, constructorTerm ), fieldVars( i ) )
    }
  }
}
case class TipDatatype( t: TBase, constructors: Seq[TipConstructor] ) {
  constructors foreach { ctr => require( ctr.datatype == t ) }
}

case class TipFun( fun: Const, definitions: Seq[Formula] )

case class TipProblem(
    ctx:                 ImmutableContext,
    sorts:               Seq[TBase],
    datatypes:           Seq[TipDatatype],
    uninterpretedConsts: Seq[Const],
    functions:           Seq[TipFun],
    assumptions:         Seq[Formula],
    goal:                Formula ) {
  def constructorInjectivity =
    for {
      TipDatatype( ty, ctrs ) <- datatypes
      if ty != To // FIXME
      ( TipConstructor( ctr1, _ ), i1 ) <- ctrs.zipWithIndex
      ( TipConstructor( ctr2, _ ), i2 ) <- ctrs.zipWithIndex
      if i1 < i2 // ignore symmetric pairs
      FunctionType( _, args1 ) = ctr1.ty
      FunctionType( _, args2 ) = ctr2.ty
    } yield universalClosure(
      ctr1( ( for ( ( t, j ) <- args1.zipWithIndex ) yield Var( s"x$j", t ) ): _* ) !==
        ctr2( ( for ( ( t, j ) <- args2.zipWithIndex ) yield Var( s"y$j", t ) ): _* ) )

  def toSequent = existentialClosure(
    datatypes.flatMap( _.constructors ).flatMap( _.projectorDefinitions ) ++:
      functions.flatMap( _.definitions ) ++:
      constructorInjectivity ++:
      assumptions ++:
      Sequent()
      :+ goal )

  def context: ImmutableContext = ctx ++ reductionRules.filter( rule =>
    // TODO: hotfix, to be removed
    rule match {
      case ConditionalReductionRule(
        List(),
        Apps( c1 @ Const( _, _, _ ), Seq( x1, y1 ) ),
        Apps( c2 @ Const( _, _, _ ), Seq( y2, x2 ) )
        ) if c1 == c2 && x1 == x2 && y1 == y2 => false
      // TODO: maybe not important?
      case ConditionalReductionRule(
        List(),
        Apps( c1 @ Const( _, _, _ ), Seq( x1, Apps( c2 @ Const( _, _, _ ), Seq( y1, z1 ) ) ) ),
        Apps( c3 @ Const( _, _, _ ), Seq( Apps( c4 @ Const( _, _, _ ), Seq( x2, y2 ) ), z2 ) )
        ) if c1 == c2 && c2 == c3 && c3 == c4 && x1 == x2 && y1 == y2 && z1 == z2 => false
      case _ => true
    } )

  def reductionRules: Seq[ConditionalReductionRule] = {
    val destructorReductionRules = datatypes.flatMap {
      _.constructors.flatMap { _.projectReductionRules }.map {
        case ReductionRule( lhs, rhs ) => ConditionalReductionRule( Nil, lhs, rhs )
      }
    }
    val definitionReductionRules = assumptions.flatMap {
      case All.Block( _, Eq( lhs @ Apps( Const( _, _, _ ), _ ), rhs ) ) =>
        Some( ConditionalReductionRule( Nil, lhs, rhs ) )
      case _ => None
    }
    functionDefinitionReductionRules ++
      destructorReductionRules ++
      definitionReductionRules :+
      ConditionalReductionRule( Nil, le"x = x", le"⊤" ) :+
      ConditionalReductionRule( Nil, hof"¬ ⊥", hof"⊤" ) :+
      ConditionalReductionRule( Nil, hof"¬ ⊤", hof"⊥" )
  }

  private val functionDefinitionReductionRules: Seq[ConditionalReductionRule] = {
    functions.flatMap { _.definitions }.flatMap {
      case All.Block( _, Imp.Block( cs, Eq( lhs @ Apps( _: Const, _ ), rhs ) ) ) if isReductionRule( cs, lhs, rhs ) =>
        Some( ConditionalReductionRule( cs, lhs, rhs ) )
      case All.Block( _, Imp.Block( cs, Neg( lhs @ Atom( _, _ ) ) ) ) if isReductionRule( cs, lhs, Bottom() ) =>
        Some( ConditionalReductionRule( cs, lhs, Bottom() ) )
      case All.Block( _, Imp.Block( cs, lhs @ Atom( _, _ ) ) ) if isReductionRule( cs, lhs, Top() ) =>
        Some( ConditionalReductionRule( cs, lhs, Top() ) )
      case All.Block( _, Imp.Block( cs, Iff( lhs, rhs ) ) ) if isReductionRule( cs, lhs, rhs ) =>
        Some( ConditionalReductionRule( cs, lhs, rhs ) )
      case _ => None
    }
  }

  private def isReductionRule( cs: Seq[Formula], lhs: Expr, rhs: Expr ): Boolean = {
    ( cs.flatMap { freeVariables( _ ) } ++ freeVariables( rhs ) ).toSet.subsetOf( freeVariables( lhs ) ) &&
      !lhs.isInstanceOf[Var]
  }

  override def toString: String = toSequent.toSigRelativeString( context )
}

/**
 * A conditional rewrite rule.
 *
 * An instance of this rule can be used to rewrite the left hand side
 * into its right hand side only if the conditions all rewrite to ⊤.
 *
 * The free variables of the conditions together with those of the
 * right hand side must form a subset of the free variables of the
 * left hand side. The left hand side must not be a variable.
 *
 * @param conditions The conditions of this rewrite rule.
 * @param lhs The left hand side of this rewrite rule.
 * @param rhs The right hand side of this rewrite rule.
 */
case class ConditionalReductionRule( conditions: Seq[Formula], lhs: Expr, rhs: Expr ) extends Update {

  require(
    ( conditions.flatMap { freeVariables( _ ) } ++
      freeVariables( rhs ) ).toSet.subsetOf( freeVariables( lhs ) ),
    """free variables in conditions and right hand side do not form a
      |subset of the free variables of the left hand side""".stripMargin )

  require( !lhs.isInstanceOf[Var], "left hand side must not be a variable" )

  override def apply( ctx: Context ): State = {
    // TODO: why does this not work?
    // ctx.check( lhs )
    // ctx.check( rhs )
    ctx.state.update[ConditionalReductions]( _ + this )
  }

  val Apps( lhsHead @ Const( lhsHeadName, _, _ ), lhsArgs ) = lhs
  val lhsArgsSize: Int = lhsArgs.size

  val allArgs: Set[Int] = lhsArgs.zipWithIndex.map( _._2 ).toSet

  // Argument positions that occur only passively or not at all in expr
  def passivesIn( expr: Expr, allPositions: Map[Const, Positions] ): Set[Int] = {
    def go( e: Expr ): Set[Int] =
      e match {
        case Apps( f @ Const( _, _, _ ), rhsArgs ) if allPositions.isDefinedAt( f ) =>
          val poses = allPositions( f )
          val passArgs = if ( poses == null ) Set() else poses.passiveArgs
          val passives = passArgs.map( rhsArgs )
          val passVars = passives.flatMap( variables( _ ) )
          val immediate = lhsArgs.zipWithIndex.collect {
            case ( l, i ) if passVars.intersect( variables( l ) ).nonEmpty => i
          }
          val nested = rhsArgs flatMap go
          immediate.toSet.intersect( nested.toSet )
        case App( a, b ) => go( a ) intersect go( b )
        case _           => allArgs
      }

    go( expr )
  }

  def primariesIn( expr: Expr, allPositions: Map[Const, Positions] ): Set[Int] = {
    def go( e: Expr ): Set[Int] =
      e match {
        case Apps( f @ Const( _, _, _ ), rhsArgs ) if allPositions.isDefinedAt( f ) =>
          val poses = allPositions( f )
          val primArgs = if ( poses == null ) rhsArgs.zipWithIndex.map( _._2 ).toSet else poses.primaryArgs
          val prims = primArgs.map( rhsArgs )
          val primVars = prims.flatMap( variables( _ ) )
          val immediate = lhsArgs.zipWithIndex.collect {
            case ( l, i ) if primVars.intersect( variables( l ) ).nonEmpty => i
          }
          val nested = prims flatMap go
          immediate.toSet ++ nested
        case App( a, b ) => go( a ) ++ go( b )
        case _           => Set()
      }

    go( expr )
  }

  def conditionalPrimaries( allPositions: Map[Const, Positions] ): Set[Int] =
    conditions.flatMap( cond => primariesIn( cond, allPositions ) ).toSet

  // Positions of arguments which do not change in recursive calls
  // or None if there are no recursive calls on the rhs.
  def selfPassiveArgs( allPositions: Map[Const, Positions] ): Option[Set[Int]] = {
    def go( e: Expr ): Option[Set[Int]] =
      e match {
        case Apps( f, rhsArgs ) if f == lhsHead =>
          val args = lhsArgs.zip( rhsArgs ).zipWithIndex collect {
            case ( ( l, r ), i ) if l == r => i
          }
          Some( args.toSet )
        case App( a, b ) =>
          go( a ) match {
            case None         => go( b )
            case Some( args ) => Some( args.intersect( go( b ).getOrElse( allArgs ) ) )
          }
        case _ => None
      }

    go( rhs )
  }

  // Positions of arguments that are self-passive and also passive in calls to other functions.
  def passiveArgs( allPositions: Map[Const, Positions] ): Option[Set[Int]] = {
    val conds = conditions.foldLeft( allArgs )( ( acc, cond ) => acc intersect passivesIn( cond, allPositions ) )
    selfPassiveArgs( allPositions ).map( _ intersect conds )
  }

  // Positions of non-passive arguments which are not matched on or None if no recursive calls on the rhs.
  def accumulatorArgs( allPositions: Map[Const, Positions] ): Option[Set[Int]] = {
    passiveArgs( allPositions ).map { passives =>
      val own = lhsArgs.zipWithIndex.collect { case ( e, i ) if e.isInstanceOf[Var] => i }.toSet -- passives
      ( own -- primariesIn( rhs, allPositions ) ) -- conditionalPrimaries( allPositions )
    }
  }

  // Positions of non-passive, non-accumulator arguments, that is, args which are matched on in the lhs and
  // change in the recursive calls on the rhs or are passed on in primary position.
  // None if no recursive calls on the rhs.
  def primaryArgs( allPositions: Map[Const, Positions] ): Option[Set[Int]] =
    passiveArgs( allPositions ).flatMap { passives =>
      accumulatorArgs( allPositions ).map { accumulators =>
        ( allArgs -- passives ) -- accumulators
      }
    }
}

case class ConditionalNormalizer( rewriteRules: Set[ConditionalReductionRule] ) {

  private val unconditionalRules =
    rewriteRules
      .filter { _.conditions.isEmpty }
      .map { r => ReductionRule( r.lhs, r.rhs ) }

  private val conditionalRules = rewriteRules.diff( rewriteRules.filter { _.conditions.isEmpty } )

  private val unconditionalNormalizer = Normalizer( unconditionalRules )

  /**
   * Normalizes an expression.
   *
   * @param e The expression to be normalized.
   * @return Returns the normalized expression, if the rewrite rules are terminating.
   */
  def normalize( e: Expr ): Expr = {
    normalize_( unconditionalNormalizer.normalize( e ) )
  }

  private def normalize_( e: Expr ): Expr = {
    for {
      ConditionalReductionRule( conditions, lhs, rhs ) <- conditionalRules
      ( instance, position ) <- findInstances( e, lhs, Nil )
    } {
      if ( conditions.map { instance( _ ) }.map { normalize( _ ) }.forall { _ == Top() } ) {
        return normalize( e.replace( position, instance( rhs ) ) )
      }
    }
    e
  }

  private def findInstances( e: Expr, l: Expr, position: List[Int] ): Set[( Substitution, LambdaPosition )] = {
    subterms( e ).flatMap {
      case ( t, p ) =>
        for {
          subst <- syntacticMatching( l, t )
        } yield { subst -> p }
    }.toSet
  }
}

object subterms {
  def apply( e: Expr ): Seq[( Expr, LambdaPosition )] = {
    subterms( e, LambdaPosition() ).map {
      case ( t, LambdaPosition( ps ) ) => t -> LambdaPosition( ps.reverse )
    }
  }
  private def apply( e: Expr, position: LambdaPosition ): Seq[( Expr, LambdaPosition )] = {
    val LambdaPosition( xs ) = position
    ( e -> position ) +: ( e match {
      case Abs( _, e1 ) =>
        subterms( e1, LambdaPosition( 1 :: xs ) )
      case App( e1, e2 ) =>
        subterms( e1, LambdaPosition( 1 +: xs ) ) ++ subterms( e2, LambdaPosition( 2 +: xs ) )
      case _ => Seq()
    } )
  }
}

trait TipProblemDefinition {
  def sorts: Seq[TBase]
  def datatypes: Seq[TipDatatype]
  def uninterpretedConsts: Seq[Const]
  def assumptions: Seq[Formula]
  def functions: Seq[TipFun]
  def goal: Formula
  def loadProblem: TipProblem = {
    var ctx = Context()
    sorts foreach { ctx += _ }
    datatypes foreach {
      dt =>
        {
          if ( !ctx.isType( dt.t ) ) {
            ctx += InductiveType( dt.t, dt.constructors.map( _.constr ): _* )
          }
          dt.constructors.foreach { ctr => ctr.projectors.foreach { ctx += _ } }
        }
    }
    uninterpretedConsts foreach { constant =>
      if ( ctx.constant( constant.name ).isEmpty ) {
        ctx += constant
      }
    }
    functions foreach { function =>
      ctx += function.fun
    }
    TipProblem( ctx, sorts, datatypes, uninterpretedConsts, functions, assumptions, goal )
  }
}

object tipScalaEncoding {

  private def compileConst( const: Const ): String = {
    "hoc" + "\"" + stripNewlines( "'" + const.name + "' :" + const.ty.toString ) + "\""
  }

  def apply( problem: TipProblem ): String = {
    "// Sorts\n" +
      compileSorts( problem ).mkString( "\n" ) + "\n\n" +
      "// Inductive types\n" +
      compileInductiveTypes( problem ).mkString( "\n\n" ) + "\n" +
      compileFunctionConstants( problem ) + "\n\n" +
      s"""|val sequent =
          |  hols\"\"\"
          |    ${
        ( compileProjectorDefinitions( problem ) ++
          compileFunctionDefinitions( problem ) ++
          compileConstructorInjectivityAxioms( problem ) ++
          compileProblemAssumptions( problem ) ) mkString ( "", ",\n    ", "" )
      }
          |    :-
          |    goal: ${stripNewlines( problem.goal.toString )}
          |  \"\"\"
      """.stripMargin
  }

  private def compileProblemAssumptions( problem: TipProblem ): Seq[String] = {
    problem.assumptions.zipWithIndex.map {
      case ( assumption, index ) => s"assumption_$index: ${stripNewlines( assumption.toString() )}"
    }
  }

  private def compileConstructorInjectivityAxioms( problem: TipProblem ): Seq[String] = {
    problem.constructorInjectivity.zipWithIndex.map {
      case ( axiom, index ) => s"constr_inj_$index: ${stripNewlines( universalClosure( axiom ).toString() )}"
    }
  }

  private def compileFunctionDefinitions( problem: TipProblem ): Seq[String] = {
    problem.functions.flatMap {
      function =>
        function.definitions.zipWithIndex.map {
          case ( definition, index ) =>
            s"def_${function.fun.name}_$index: ${stripNewlines( universalClosure( definition ).toString() )}"
        }
    }
  }

  private def compileProjectorDefinitions( problem: TipProblem ): Seq[String] = {
    val constructors = problem.datatypes.flatMap( _.constructors )
    ( constructors.flatMap( _.projectors ).map( _.name ) zip
      constructors.flatMap( _.projectorDefinitions ) ) map
      {
        case ( name, definition ) =>
          s"def_${
            name.map { c => if ( c == '-' ) '_' else c }
          }: ${
            stripNewlines( universalClosure( definition ).toString() )
          }"
      }
  }

  private def compileFunctionConstants( problem: TipProblem ): String = {
    "\n//Function constants\n" +
      ( problem.functions map { f => "ctx += " + compileConst( f.fun ) } mkString ( "\n" ) )
  }

  private def compileInductiveTypes( problem: TipProblem ): Seq[String] = {
    problem.datatypes.tail map compileInductiveType
  }

  private def compileInductiveType( datatype: TipDatatype ): String = {
    val constructors = datatype.constructors.map { c => compileConst( c.constr ) } mkString ( ", " )
    val projectors = compileProjectors( datatype.constructors.flatMap( _.projectors ) )
    s"ctx += InductiveType(ty${"\"" + datatype.t.name + "\""}, ${constructors})" + "\n" + projectors
  }

  private def compileProjectors( projectors: Seq[Const] ): String = {
    projectors.map { compileProjector } mkString ( "", "\n", "" )
  }

  private def compileProjector( projector: Const ): String = {
    s"ctx += ${compileConst( projector )}"
  }

  private def compileSorts( problem: TipProblem ): Seq[String] =
    problem.sorts map {
      sort => s"ctx += TBase(${"\"" + sort.name + "\""})"
    }

  private def stripNewlines( s: String ): String =
    s.map( c => if ( c == '\n' ) ' ' else c )
}
