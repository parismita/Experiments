/**@author mathew
 * 
 * Class used for encoding a CaseExpression. Any Case expression (currently assumes that
 * case expressions are non-nested, i.e. no case expression within  another) is 
 * encoded as a list of when Conditionals and an else conditional. Also 
 * assumes that cases with switch expressions are normalized to ones that does not 
 * have switch expressions. For instance, Case A When X1 then Y1 When X2 Then Y2 ..
 * is normalized to Case When A=X1 then Y1 When A=X2 Then Y2 ...
 * 
 * */
package parsing;

import java.io.Serializable;
import java.util.ArrayList;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitor;
import net.sf.jsqlparser.statement.create.table.ColDataType;

public class CastExpression implements Cloneable,Serializable{
	
	private static final long serialVersionUID = -7192918525557389737L;
	
	Expression leftExpression;
   	ColDataType type;
   	boolean useCastKeyword = true;

	
	public CastExpression() {
	}
	public CastExpression(Expression e, ColDataType t, boolean u) {
		leftExpression=e;
		type=t;
		useCastKeyword=u;
	}

	public ColDataType getType() {
		return this.type;
	}

	public void setType(ColDataType type) {
		this.type = type;
	}

	public Expression getLeftExpression() {
		return this.leftExpression;
	}

	public void setLeftExpression(Expression expression) {
		this.leftExpression = expression;
	}

	public boolean isUseCastKeyword() {
		return this.useCastKeyword;
	}

	public void setUseCastKeyword(boolean useCastKeyword) {
		this.useCastKeyword = useCastKeyword;
	}

	@Override
	public String toString() {
		return this.useCastKeyword ? "CAST(" + this.leftExpression + " AS " + this.type.toString() + ")" : this.leftExpression + "::" + this.type.toString();
	}
}
