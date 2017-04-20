package org.xvm.compiler.ast;


import org.xvm.compiler.Token;

import java.lang.reflect.Field;

import java.util.List;


/**
 * A composition step. This defines one of the "building blocks" for a type composition.
 *
 * @author cp 2017.03.28
 */
public abstract class Composition
        extends AstNode
    {
    // ----- constructors --------------------------------------------------------------------------

    public Composition(Expression condition, Token keyword, TypeExpression type)
        {
        this.condition = condition;
        this.keyword   = keyword;
        this.type      = type;
        }


    // ----- accessors -----------------------------------------------------------------------------

    public Token getKeyword()
        {
        return keyword;
        }

    public TypeExpression getType()
        {
        return type;
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
        return toStartString() + toEndString();
        }

    public String toStartString()
        {
        StringBuilder sb = new StringBuilder();

        if (condition != null)
            {
            sb.append("if (")
                    .append(condition)
                    .append(") { ");
            }

        sb.append(keyword.getId().TEXT)
                .append(' ')
                .append(type);

        return sb.toString();
        }

    protected String toEndString()
        {
        return condition == null ? "" : " }";
        }

    @Override
    public String getDumpDesc()
        {
        return toString();
        }


    // ----- inner class: Extends ------------------------------------------------------------------

    public static class Extends
            extends Composition
        {
        public Extends(Expression condition, Token keyword, TypeExpression type, List<Expression> args)
            {
            super(condition, keyword, type);
            this.args = args;
            }

        @Override
        protected Field[] getChildFields()
            {
            return CHILD_FIELDS;
            }

        @Override
        public String toString()
            {
            StringBuilder sb = new StringBuilder();

            sb.append(toStartString());

            if (args != null)
                {
                sb.append('(');
                boolean first = true;
                for (Expression arg : args)
                    {
                    if (first)
                        {
                        first = false;
                        }
                    else
                        {
                        sb.append(", ");
                        }
                    sb.append(arg);
                    }
                  sb.append(')');
                }

            sb.append(toEndString());
            return sb.toString();
            }

        protected List<Expression> args;

        private static final Field[] CHILD_FIELDS = fieldsForNames(Extends.class, "condition", "type", "args");
        }


    // ----- inner class: Incorporates -------------------------------------------------------------

    public static class Incorporates
            extends Composition
        {
        public Incorporates(Expression condition, Token keyword, TypeExpression type, List<Expression> args)
            {
            super(condition, keyword, type);
            this.args = args;
            }

        @Override
        protected Field[] getChildFields()
            {
            return CHILD_FIELDS;
            }

        @Override
        public String toString()
            {
            StringBuilder sb = new StringBuilder();

            sb.append(toStartString());

            if (args != null)
                {
                sb.append('(');
                boolean first = true;
                for (Expression arg : args)
                    {
                    if (first)
                        {
                        first = false;
                        }
                    else
                        {
                        sb.append(", ");
                        }
                    sb.append(arg);
                    }
                sb.append(')');
                }

            sb.append(toEndString());
            return sb.toString();
            }

        protected List<Expression> args;

        private static final Field[] CHILD_FIELDS = fieldsForNames(Incorporates.class, "condition", "type", "args");
        }


    // ----- inner class: Implements ---------------------------------------------------------------

    public static class Implements
            extends Composition
        {
        public Implements(Expression condition, Token keyword, TypeExpression type)
            {
            super(condition, keyword, type);
            }
        }


    // ----- inner class: Delegates ----------------------------------------------------------------

    public static class Delegates
            extends Composition
        {
        public Delegates(Expression condition, Token keyword, TypeExpression type, Expression delegate)
            {
            super(condition, keyword, type);
            this.delegate = delegate;
            }

        @Override
        protected Field[] getChildFields()
            {
            return CHILD_FIELDS;
            }

        @Override
        public String toString()
            {
            return toStartString() + '(' + delegate + ')' + toEndString();
            }

        protected Expression delegate;

        private static final Field[] CHILD_FIELDS = fieldsForNames(Delegates.class, "condition", "type", "delegate");
        }


    // ----- inner class: Into ---------------------------------------------------------------------

    public static class Into
            extends Composition
        {
        public Into(Expression condition, Token keyword, TypeExpression type)
            {
            super(condition, keyword, type);
            }
        }


    // ----- inner class: Import -------------------------------------------------------------------

    public static class Import
            extends Composition
        {
        public Import(Expression condition, Token keyword, NamedTypeExpression type, List<VersionOverride> vers)
            {
            super(condition, keyword, type);
            this.vers = vers;
            }

        @Override
        protected Field[] getChildFields()
            {
            return CHILD_FIELDS;
            }

        @Override
        public String toString()
            {
            StringBuilder sb = new StringBuilder();

            sb.append(toStartString());

            if (vers != null)
                {
                boolean first = true;
                for (VersionOverride ver : vers)
                    {
                    if (first)
                        {
                        sb.append(' ');
                        first = false;
                        }
                    else
                        {
                        sb.append("\n        ");
                        }
                    sb.append(ver);
                    }
                }

            sb.append(toEndString());
            return sb.toString();
            }

        protected List<VersionOverride> vers;

        private static final Field[] CHILD_FIELDS = fieldsForNames(Import.class, "condition", "type", "vers");
        }


    // ----- fields --------------------------------------------------------------------------------

    protected Expression     condition;
    protected Token          keyword;
    protected TypeExpression type;

    private static final Field[] CHILD_FIELDS = fieldsForNames(Composition.class, "condition", "type");
    }
