package org.xvm.asm.op;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.xvm.asm.OpCallable;

import org.xvm.asm.constants.StringConstant;

import org.xvm.runtime.Frame;
import org.xvm.runtime.ObjectHandle.ExceptionHandle;

import org.xvm.runtime.template.xBoolean.BooleanHandle;
import org.xvm.runtime.template.xException;


/**
 * ASSERT rvalue, CONST_STRING
 *
 * @author gg 2017.03.08
 */
public class AssertT extends OpCallable
    {
    private final int f_nValue;
    private final int f_nTextConstId;

    public AssertT(int nValue, int nTextId)
        {
        f_nValue = nValue;
        f_nTextConstId = nTextId;
        }

    public AssertT(DataInput in)
            throws IOException
        {
        f_nValue = in.readInt();
        f_nTextConstId = in.readInt();
        }

    @Override
    public void write(DataOutput out)
            throws IOException
        {
        out.write(OP_ASSERT_T);
        out.writeInt(f_nValue);
        out.writeInt(f_nTextConstId);
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

            StringConstant constText = (StringConstant)
                    frame.f_context.f_pool.getConstant(-f_nTextConstId);

            return frame.raiseException(
                    xException.makeHandle("Assertion failed: " + constText.getValueString()));
            }
        catch (ExceptionHandle.WrapperException e)
            {
            return frame.raiseException(e);
            }
        }
    }
