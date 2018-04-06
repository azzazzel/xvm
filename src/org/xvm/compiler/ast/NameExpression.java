package org.xvm.compiler.ast;


import java.lang.reflect.Field;

import java.util.Collections;
import java.util.List;

import org.xvm.asm.Constant;
import org.xvm.asm.ConstantPool;
import org.xvm.asm.ErrorListener;
import org.xvm.asm.MethodStructure.Code;
import org.xvm.asm.Op;
import org.xvm.asm.Op.Argument;
import org.xvm.asm.Register;

import org.xvm.asm.constants.ConditionalConstant;
import org.xvm.asm.constants.PropertyConstant;
import org.xvm.asm.constants.TypeConstant;
import org.xvm.asm.constants.UnresolvedNameConstant;

import org.xvm.compiler.Compiler;
import org.xvm.compiler.Token;
import org.xvm.compiler.Token.Id;

import org.xvm.compiler.ast.Statement.Context;

import org.xvm.util.Severity;


/**
 * A name expression specifies a name. This handles both a simple name, a qualified name, and a name
 * with type parameters.
 */
public class NameExpression
        extends Expression
    {
    // ----- constructors --------------------------------------------------------------------------

    public NameExpression(Token name)
        {
        this(Collections.singletonList(name), null, name.getEndPosition());
        }

    public NameExpression(List<Token> names, List<TypeExpression> params, long lEndPos)
        {
        this.names   = names;
        this.params  = params;
        this.lEndPos = lEndPos;
        }


    // ----- accessors -----------------------------------------------------------------------------

    @Override
    protected boolean usesSuper()
        {
        for (Token token : names)
            {
            if (token.getId() == Id.SUPER)
                {
                return true;
                }
            }
        return false;
        }

    @Override
    public boolean validateCondition(ErrorListener errs)
        {
        return isDotNameWithNoParams("present") || super.validateCondition(errs);
        }

    @Override
    public ConditionalConstant toConditionalConstant()
        {
        if (validateCondition(null))
            {
            ConstantPool pool = pool();
            return pool.ensurePresentCondition(new UnresolvedNameConstant(pool, getUpToDotName(), false));
            }

        return super.toConditionalConstant();
        }

    /**
     * Determine if the expression is a multi-part, dot-delimited name that has no type params.
     *
     * @param sName  the last name of the expression must match this name
     *
     * @return true iff the expression is a multi-part, dot-delimited name that has no type params,
     *         with the last part of the name matching the specified name
     */
    protected boolean isDotNameWithNoParams(String sName)
        {
        List<Token> names  = this.names;
        int         cNames = names.size();
        return cNames > 1 && names.get(cNames-1).getValue().equals(sName) && (params == null || params.isEmpty());
        }

    /**
     * Get all of the names in the expression except the last one.
     *
     * @return an array of names
     */
    protected String[] getUpToDotName()
        {
        List<Token> listNames = this.names;
        int         cNames    = listNames.size() - 1;
        String[]    aNames    = new String[cNames];
        for (int i = 0; i < cNames; ++i)
            {
            aNames[i] = (String) listNames.get(i).getValue();
            }
        return aNames;
        }

    /**
     * @return the number of dot-delimited names in the expression
     */
    public int getNameCount()
        {
        return names.size();
        }

    /**
     * @param i  the index of the name to obtain from the dot-delimited names in the expression
     *
     * @return the i-th name in the expression
     */
    String getName(int i)
        {
        return (String) names.get(i).getValue();
        }

    @Override
    public long getStartPosition()
        {
        return names.get(0).getStartPosition();
        }

    @Override
    public long getEndPosition()
        {
        return lEndPos;
        }

    public boolean isSpecial()
        {
        for (Token name : names)
            {
            if (name.isSpecial())
                {
                return true;
                }
            }
        return false;
        }

    @Override
    public TypeExpression toTypeExpression()
        {
        return new NamedTypeExpression(null, names, null, null, params, lEndPos);
        }

    @Override
    protected Field[] getChildFields()
        {
        return CHILD_FIELDS;
        }


    // ----- compilation ---------------------------------------------------------------------------


    @Override
    protected Expression validate(Context ctx, TypeConstant typeRequired, TuplePref pref, ErrorListener errs)
        {
        boolean fValid = true;

        // a name expression has params from the construct:
        //      QualifiedNameName TypeParameterTypeList-opt
        ConstantPool pool = pool();
        if (params != null)
            {
            for (int i = 0, c = params.size(); i < c; ++i)
                {
                TypeExpression typeOld = params.get(i);
                TypeExpression typeNew = (TypeExpression) typeOld.validate(
                        ctx, pool.typeType(), TuplePref.Rejected, errs);
                fValid &= typeNew != null;
                if (typeNew != typeOld && typeNew != null)
                    {
                    params.set(i, typeNew);
                    }
                }
            }

        // resolve the initial name
        Token        name  = names.get(0);
        String       sName = name.getValue().toString();
        Argument     arg   = ctx.resolveName(name, errs);
        if (arg == null)
            {
            log(errs, Severity.ERROR, org.xvm.compiler.Compiler.NAME_MISSING,
                    sName, ctx.getMethod().getIdentityConstant().getSignature());
            fValid = false;
            }

        // if anything has failed already, we won't be able to complete the validation
        if (!fValid)
            {
            finishValidation(TypeFit.NoFit, typeRequired, arg instanceof Constant ? (Constant) arg : null);
            return null;
            }

        // resolve the name to an argument, and determine assignability
        if (names.size() == 1)
            {
            // TODO incorporate params, if any
            m_arg         = arg;
            m_fAssignable = ctx.isVarWritable(sName); // TODO: handle properties
            }
        else
            {
            // TODO resolve subsequent names
            notImplemented();
            }

        TypeConstant type = arg.getRefType();

        // validate that the expression can be of the required type
        TypeFit fit = TypeFit.Fit;
        if (fValid && typeRequired != null && !type.isA(typeRequired))
            {
            // check if conversion in required
            if ()
                {
                // TODO
                }
            else
                {
                arg.getRefType()
                if (arg != null && !arg.getRefType().isAssignableTo(typeRequired))
                    {
                    log(errs, Severity.ERROR, Compiler.WRONG_TYPE, typeRequired, arg.getRefType());
                    fValid = false;
                    }
                }

            }

        if (!fValid)
            {
            // if there's any problem computing the type, and the expression is already invalid,
            // then just agree to whatever was asked
            if (typeRequired != null)
                {
                type = typeRequired;
                }

            fit = TypeFit.NoFit;
            }
        else  if (pref == TuplePref.Required)
            {
            fit = fit.addPack();
            }

        boolean fConstant = m_arg != null && m_arg instanceof Constant && !m_fAssignable;
        finishValidation(fit, type, fConstant ? (Constant) m_arg : null);

        return fValid
                ? this
                : null;
        }

    @Override
    public TypeConstant getType()
        {
        return m_type == null
                ? m_arg == null
                        ? pool().typeObject()
                        : m_arg.getRefType()
                : m_type;
        }

    @Override
    public boolean isAssignable()
        {
        return m_fAssignable;
        }

    @Override
    public boolean isConstant()
        {
        return m_arg != null && m_arg instanceof Constant && !isAssignable();
        }

    @Override
    public Constant toConstant()
        {
        assert isConstant();
        return (Constant) m_arg;
        }

    @Override
    public Argument generateArgument(Code code, TypeConstant type, boolean fTupleOk, ErrorListener errs)
        {
        if (m_arg == null)
            {
            // TODO log error
            return generateBlackHole(type);
            }

        return isConstant()
                ? super.generateArgument(code, type, fTupleOk, errs)
                : validateAndConvertSingle(m_arg, code, type, errs);
        }

    @Override
    public Assignable generateAssignable(Code code, ErrorListener errs)
        {
        Assignable LVal = m_LVal;
        if (LVal == null && isAssignable())
            {
            if (m_arg instanceof Register)
                {
                LVal = new Assignable((Register) m_arg);
                }
            else if (m_arg instanceof PropertyConstant)
                {
                // TODO: use getThisClass().toTypeConstant() for a type
                LVal = new Assignable(
                    new Register(pool().typeObject(), Op.A_TARGET),
                    (PropertyConstant) m_arg);
                }
            else
                {
                LVal = super.generateAssignable(code, errs);
                }
            m_LVal = LVal;
            }

        return LVal;
        }

    // ----- debugging assistance ------------------------------------------------------------------

    @Override
    public String toString()
        {
        StringBuilder sb = new StringBuilder();

        boolean first = true;
        for (Token token : names)
            {
            if (first)
                {
                first = false;
                }
            else
                {
                sb.append('.');
                }
            sb.append(token.getValue());
            }

        if (params != null)
            {
            sb.append('<');
            first = true;
            for (Expression param : params)
                {
                if (first)
                    {
                    first = false;
                    }
                else
                    {
                    sb.append(", ");
                    }
                sb.append(param);
                }
            sb.append('>');
            }

        return sb.toString();
        }

    @Override
    public String getDumpDesc()
        {
        return toString();
        }


    // ----- fields --------------------------------------------------------------------------------

    protected List<Token>          names;
    protected List<TypeExpression> params;
    protected long                 lEndPos;

    private Argument     m_arg;
    private Assignable   m_LVal;
    private boolean      m_fAssignable;
    private TypeConstant m_type;

    private static final Field[] CHILD_FIELDS = fieldsForNames(NameExpression.class, "params");
    }
