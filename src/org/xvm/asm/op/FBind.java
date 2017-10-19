package org.xvm.asm.op;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.xvm.asm.Constant;

import org.xvm.asm.OpCallable;
import org.xvm.asm.Register;
import org.xvm.runtime.CallChain;
import org.xvm.runtime.Frame;
import org.xvm.runtime.ObjectHandle;
import org.xvm.runtime.ObjectHandle.ExceptionHandle;

import org.xvm.runtime.Utils;
import org.xvm.runtime.template.Function;
import org.xvm.runtime.template.Function.FunctionHandle;

import static org.xvm.util.Handy.readPackedInt;
import static org.xvm.util.Handy.writePackedLong;


/**
 * FBIND rvalue-fn, #params:(param-index, rvalue-param), lvalue-fn-result
 */
public class FBind
        extends OpCallable
    {
    /**
     * Construct an FBIND op.
     *
     * @param nFunction     identifies the function to bind
     * @param anParamIx     identifies the parameter(s) to bind
     * @param anParamValue  identifies the values to use corresponding to those parameters
     * @param nRet          identifies where to place the bound function
     *
     * @deprecated
     */
    public FBind(int nFunction, int[] anParamIx, int[] anParamValue, int nRet)
        {
        super(nFunction);

        m_anParamIx = anParamIx;
        m_anParamValue = anParamValue;
        m_nResultValue = nRet;
        }

    /**
     * Construct a FBIND op based on the passed arguments.
     *
     * @param argFunction  the function Argument
     * @param anParamIx    the indexes of parameter(s) to bind (sorted in ascending order)
     * @param aArgValue    the array of Arguments to bind the values to
     * @param regReturn    the return Register
     */
    public FBind(Argument argFunction, int[] anParamIx, Argument[] aArgValue, Register regReturn)
        {
        super(argFunction);

        m_anParamIx = anParamIx;
        m_aArgParam = aArgValue;
        m_regReturn = regReturn;
        }

    /**
     * Deserialization constructor.
     *
     * @param in      the DataInput to read from
     * @param aconst  an array of constants used within the method
     */
    public FBind(DataInput in, Constant[] aconst)
            throws IOException
        {
        super(readPackedInt(in));

        int c = readPackedInt(in);

        m_anParamIx = new int[c];
        m_anParamValue = new int[c];

        for (int i = 0; i < c; i++)
            {
            m_anParamIx[i]    = readPackedInt(in);
            m_anParamValue[i] = readPackedInt(in);
            }
        m_nResultValue = readPackedInt(in);
        }

    @Override
    public void write(DataOutput out, ConstantRegistry registry)
            throws IOException
        {
        super.write(out, registry);

        if (m_aArgParam != null)
            {
            m_anParamValue = encodeArguments(m_aArgParam, registry);
            m_nResultValue = encodeArgument(m_regReturn, registry);
            }

        int c = m_anParamIx.length;
        writePackedLong(out, c);
        for (int i = 0; i < c; i++)
            {
            writePackedLong(out, m_anParamIx[i]);
            writePackedLong(out, m_anParamValue[i]);
            }
        writePackedLong(out, m_nResultValue);
        }

    @Override
    public int getOpCode()
        {
        return OP_FBIND;
        }

    @Override
    public int process(Frame frame, int iPC)
        {
        try
            {
            FunctionHandle hFunction;

            if (m_nFunctionValue == A_SUPER)
                {
                CallChain chain = frame.m_chain;
                if (chain == null)
                    {
                    throw new IllegalStateException();
                    }
                hFunction = Function.makeHandle(chain, frame.m_nDepth + 1);
                }
            else if (m_nFunctionValue < 0)
                {
                hFunction = Function.makeHandle(getMethodStructure(frame));
                }
            else
                {
                hFunction = (FunctionHandle) frame.getArgument(m_nFunctionValue);
                if (hFunction == null)
                    {
                    return R_REPEAT;
                    }
                }

            int cParams = m_anParamIx.length;
            ObjectHandle[] ahParam = new ObjectHandle[cParams];
            boolean fAnyProperty = false;

            for (int i = 0; i < cParams; i++)
                {
                ObjectHandle hParam = frame.getArgument(m_anParamValue[i]);
                if (hParam == null)
                    {
                    return R_REPEAT;
                    }
                ahParam[i] = hParam;
                fAnyProperty |= isProperty(hParam);
                }

            if (fAnyProperty)
                {
                Frame.Continuation stepNext = frameCaller ->
                    complete(frameCaller, hFunction, ahParam);

                return new Utils.GetArguments(ahParam, new int[]{0}, stepNext).doNext(frame);
                }

            return complete(frame, hFunction, ahParam);
            }
        catch (ExceptionHandle.WrapperException e)
            {
            return frame.raiseException(e);
            }
        }

    protected int complete(Frame frame, FunctionHandle hFunction, ObjectHandle[] ahParam)
        {
        // we assume that the indexes are sorted in the ascending order
        for (int i = 0, c = m_anParamIx.length; i < c; i++)
            {
            hFunction = hFunction.bind(m_anParamIx[i], ahParam[i]);
            }

        return frame.assignValue(m_nResultValue, hFunction);
        }

    @Override
    public void registerConstants(ConstantRegistry registry)
        {
        super.registerConstants(registry);

        registerArguments(m_aArgParam, registry);
        }

    private int[] m_anParamIx;
    private int[] m_anParamValue;
    private int   m_nResultValue;

    private Argument[] m_aArgParam;
    private Register m_regReturn;
    }
