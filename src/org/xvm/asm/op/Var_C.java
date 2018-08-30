package org.xvm.asm.op;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.xvm.asm.Argument;
import org.xvm.asm.Constant;
import org.xvm.asm.OpVar;
import org.xvm.asm.Register;

import org.xvm.runtime.Frame;
import org.xvm.runtime.ObjectHandle.ExceptionHandle;

import org.xvm.runtime.template.xRef.RefHandle;

import static org.xvm.util.Handy.readPackedInt;
import static org.xvm.util.Handy.writePackedLong;


/**
 * VAR_C rvalue-src ; (next register is a local variable representation of the specified Ref/Var)
 */
public class Var_C
        extends OpVar
    {
    /**
     * Construct a VAR_C op for the specified register and argument.
     *
     * @param reg       the register
     * @param argValue  the value argument of type Ref or Var
     */
    public Var_C(Register reg, Argument argValue)
        {
        super(reg);

        if (argValue == null)
            {
            throw new IllegalArgumentException("value required");
            }

        if (!argValue.getType().isA(argValue.getType().getConstantPool().typeRef()))
            {
            throw new IllegalArgumentException("value must be a Ref or Var");
            }

        m_argValue = argValue;
        }

    /**
     * Deserialization constructor.
     *
     * @param in      the DataInput to read from
     * @param aconst  an array of constants used within the method
     */
    public Var_C(DataInput in, Constant[] aconst)
            throws IOException
        {
        super(in, aconst);

        m_nArgValue = readPackedInt(in);
        }

    @Override
    public void write(DataOutput out, ConstantRegistry registry)
            throws IOException
        {
        super.write(out, registry);

        if (m_argValue != null)
            {
            m_nArgValue = encodeArgument(m_argValue, registry);
            }

        writePackedLong(out, m_nArgValue);
        }

    @Override
    public int getOpCode()
        {
        return OP_VAR_C;
        }

    @Override
    public int process(Frame frame, int iPC)
        {
        try
            {
            RefHandle hRef = (RefHandle) frame.getArgument(m_nArgValue);

            frame.introduceResolvedVar(m_nVar, hRef.getDeclaredType(),
                hRef.getName(), Frame.VAR_DYNAMIC_REF, hRef);

            return iPC + 1;
            }
        catch (ExceptionHandle.WrapperException e)
            {
            return frame.raiseException(e);
            }
        }

    @Override
    public void registerConstants(ConstantRegistry registry)
        {
        super.registerConstants(registry);

        m_argValue = registerArgument(m_argValue, registry);
        }

    @Override
    public String toString()
        {
        return super.toString()
                + ' ' + Argument.toIdString(m_argValue, m_nArgValue);
        }

    private int m_nArgValue;

    private Argument m_argValue;
    }
