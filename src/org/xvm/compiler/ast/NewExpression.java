package org.xvm.compiler.ast;


import java.lang.reflect.Field;

import java.util.List;

import org.xvm.asm.Argument;
import org.xvm.asm.ErrorListener;
import org.xvm.asm.MethodStructure.Code;
import org.xvm.asm.Register;

import org.xvm.asm.constants.MethodConstant;
import org.xvm.asm.constants.TypeConstant;
import org.xvm.asm.constants.TypeInfo;

import org.xvm.asm.op.New_0;
import org.xvm.asm.op.New_1;
import org.xvm.asm.op.New_N;

import org.xvm.compiler.Compiler;
import org.xvm.compiler.Constants;
import org.xvm.compiler.Token;

import org.xvm.compiler.ast.Statement.Context;

import org.xvm.util.Severity;

import static org.xvm.util.Handy.indentLines;


/**
 * "New object" expression.
 */
public class NewExpression
        extends Expression
    {
    // ----- constructors --------------------------------------------------------------------------

    /**
     * Construct a "new" expression.
     *
     * @param operator  presumably, the "new" operator
     * @param type      the type being instantiated
     * @param args      a list of constructor arguments for the type being instantiated
     * @param body      the body of the anonymous inner class being instantiated, or null
     * @param lEndPos   the expression's end position in the source code
     */
    public NewExpression(Token operator, TypeExpression type, List<Expression> args, StatementBlock body, long lEndPos)
        {
        assert operator != null;
        assert type != null;
        assert args != null;

        this.left     = null;
        this.operator = operator;
        this.type     = type;
        this.args     = args;
        this.body     = body;
        this.lEndPos  = lEndPos;
        }

    /**
     * Construct a ".new" expression.
     *
     * @param left      the "left" expression
     * @param operator  presumably, the "new" operator
     * @param type      the type being instantiated
     * @param args      a list of constructor arguments for the type being instantiated, or null
     * @param lEndPos   the expression's end position in the source code
     */
    public NewExpression(Expression left, Token operator, TypeExpression type, List<Expression> args, long lEndPos)
        {
        assert left != null;
        assert operator != null;
        assert type != null;
        assert args != null;

        this.left     = left;
        this.operator = operator;
        this.type     = type;
        this.args     = args;
        this.body     = null;
        this.lEndPos  = lEndPos;
        }


    // ----- accessors -----------------------------------------------------------------------------

    /**
     * @return the parent of the object being new'd, for example "parent.new Child()", or null if
     *         the object being new'd is of a top level class
     */
    public Expression getLeftExpression()
        {
        return left;
        }

    /**
     * @return the type of the object being new'd; never null
     */
    public TypeExpression getTypeExpression()
        {
        return type;
        }

    /**
     * @return return a list of expressions that are passed to the constructor of the object being
     *         instantiated, or null if there is no argument list specified
     */
    public List<Expression> getConstructorArguments()
        {
        return args;
        }

    /**
     * @return the body of the anonymous inner class, or null if this "new" is not instantiating an
     *         anonymous inner class
     */
    public StatementBlock getAnonymousInnerClassBody()
        {
        return body;
        }

    @Override
    public long getStartPosition()
        {
        return left == null ? operator.getStartPosition() : left.getStartPosition();
        }

    @Override
    public long getEndPosition()
        {
        return lEndPos;
        }

    @Override
    protected Field[] getChildFields()
        {
        return CHILD_FIELDS;
        }


    // ----- compilation ---------------------------------------------------------------------------

    @Override
    public TypeConstant getImplicitType(Context ctx)
        {
        if (body != null)
            {
            // TODO
            throw new UnsupportedOperationException("anonymous inner class type");
            }

        // REVIEW: if (left != null)
        return getTypeExpression().ensureTypeConstant();
        }

    @Override
    public TypeFit testFit(Context ctx, TypeConstant typeRequired, TuplePref pref)
        {
        // TODO there should be a default implementation of this
        TypeConstant typeThis = getImplicitType(ctx);
        if (typeThis == null)
            {
            return TypeFit.NoFit;
            }

        if (typeRequired == null || typeThis.isA(typeRequired))
            {
            return pref == TuplePref.Required
                    ? TypeFit.Pack
                    : TypeFit.Fit;
            }

        if (typeThis.getConverterTo(typeRequired) != null)
            {
            return pref == TuplePref.Required
                    ? TypeFit.ConvPack
                    : TypeFit.Conv;
            }

        return TypeFit.NoFit;
        }

    @Override
    protected Expression validate(Context ctx, TypeConstant typeRequired, TuplePref pref, ErrorListener errs)
        {
        boolean fValid = true;

        Expression   exprLeftOld = this.left;
        TypeConstant typeLeft    = null;
        if (exprLeftOld != null)
            {
            Expression exprLeftNew = exprLeftOld.validate(ctx, null, TuplePref.Rejected, errs);
            if (exprLeftNew == null)
                {
                fValid = false;
                }
            else
                {
                this.left = exprLeftNew;
                typeLeft  = exprLeftNew.getType();
                }
            }

        TypeExpression exprTypeOld   = this.type;
        TypeExpression exprTypeNew   = (TypeExpression) exprTypeOld.validate(ctx, typeRequired, pref, errs);
        TypeConstant   typeConstruct = null;
        TypeInfo       infoConstruct = null;
        if (exprTypeNew == null)
            {
            fValid = false;
            }
        else
            {
            this.type = exprTypeNew;

            typeConstruct = exprTypeNew.ensureTypeConstant();
            infoConstruct = typeConstruct.ensureTypeInfo(errs);

            // if the type is not new-able, then it must be an anonymous inner class with a body
            // that makes the type new-able
            if (body == null && !infoConstruct.isNewable())
                {
                log(errs, Severity.ERROR, Constants.VE_NEW_ILLEGAL_TYPE, typeConstruct.getValueString());
                fValid = false;
                }

            if (left != null)
                {
                // figure out the relationship between the type of "left" and the type being
                // constructed; they must both belong to the same "localized class tree", and the
                // type being instantiated must either be a static child class, the top level class,
                // or an instance class directly nested under the class specified by the "left" type
                // TODO detect & log errors: VE_NEW_REQUIRES_PARENT VE_NEW_DISALLOWS_PARENT VE_NEW_UNRELATED_PARENT
                throw new UnsupportedOperationException("instantiating child class not yet supported");
                }
            }

        List<Expression> listArgs   = this.args;
        int              cArgs      = listArgs.size();
        TypeConstant[]   atypeArgs  = cArgs == 0 ? TypeConstant.NO_TYPES : new TypeConstant[cArgs];
        String[]         asArgNames = null;
        for (int i = 0; i < cArgs; ++i)
            {
            Expression exprArgOld = listArgs.get(i);
            Expression exprArgNew = exprArgOld.validate(ctx, null, TuplePref.Rejected, errs);
            if (exprArgNew == null)
                {
                fValid = false;
                }
            else
                {
                if (exprArgNew != exprArgOld)
                    {
                    listArgs.set(i, exprArgNew);
                    }

                atypeArgs[i] = exprArgNew.getType();

                if (exprArgNew instanceof LabeledExpression)
                    {
                    String sName = ((LabeledExpression) exprArgNew).getName();

                    if (asArgNames == null)
                        {
                        asArgNames = new String[cArgs];
                        }
                    else
                        {
                        for (int iPrev = 0; iPrev < i; ++iPrev)
                            {
                            if (asArgNames[iPrev] != null && asArgNames[iPrev].equals(sName))
                                {
                                exprArgNew.log(errs, Severity.ERROR, Compiler.NAME_COLLISION, sName);
                                fValid = false;
                                }
                            }
                        }
                    asArgNames[i] = sName;
                    }
                }
            }

        if (body != null)
            {
            throw new UnsupportedOperationException("anonymous inner class not yet supported");
            }

        if (fValid)
            {
            // find the constructor to use
            m_idConstructor = infoConstruct.findCallable(
                    "construct", false, true, null, atypeArgs, asArgNames);
            if (m_idConstructor == null)
                {
                log(errs, Severity.ERROR, Compiler.MISSING_CONSTRUCTOR, typeConstruct.getValueString());
                fValid = false;
                }
            }

        return finishValidation(typeRequired, typeConstruct, fValid ? TypeFit.Fit : TypeFit.NoFit,
                null, errs);
        }

    @Override
    public Argument generateArgument(Code code, boolean fPack, boolean fLocalPropOk, boolean fUsedOnce, ErrorListener errs)
        {
        assert m_idConstructor != null;
        assert left == null; // TODO construct child class
        assert body == null; // TODO anonymous inner class

        List<Expression> listArgs = args;
        int              cArgs    = listArgs.size();
        Argument[]       aArgs    = new Argument[cArgs];
        for (int i = 0; i < cArgs; ++i)
            {
            aArgs[i] = listArgs.get(i).generateArgument(code, false, true, true, errs);
            }

        Argument argResult = new Register(getType());
        switch (cArgs)
            {
            case 0:
                code.add(new New_0(m_idConstructor, argResult));
                break;

            case 1:
                code.add(new New_1(m_idConstructor, aArgs[0], argResult));
                break;

            default:
                code.add(new New_N(m_idConstructor, aArgs, argResult));
                break;
            }
        return argResult;
        }

    @Override
    public void generateAssignment(Code code, Assignable LVal, ErrorListener errs)
        {
        assert m_idConstructor != null;
        assert left == null; // TODO construct child class
        assert body == null; // TODO anonymous inner class

        List<Expression> listArgs = args;
        int              cArgs    = listArgs.size();
        Argument[]       aArgs    = new Argument[cArgs];
        for (int i = 0; i < cArgs; ++i)
            {
            aArgs[i] = listArgs.get(i).generateArgument(code, false, true, true, errs);
            }

        Argument argResult = LVal.isLocalArgument()
                ? LVal.getLocalArgument()
                : new Register(LVal.getType());

        switch (cArgs)
            {
            case 0:
                code.add(new New_0(m_idConstructor, argResult));
                break;

            case 1:
                code.add(new New_1(m_idConstructor, aArgs[0], argResult));
                break;

            default:
                code.add(new New_N(m_idConstructor, aArgs, argResult));
                break;
            }

        if (!LVal.isLocalArgument())
            {
            LVal.assign(argResult, code, errs);
            }
        }


    // ----- debugging assistance ------------------------------------------------------------------

    /**
     * @return the signature of the contructor invocation
     */
    public String toSignatureString()
        {
        StringBuilder sb = new StringBuilder();

        if (left != null)
            {
            sb.append(left)
              .append('.');
            }

        sb.append(operator.getId().TEXT)
          .append(' ')
          .append(type);

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

        return sb.toString();
        }

    @Override
    public String toString()
        {
        StringBuilder sb = new StringBuilder();

        sb.append(toSignatureString());

        if (body != null)
            {
            sb.append('\n')
              .append(indentLines(body.toString(), "        "));
            }

        return sb.toString();
        }

    @Override
    public String getDumpDesc()
        {
        String s = toSignatureString();

        return body == null
                ? s
                : s + "{..}";
        }


    // ----- fields --------------------------------------------------------------------------------

    protected Expression       left;
    protected Token            operator;
    protected TypeExpression   type;
    protected List<Expression> args;
    protected StatementBlock   body;
    protected long             lEndPos;

    private transient MethodConstant m_idConstructor;

    private static final Field[] CHILD_FIELDS = fieldsForNames(NewExpression.class, "left", "type", "args", "body");
    }
