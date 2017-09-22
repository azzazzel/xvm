package org.xvm.asm.op;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.xvm.asm.OpCallable;

import org.xvm.runtime.Frame;
import org.xvm.runtime.ObjectHandle.ExceptionHandle;

import org.xvm.runtime.template.xBoolean.BooleanHandle;
import org.xvm.runtime.template.xException;


/**
 * ASSERT rvalue
 *
 * @author gg 2017.03.08
 */
public class Assert extends OpCallable
    {
    private final int f_nValue;

    public Assert(int nValue)
        {
        f_nValue = nValue;
        }

    public Assert(DataInput in)
            throws IOException
        {
        f_nValue = in.readInt();
        }

    @Override
    public void write(DataOutput out)
            throws IOException
        {
        out.write(OP_ASSERT);
        out.writeInt(f_nValue);
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

            if (hTest.get())
                {
                return iPC + 1;
                }

            return frame.raiseException(xException.makeHandle("Assertion failed"));
            }
        catch (ExceptionHandle.WrapperException e)
            {
            return frame.raiseException(e);
            }
        }
    }
