/*
 * typedLambdaCalculus.scala
 *
 */

package at.logic.language

import at.logic.language.Symbols._
import scala.collection.immutable._

object TypedLambdaCalculus {

    import at.logic.language.Types._
    
    
    abstract class LambdaExpression {
        def exptype: TA

        def getFreeAndBoundVariables():Tuple2[Set[Var],Set[Var]] = this match {
            case v: Var => (HashSet(v),  new EmptySet )
            case App(m,n) => {
                    val mFBV = m.getFreeAndBoundVariables()
                    val nFBV = n.getFreeAndBoundVariables()
                    (mFBV._1 ++ nFBV._1, mFBV._2 ++ nFBV._2)
            }
            case Abs(x,m) => {
                    val mFBV = m.getFreeAndBoundVariables()
                    val bound = mFBV._2 + x
                    val free = mFBV._1 - x
                    (free, bound)
            }
        }
    }


    case class Var(name: SymbolA, exptype: TA )
        extends LambdaExpression

    case class Abs(variable: Var, expression: LambdaExpression)
        extends LambdaExpression {
            def exptype: TA = ->(variable.exptype,expression.exptype)
        }

    case class App(function: LambdaExpression, argument: LambdaExpression )
        extends LambdaExpression {
            require({
                function.exptype match {
                    case ->(in,out) => {if (in == argument.exptype) true
                                        else false}
                    case _          => false
                }
            })
            def exptype: TA = {
                function.exptype match {
                    case ->(in,out) => out
                }
            }
        }

    object AbsN {
        def apply(variables: List[Var], expression: LambdaExpression) = if (!variables.isEmpty) (variables :\ expression)(Abs)
                                                                        else expression
        def unapply(expression: LambdaExpression):Option[(List[Var], LambdaExpression)] = Some(unapplyRec(expression))
        def unapplyRec(expression: LambdaExpression): (List[Var], LambdaExpression) = expression match {
            case Abs(v,exp) => (v :: unapplyRec(exp)._1, unapplyRec(exp)._2 )
            case v: Var => (Nil, v) 
            case a: App => (Nil, a)
        }
    }

    object AppN {
        def apply(function: LambdaExpression, arguments:List[LambdaExpression]) = if (!arguments.isEmpty) (function /: arguments)(App)
                                                                                  else function
        def unapply(expression: LambdaExpression):Option[(LambdaExpression, List[LambdaExpression])] = Some(unapplyRec(expression))
        def unapplyRec(expression: LambdaExpression):(LambdaExpression, List[LambdaExpression]) = expression match {
            case App(f,arg) => (unapplyRec(f)._1, unapplyRec(f)._2 ::: (arg::Nil) )
            case v: Var => (v,Nil)
            case a: Abs => (a,Nil)
        }
    }

    object freshVar {
        private var counter = 0
        def apply(exptype: TA) = {
            counter += 1
            new Var(StringSymbol("#"+counter), exptype)
        }
    }

    def exportLambdaExpressionToString(expression: LambdaExpression):String = expression match {
        case Var(name,exptype) => name.toString
        case Abs(variable, exp) => "\\" + exportLambdaExpressionToString(variable) + "." + exportLambdaExpressionToString(exp)
        case App(function, argument) => "(" + exportLambdaExpressionToString(function) + " " + exportLambdaExpressionToString(argument)  + ")"
    }

    def exportLambdaExpressionToStringWithTypes(expression: LambdaExpression):String = expression match {
        case Abs(variable, exp) => "\\" + exportLambdaExpressionToString(variable) + "." + exportLambdaExpressionToString(exp)
        case App(function, argument) => "(" + exportLambdaExpressionToString(function) + " " + exportLambdaExpressionToString(argument)  + ")"
        case Var(name,exptype) => {
            name.toString +
                "ToDo"

        }
    }
}