package org.xvm.asm.op;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.xvm.asm.Op;

import org.xvm.runtime.Frame;
import org.xvm.runtime.ObjectHandle;
import org.xvm.runtime.ObjectHandle.ExceptionHandle;


/**
 * MOV rvalue-src, lvalue-dest
 *
 * @author gg 2017.03.08
 */
public class Move extends Op
    {
    final private int f_nFromValue;
    final private int f_nToValue;

    public Move(int nFrom, int nTo)
        {
        f_nToValue = nTo;
        f_nFromValue = nFrom;
        }

    public Move(DataInput in)
            throws IOException
        {
        f_nFromValue = in.readInt();
        f_nToValue = in.readInt();
        }

    @Override
    public void write(DataOutput out)
            throws IOException
        {
        out.write(OP_MOV);
        out.writeInt(f_nFromValue);
        out.writeInt(f_nToValue);
        }

    @Override
    public int process(Frame frame, int iPC)
        {
        try
            {
            ObjectHandle hValue = frame.getArgument(f_nFromValue);
            if (hValue == null)
                {
                return R_REPEAT;
                }

            return frame.assignValue(f_nToValue, hValue);
            }
        catch (ExceptionHandle.WrapperException e)
            {
            return frame.raiseException(e);
            }
        }
    }
