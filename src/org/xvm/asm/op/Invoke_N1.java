package org.xvm.asm.op;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.xvm.asm.Constant;
import org.xvm.asm.OpInvocable;
import org.xvm.asm.Register;

import org.xvm.asm.constants.MethodConstant;

import org.xvm.runtime.CallChain;
import org.xvm.runtime.Frame;
import org.xvm.runtime.ObjectHandle;
import org.xvm.runtime.ObjectHandle.ExceptionHandle;
import org.xvm.runtime.TypeComposition;
import org.xvm.runtime.Utils;

import static org.xvm.util.Handy.readPackedInt;
import static org.xvm.util.Handy.writePackedLong;


/**
 * NVOK_N1  rvalue-target, CONST-METHOD, #params:(rvalue), lvalue-return
 */
public class Invoke_N1
        extends OpInvocable
    {
    /**
     * Construct an NVOK_N1 op.
     *
     * @param nTarget    r-value that specifies the object on which the method being invoked
     * @param nMethodId  r-value that specifies the method being invoked
     * @param anArg      the r-value locations of the method arguments
     * @param nRet       the l-value location for the result
     *
     * @deprecated
     */
    public Invoke_N1(int nTarget, int nMethodId, int[] anArg, int nRet)
        {
        super(nTarget, nMethodId);

        m_anArgValue = anArg;
        m_nRetValue = nRet;
        }

    /**
     * Construct an NVOK_N1 op based on the passed arguments.
     *
     * @param argTarget    the target Argument
     * @param constMethod  the method constant
     * @param aArgValue    the array of Argument values
     * @param regReturn    the Register to move the result into
     */
    public Invoke_N1(Argument argTarget, MethodConstant constMethod, Argument[] aArgValue, Register regReturn)
        {
        super(argTarget, constMethod);

        m_aArgValue   = aArgValue;
        m_regReturn   = regReturn;
        }

    /**
     * Deserialization constructor.
     *
     * @param in      the DataInput to read from
     * @param aconst  an array of constants used within the method
     */
    public Invoke_N1(DataInput in, Constant[] aconst)
            throws IOException
        {
        super(readPackedInt(in), readPackedInt(in));

        m_anArgValue = readIntArray(in);
        m_nRetValue = readPackedInt(in);
        }

    @Override
    public void write(DataOutput out, ConstantRegistry registry)
            throws IOException
        {
        super.write(out, registry);

        if (m_aArgValue != null)
            {
            m_anArgValue = encodeArguments(m_aArgValue, registry);
            m_nRetValue  = encodeArgument(m_regReturn, registry);
            }

        writeIntArray(out, m_anArgValue);
        writePackedLong(out, m_nRetValue);
        }

    @Override
    public int getOpCode()
        {
        return OP_NVOK_N1;
        }

    @Override
    public int process(Frame frame, int iPC)
        {
        try
            {
            ObjectHandle hTarget = frame.getArgument(m_nTarget);
            if (hTarget == null)
                {
                return R_REPEAT;
                }

            if (isProperty(hTarget))
                {
                ObjectHandle[] ahArg = frame.getArguments(m_anArgValue, m_anArgValue.length);
                if (ahArg == null)
                    {
                    return R_REPEAT;
                    }

                ObjectHandle[] ahTarget = new ObjectHandle[] {hTarget};
                Frame.Continuation stepNext = frameCaller -> resolveArgs(frameCaller, ahTarget[0], ahArg);

                return new Utils.GetArgument(ahTarget, stepNext).doNext(frame);
                }

            return resolveArgs(frame, hTarget, null);
            }
        catch (ExceptionHandle.WrapperException e)
            {
            return frame.raiseException(e);
            }
        }

    protected int resolveArgs(Frame frame, ObjectHandle hTarget, ObjectHandle[] ahArg)
        {
        CallChain chain = getCallChain(frame, hTarget.f_clazz);

        ObjectHandle[] ahVar;
        if (ahArg == null)
            {
            try
                {
                ahVar = frame.getArguments(m_anArgValue, chain.getTop().getMaxVars());
                if (ahVar == null)
                    {
                    return R_REPEAT;
                    }
                }
            catch (ExceptionHandle.WrapperException e)
                {
                return frame.raiseException(e);
                }
            }
        else
            {
            ahVar = Utils.ensureSize(ahArg, chain.getTop().getMaxVars());
            }

        if (anyProperty(ahVar))
            {
            Frame.Continuation stepNext =
                frameCaller -> complete(frameCaller, chain, hTarget, ahVar);
            return new Utils.GetArguments(ahVar, new int[]{0}, stepNext).doNext(frame);
            }
        return complete(frame, chain, hTarget, ahVar);
        }

    protected int complete(Frame frame, CallChain chain, ObjectHandle hTarget, ObjectHandle[] ahVar)
        {
        TypeComposition clz = hTarget.f_clazz;

        return chain.isNative()
             ? clz.f_template.invokeNativeN(frame, chain.getTop(), hTarget, ahVar, m_nRetValue)
             : clz.f_template.invoke1(frame, chain, hTarget, ahVar, m_nRetValue);
        }

    @Override
    public void registerConstants(ConstantRegistry registry)
        {
        super.registerConstants(registry);

        registerArguments(m_aArgValue, registry);
        }

    private int[] m_anArgValue;
    private int   m_nRetValue;

    private Argument[] m_aArgValue;
    private Register   m_regReturn;
    }
