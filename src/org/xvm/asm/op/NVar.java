package org.xvm.asm.op;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.xvm.asm.Op;

import org.xvm.asm.constants.StringConstant;

import org.xvm.runtime.Frame;
import org.xvm.runtime.ServiceContext;
import org.xvm.runtime.TypeComposition;


/**
 * NVAR CONST_CLASS, CONST_STRING ; (next register is an uninitialized named variable)
 *
 * @author gg 2017.03.08
 */
public class NVar extends Op
    {
    private final int f_nClassConstId;
    private final int f_nNameConstId;

    public NVar(int nClassConstId, int nNameConstId)
        {
        f_nClassConstId = nClassConstId;
        f_nNameConstId = nNameConstId;
        }

    public NVar(DataInput in)
            throws IOException
        {
        f_nClassConstId = in.readInt();
        f_nNameConstId = in.readInt();
        }

    @Override
    public void write(DataOutput out)
            throws IOException
        {
        out.write(OP_NVAR);
        out.writeInt(f_nClassConstId);
        out.writeInt(f_nNameConstId);
        }

    @Override
    public int process(Frame frame, int iPC)
        {
        ServiceContext context = frame.f_context;

        TypeComposition clazz = context.f_types.ensureComposition(
                f_nClassConstId, frame.getActualTypes());
        StringConstant constName = (StringConstant)
                context.f_pool.getConstant(f_nNameConstId);

        frame.introduceVar(clazz, constName.getValue(), Frame.VAR_STANDARD, null);

        return iPC + 1;
        }
    }
