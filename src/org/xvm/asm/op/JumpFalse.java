package org.xvm.asm.op;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.xvm.asm.Op;

import org.xvm.runtime.Frame;
import org.xvm.runtime.ObjectHandle.ExceptionHandle;

import org.xvm.runtime.template.xBoolean.BooleanHandle;


/**
 * JMP_FALSE rvalue-bool, rel-addr ; jump if value is false
 *
 * @author gg 2017.03.08
 */
public class JumpFalse extends Op
    {
    private final int f_nValue;
    private final int f_nRelAddr;

    public JumpFalse(int nValue, int nRelAddr)
        {
        f_nValue = nValue;
        f_nRelAddr = nRelAddr;
        }

    public JumpFalse(DataInput in)
            throws IOException
        {
        f_nValue = in.readInt();
        f_nRelAddr = in.readInt();
        }

    @Override
    public void write(DataOutput out)
            throws IOException
        {
        out.write(OP_JMP_FALSE);
        out.writeInt(f_nValue);
        out.writeInt(f_nRelAddr);
        }

    @Override
    public int process(Frame frame, int iPC)
        {
        try
            {
            BooleanHandle hTest = (BooleanHandle) frame.getArgument(f_nValue);
            if (hTest == null)
                {
                return R_REPEAT;
                }

            return hTest.get() ? iPC + 1 : iPC + f_nRelAddr;
            }
        catch (ExceptionHandle.WrapperException e)
            {
            return frame.raiseException(e);
            }
        }
    }
