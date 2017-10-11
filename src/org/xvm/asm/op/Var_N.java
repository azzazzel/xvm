package org.xvm.asm.op;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.xvm.asm.Constant;
import org.xvm.asm.OpVar;

import org.xvm.asm.constants.StringConstant;
import org.xvm.asm.constants.TypeConstant;
import org.xvm.runtime.Frame;

import static org.xvm.util.Handy.readPackedInt;
import static org.xvm.util.Handy.writePackedLong;


/**
 * VAR_N TYPE, STRING ; (next register is an uninitialized named variable)
 */
public class Var_N
        extends OpVar
    {
    /**
     * Construct a VAR_N op.
     *
     * @param nType     the variable type id
     * @param nNameId   the name of the variable id
     */
    public Var_N(int nType, int nNameId)
        {
        super(nType);

        m_nNameId = nNameId;
        }

    /**
     * Construct a VAR_N op for the specified type and name.
     *
     * @param constType  the variable type
     * @param constName  the name constant
     */
    public Var_N(TypeConstant constType, StringConstant constName)
        {
        super(constType);

        if (constName == null)
            {
            throw new IllegalArgumentException("name required");
            }
        m_constName = constName;
        }

    /**
     * Deserialization constructor.
     *
     * @param in      the DataInput to read from
     * @param aconst  an array of constants used within the method
     */
    public Var_N(DataInput in, Constant[] aconst)
            throws IOException
        {
        super(readPackedInt(in));

        m_nNameId = readPackedInt(in);
        }

    @Override
    public void write(DataOutput out, ConstantRegistry registry)
            throws IOException
        {
        super.write(out, registry);

        if (m_constName != null)
            {
            m_nNameId = encodeArgument(m_constName, registry);
            }
        writePackedLong(out, m_nNameId);
        }

    @Override
    public int getOpCode()
        {
        return OP_VAR_N;
        }

    @Override
    public int process(Frame frame, int iPC)
        {
        frame.introduceVar(m_nType, m_nNameId, Frame.VAR_STANDARD, null);

        return iPC + 1;
        }

    @Override
    public void registerConstants(ConstantRegistry registry)
        {
        super.registerConstants(registry);

        registerArgument(m_constName, registry);
        }

    private int m_nNameId;

    private StringConstant m_constName;
    }
