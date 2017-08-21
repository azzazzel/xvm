package org.xvm.proto.op;

import org.xvm.asm.MethodStructure;

import org.xvm.proto.Frame;
import org.xvm.proto.ObjectHandle;
import org.xvm.proto.ObjectHandle.ExceptionHandle;
import org.xvm.proto.OpCallable;

import org.xvm.proto.template.Function.FunctionHandle;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * CALL_N1 rvalue-function, #params:(rvalue) lvalue-return
 *
 * @author gg 2017.03.08
 */
public class Call_N1 extends OpCallable
    {
    private final int f_nFunctionValue;
    private final int[] f_anArgValue;
    private final int f_nRetValue;

    public Call_N1(int nFunction, int[] anArg, int nRet)
        {
        f_nFunctionValue = nFunction;
        f_anArgValue = anArg;
        f_nRetValue = nRet;
        }

    public Call_N1(DataInput in)
            throws IOException
        {
        f_nFunctionValue = in.readInt();
        f_anArgValue = readIntArray(in);
        f_nRetValue = in.readInt();
        }

    @Override
    public void write(DataOutput out)
            throws IOException
        {
        out.write(OP_CALL_N1);
        out.writeInt(f_nFunctionValue);
        writeIntArray(out, f_anArgValue);
        out.writeInt(f_nRetValue);
        }

    @Override
    public int process(Frame frame, int iPC)
        {
        try
            {
            if (f_nFunctionValue == A_SUPER)
                {
                return callSuperN1(frame, f_anArgValue, f_nRetValue);
                }

            if (f_nFunctionValue < 0)
                {
                MethodStructure function = getMethodStructure(frame, -f_nFunctionValue);

                ObjectHandle[] ahVar = frame.getArguments(f_anArgValue, frame.f_adapter.getVarCount(function));
                if (ahVar == null)
                    {
                    return R_REPEAT;
                    }

                return frame.call1(function, null, ahVar, f_nRetValue);
                }

            FunctionHandle hFunction = (FunctionHandle) frame.getArgument(f_nFunctionValue);
            if (hFunction == null)
                {
                return R_REPEAT;
                }

            ObjectHandle[] ahVar = frame.getArguments(f_anArgValue, hFunction.getVarCount());
            if (ahVar == null)
                {
                return R_REPEAT;
                }

            return hFunction.call1(frame, null, ahVar, f_nRetValue);
            }
        catch (ExceptionHandle.WrapperException e)
            {
            frame.m_hException = e.getExceptionHandle();
            return R_EXCEPTION;
            }
        }
    }