package org.xvm.asm.op;


import java.io.DataInput;
import java.io.IOException;

import org.xvm.asm.Argument;
import org.xvm.asm.Constant;
import org.xvm.asm.OpIndexInPlace;

import org.xvm.runtime.Frame;
import org.xvm.runtime.ObjectHandle;
import org.xvm.runtime.ObjectHandle.JavaLong;

import org.xvm.runtime.template.IndexSupport;


/**
 * IIP_SUB rvalue-target, rvalue-ix, rvalue2 ; T[ix] -= T
 */
public class IIP_Sub
        extends OpIndexInPlace
    {
    /**
     * Construct an IIP_SUB op for the passed target.
     *
     * @param argTarget  the target Argument
     * @param argIndex   the index Argument
     * @param argValue   the value Argument
     */
    public IIP_Sub(Argument argTarget, Argument argIndex, Argument argValue)
        {
        super(argTarget, argIndex, argValue);
        }

    /**
     * Deserialization constructor.
     *
     * @param in      the DataInput to read from
     * @param aconst  an array of constants used within the method
     */
    public IIP_Sub(DataInput in, Constant[] aconst)
            throws IOException
        {
        super(in, aconst);
        }

    @Override
    public int getOpCode()
        {
        return OP_IIP_SUB;
        }

    @Override
    protected int complete(Frame frame, ObjectHandle hTarget, JavaLong hIndex, ObjectHandle hValue)
        {
        IndexSupport template = (IndexSupport) hTarget.getOpSupport();
        long lIndex = hIndex.getValue();

        ObjectHandle hCurrent;
        switch (template.extractArrayValue(frame, hTarget, lIndex, A_STACK))
            {
            case R_NEXT:
                hCurrent = frame.popStack();
                break;

            case R_EXCEPTION:
                return R_EXCEPTION;

            default:
                // for now, virtual array ops are not supported
                throw new IllegalStateException();
            }

        switch (hCurrent.getOpSupport().invokeSub(frame, hCurrent, hValue, A_STACK))
            {
            case R_NEXT:
                return template.assignArrayValue(frame, hTarget, lIndex, frame.popStack());

            case R_CALL:
                frame.m_frameNext.setContinuation(frameCaller ->
                     template.assignArrayValue(frame, hTarget, lIndex, frame.popStack()));
                return R_CALL;

            case R_EXCEPTION:
                return R_EXCEPTION;

            default:
                throw new IllegalStateException();
            }
        }
    }