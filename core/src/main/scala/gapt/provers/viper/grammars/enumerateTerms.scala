package gapt.provers.viper.grammars

import cats.instances.list._
import cats.syntax.traverse._
import gapt.expr._
import gapt.proofs.context.Context
import gapt.proofs.context.facet.BaseTypes
import gapt.proofs.context.facet.StructurallyInductiveTypes
import gapt.utils.NameGenerator

import scala.collection.mutable

object enumerateTerms {

  private def normalizeFreeVars( expr: Expr ): Expr = {
    val nameGen = new NameGenerator( Set() )
    def norm( e: Expr ): Expr = e match {
      case App( a, b ) => App( norm( a ), norm( b ) )
      case Var( _, t ) => Var( nameGen.freshWithIndex( "x" ), t )
      case c: Const    => c
    }
    norm( expr )
  }

  def constructorsForType( t: Ty* )( implicit ctx: Context ): Set[VarOrConst] = {
    val done = mutable.Set[Ty]()
    val out = Set.newBuilder[VarOrConst]
    def go( t: Ty ): Unit = if ( !done( t ) ) {
      done += t
      ctx.getConstructors( t ) match {
        case Some( ctrs ) =>
          for ( ctr <- ctrs ) {
            out += ctr
            go( ctr.ty )
          }
        case None =>
          t match {
            case TArr( i, o ) =>
              go( i ); go( o )
            case _ => out += Var( "x", t )
          }
      }
    }
    t.foreach( go )
    out.result()
  }

  def asStream( implicit ctx: Context ): Stream[Expr] = withSymbols( Set.empty
    ++ ctx.get[StructurallyInductiveTypes].constructors.values.flatten
    ++ ( ctx.get[BaseTypes].baseTypes -- ctx.get[StructurallyInductiveTypes].constructors.keySet ).values.map( Var( "x", _ ) ) )

  def forType( ty: Ty* )( implicit ctx: Context ): Stream[Expr] =
    withSymbols( constructorsForType( ty: _* ) )

  def withSymbols( syms: Set[VarOrConst] ): Stream[Expr] = {
    val terms = mutable.Set[Expr]()
    terms ++= syms.view.filter( _.ty.isInstanceOf[TBase] )

    val nonConstantCtrs = syms.filter( !_.ty.isInstanceOf[TBase] )

    def take( tys: Seq[Ty] ): Seq[Seq[Expr]] =
      tys.toList.traverse( t => terms.filter( _.ty == t ).toList )
    def iterate() =
      nonConstantCtrs.flatMap {
        case ctr @ Const( _, FunctionType( _, argTypes ), _ ) =>
          take( argTypes ).map( ctr( _ ) ).map( normalizeFreeVars )
      }

    ( terms.toVector +: Stream.continually {
      val newTerms = iterate().filterNot( terms )
      terms ++= newTerms
      newTerms
    } ).takeWhile( _.nonEmpty ).flatten
  }

}
