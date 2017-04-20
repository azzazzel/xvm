package org.xvm.compiler.ast;


import java.lang.reflect.Field;

import java.util.List;


/**
 * An array access expression is an expression followed by an array index expression.
 *
 * @author cp 2017.04.08
 */
public class ArrayAccessExpression
        extends Expression
    {
    // ----- constructors --------------------------------------------------------------------------

    public ArrayAccessExpression(Expression expr, List<Expression> indexes)
        {
        this.expr    = expr;
        this.indexes = indexes;
        }


    // ----- accessors -----------------------------------------------------------------------------

    @Override
    public TypeExpression toTypeExpression()
        {
        return new ArrayTypeExpression(expr.toTypeExpression(), indexes);
        }

    @Override
    protected Field[] getChildFields()
        {
        return CHILD_FIELDS;
        }


    // ----- debugging assistance ------------------------------------------------------------------

    @Override
    public String toString()
        {
        StringBuilder sb = new StringBuilder();

        sb.append(expr)
          .append('[');

        boolean first = true;
        for (Expression index : indexes)
            {
            if (first)
                {
                first = false;
                }
            else
                {
                sb.append(", ");
                }
            sb.append(index);
            }

          sb.append(']');

        return sb.toString();
        }

    @Override
    public String getDumpDesc()
        {
        return toString();
        }


    // ----- fields --------------------------------------------------------------------------------

    protected Expression       expr;
    protected List<Expression> indexes;

    private static final Field[] CHILD_FIELDS = fieldsForNames(ArrayAccessExpression.class, "expr", "indexes");
    }
