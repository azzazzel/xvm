package org.xvm.compiler.ast;


import org.xvm.asm.Constant;
import org.xvm.asm.ConstantPool;
import org.xvm.asm.constants.TypeConstant;


/**
 * A type expression is used to specify an abstract data type. In its compiled form, there are many
 * different possible representations of an abstract data type, depending on how it is declared, and
 * depending on how it is used. A TypeExpression may be used to indicate either a Constant
 * (such as a ModuleConstant, PackageConstant, ClassConstant, etc.) or a TypeConstant (such as a
 * ClassTypeConstant, a ParameterTypeConstant, etc.) Often, a type expression must provide a
 * compiled representation of itself before it is able to resolve what its actual ADT will be; in
 * these cases, the type expression can create a temporary place-holder, known as an unresolved
 * constant, which will later be replaced with the real ADT information once the type information
 * has been fully resolved.
 */
public abstract class TypeExpression
        extends Expression
    {
    // ----- type specific functionality -----------------------------------------------------------

    @Override
    public TypeExpression toTypeExpression()
        {
        return this;
        }

    /**
     * Obtain the TypeConstant currently associated with this TypeExpression, creating an unresolved
     * TypeConstant if necessary.
     *
     * @return a TypeConstant
     */
    public TypeConstant ensureTypeConstant()
        {
        TypeConstant constType = getTypeConstant();
        if (constType == null)
            {
            constType = instantiateTypeConstant();
            setTypeConstant(constType);
            }
        return constType;
        }

    /**
     * @return a TypeConstant for this TypeExpression
     */
    protected abstract TypeConstant instantiateTypeConstant();

    /**
     * @return the TypeConstant currently associated with this TypeExpression, or null
     */
    protected TypeConstant getTypeConstant()
        {
        return m_constType;
        }

    /**
     * @param constType  the TypeConstant to associate with this TypeExpression
     */
    protected void setTypeConstant(TypeConstant constType)
        {
        // store the new type constant
        m_constType = constType;
        }

    /**
     * Perform right-to-left inference of type information, if possible.
     *
     * @param type  a type constant from an expression related to this TypeExpression, in such a
     *              way that this TypeExpression can steal information from the TypeConstant, such
     *              as parameter types
     *
     * @return a TypeExpression to use instead of this TypeExpression
     */
    public TypeExpression inferTypeFrom(TypeConstant type)
        {
        assert m_constType != null;
        assert type.isA(m_constType);

        // REVIEW this is where we could also add support for a "var" (and/or "val") keyword

        return this;
        }


    // ----- Expression methods --------------------------------------------------------------------

    @Override
    public boolean isConstant()
        {
        return ensureTypeConstant().isConstant();
        }

    @Override
    public Constant toConstant()
        {
        TypeConstant type = ensureTypeConstant();
        assert type.isConstant();
        return type;
        }


    // ----- fields --------------------------------------------------------------------------------

    /**
     * The TypeConstant currently associated with this TypeExpression.
     */
    private TypeConstant m_constType;
    }
