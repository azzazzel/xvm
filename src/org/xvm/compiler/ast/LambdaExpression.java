package org.xvm.compiler.ast;


import java.lang.reflect.Field;

import java.util.ArrayList;
import java.util.List;

import org.xvm.asm.Component;
import org.xvm.asm.ErrorListener;
import org.xvm.asm.MethodStructure;
import org.xvm.asm.MethodStructure.Code;
import org.xvm.asm.MultiMethodStructure;

import org.xvm.asm.constants.TypeConstant;

import org.xvm.compiler.Compiler;
import org.xvm.compiler.Token;

import org.xvm.compiler.Token.Id;
import org.xvm.compiler.ast.Statement.CaptureContext;
import org.xvm.compiler.ast.Statement.Context;

import org.xvm.util.Severity;

import static org.xvm.util.Handy.indentLines;


/**
 * Lambda expression is an inlined function. This version uses parameters that are assumed to be
 * names only.
 */
public class LambdaExpression
        extends Expression
    {
    // ----- constructors --------------------------------------------------------------------------

    /**
     *
     * @param params     either a list of Expression objects or a list of Parameter objects
     * @param operator
     * @param body
     * @param lStartPos
     */
    public LambdaExpression(List params, Token operator, StatementBlock body, long lStartPos)
        {
        if (!params.isEmpty() && params.get(0) instanceof Expression)
            {
            assert params.stream().allMatch(Expression.class::isInstance);
            this.paramNames = params;
            }
        else
            {
            assert params.stream().allMatch(org.xvm.asm.Parameter.class::isInstance);
            this.params = params;
            }

        this.operator  = operator;
        this.body      = body;
        this.lStartPos = lStartPos;
        }

    // ----- accessors -----------------------------------------------------------------------------

    @Override
    public Component getComponent()
        {
        MethodStructure method = m_lambda;
        return method == null
                ? super.getComponent()
                : method;
        }

    @Override
    public long getStartPosition()
        {
        return lStartPos;
        }

    @Override
    public long getEndPosition()
        {
        return body.getEndPosition();
        }

    @Override
    protected Field[] getChildFields()
        {
        return CHILD_FIELDS;
        }


    // ----- compilation ---------------------------------------------------------------------------

    @Override
    protected void registerStructures(StageMgr mgr, ErrorListener errs)
        {
        checkDebug();

        if (m_lambda == null)
            {
            TypeConstant[] atypes   = null;
            String[]       asParams = null;
            if (paramNames == null)
                {
                // build an array of types and an array of names
                int cParams = params == null ? 0 : params.size();
                atypes   = new TypeConstant[cParams];
                asParams = new String[cParams];
                for (int i = 0; i < cParams; ++i)
                    {
                    Parameter param = params.get(i);
                    atypes  [i] = param.getType().ensureTypeConstant();
                    asParams[i] = param.getName();
                    }
                }
            else
                {
                // build an array of names
                int cParams = paramNames.size();
                asParams = new String[cParams];
                for (int i = 0; i < cParams; ++i)
                    {
                    Expression expr = paramNames.get(i);
                    if (expr instanceof NameExpression)
                        {
                        // note: could also be an IgnoredNameExpression
                        asParams[i] = ((NameExpression) expr).getName();
                        }
                    else
                        {
                        expr.log(errs, Severity.ERROR, Compiler.NAME_REQUIRED);
                        asParams[i] = Id.IGNORED.TEXT;
                        }
                    }
                }

            Component            container = getParent().getComponent();
            MultiMethodStructure structMM  = container.ensureMultiMethodStructure(METHOD_NAME);
            MethodStructure      lambda    = structMM.createLambda(atypes, asParams);
            // TODO
            m_lambda = lambda;
            }

        super.registerStructures(mgr, errs);
        }

    @Override
    public void resolveNames(StageMgr mgr, ErrorListener errs)
        {
        checkDebug();

        super.resolveNames(mgr, errs);
        }

    @Override
    public void validateExpressions(StageMgr mgr, ErrorListener errs)
        {
        checkDebug();

        super.validateExpressions(mgr, errs);
        }

    @Override
    public void generateCode(StageMgr mgr, ErrorListener errs)
        {
        checkDebug();

        super.generateCode(mgr, errs);
        }

    @Override
    public TypeConstant getImplicitType(Context ctx)
        {
        checkDebug(); // TODO remove

        assert m_typeRequired == null && m_listRetTypes == null;
        if (isValidated())
            {
            return getType();
            }

        // clone the body (to avoid damaging the original) and validate it to calculate its type
        StatementBlock blockTemp = (StatementBlock) body.clone();

        // the resulting returned types come back in m_listRetTypes
        if (blockTemp.validate(ctx.createCaptureContext(blockTemp), ErrorListener.BLACKHOLE) == null
                || m_listRetTypes == null)
            {
            m_listRetTypes = null;
            return null;
            }

        TypeConstant[] aTypes = m_listRetTypes.toArray(new TypeConstant[m_listRetTypes.size()]);
        m_listRetTypes = null;
        return ListExpression.inferCommonType(aTypes);
        }

    @Override
    public TypeFit testFit(Context ctx, TypeConstant typeRequired)
        {
        checkDebug(); // TODO remove

        assert m_typeRequired == null && m_listRetTypes == null;
        if (isValidated())
            {
            return getType().isA(typeRequired)
                    ? TypeFit.Fit
                    : TypeFit.NoFit;
            }

        // clone the body and validate it using the requested type to test if that type will work
        m_typeRequired = typeRequired;
        StatementBlock blockTemp = (StatementBlock) body.clone();
        blockTemp.validate(ctx.createCaptureContext(blockTemp), ErrorListener.BLACKHOLE);
        m_typeRequired = null;

        // the resulting returned types come back in m_listRetTypes
        if (m_listRetTypes == null)
            {
            return TypeFit.NoFit;
            }
        else
            {
            TypeConstant[] aTypes = m_listRetTypes.toArray(new TypeConstant[m_listRetTypes.size()]);
            m_listRetTypes = null;

            // calculate the resulting type
            TypeConstant typeResult = ListExpression.inferCommonType(aTypes);
            return typeResult != null && typeResult.isA(typeRequired)
                    ? TypeFit.Fit
                    : TypeFit.NoFit;
            }
        }


    @Override
    protected Expression validate(Context ctx, TypeConstant typeRequired, ErrorListener errs)
        {
        checkDebug(); // TODO remove

        assert m_typeRequired == null && m_listRetTypes == null;
        m_typeRequired = typeRequired;

        TypeFit        fit     = TypeFit.Fit;
        StatementBlock bodyOld = body;
        CaptureContext ctxNew  = ctx.createCaptureContext(body);
        StatementBlock bodyNew = (StatementBlock) bodyOld.validate(ctxNew, errs);
        if (bodyNew == null)
            {
            fit = TypeFit.NoFit;
            }
        else
            {
            body = bodyNew;
            }

        TypeConstant typeActual = null;
        if (m_listRetTypes != null)
            {
            TypeConstant[] aTypes = m_listRetTypes.toArray(new TypeConstant[m_listRetTypes.size()]);
            m_listRetTypes = null;
            typeActual = ListExpression.inferCommonType(aTypes);
            }
        if (typeActual == null)
            {
            fit = TypeFit.NoFit;
            }

        return finishValidation(typeRequired, typeActual, fit, null, errs);
        }

    @Override
    public void generateAssignment(Context ctx, Code code, Assignable LVal, ErrorListener errs)
        {
        checkDebug();

        // TODO somehow, at the end of validate (after all the various things that could happen could happen, i.e. all assignments before & after this),
        // TODO ... we build the lambda signature, and set it on the MethodConstant
        // TODO
        super.generateAssignment(ctx, code, LVal, errs);
        }


    // TODO temp
    static LambdaExpression exprDebug;
    void checkDebug()
        {
        if (exprDebug == null && !getComponent().getIdentityConstant().getModuleConstant().toString().contains("Ecstasy"))
            {
            exprDebug = this;
            }

        if (this == exprDebug)
            {
            String s = toString();
            }
        }

    // ----- compilation helpers -------------------------------------------------------------------

    /**
     * @return the required type, which is the specified required type during validation, or the
     *         actual type once the expression is validatd
     */
    TypeConstant getRequiredType()
        {
        return isValidated() ? getType() : m_typeRequired;
        }

    void addReturnType(TypeConstant typeRet)
        {
        List<TypeConstant> list = m_listRetTypes;
        if (list == null)
            {
            m_listRetTypes = list = new ArrayList<>();
            }
        list.add(typeRet);
        }


    // ----- debugging assistance ------------------------------------------------------------------

    public String toSignatureString()
        {
        StringBuilder sb = new StringBuilder();

        sb.append('(');
        boolean first = true;
        for (Object param : (params == null ? paramNames : params))
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

        sb.append(')')
          .append(' ')
          .append(operator.getId().TEXT);

        return sb.toString();
        }

    @Override
    public String toString()
        {
        StringBuilder sb = new StringBuilder();

        sb.append(toSignatureString());

        String s = body.toString();
        if (s.indexOf('\n') >= 0)
            {
            sb.append('\n')
              .append(indentLines(s, "    "));
            }
        else
            {
            sb.append(' ')
              .append(s);
            }

        return sb.toString();
        }

    @Override
    public String toDumpString()
        {
        return toSignatureString() + " {...}";
        }


    // ----- fields --------------------------------------------------------------------------------

    public static final String METHOD_NAME = "->";

    protected List<Parameter>  params;
    protected List<Expression> paramNames;
    protected Token            operator;
    protected StatementBlock   body;
    protected long             lStartPos;

    private MethodStructure m_lambda;

    private transient TypeConstant       m_typeRequired;
    private transient List<TypeConstant> m_listRetTypes;

    private static final Field[] CHILD_FIELDS = fieldsForNames(LambdaExpression.class, "params", "paramNames", "body");
    }
