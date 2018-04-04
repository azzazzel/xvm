package org.xvm.asm.constants;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.function.Consumer;

import org.xvm.asm.ClassStructure;
import org.xvm.asm.Component;
import org.xvm.asm.Component.Composition;
import org.xvm.asm.Component.Contribution;
import org.xvm.asm.Component.ContributionChain;
import org.xvm.asm.CompositeComponent;
import org.xvm.asm.Constant;
import org.xvm.asm.ConstantPool;
import org.xvm.asm.ErrorListener;
import org.xvm.asm.GenericTypeResolver;
import org.xvm.asm.PropertyStructure;
import org.xvm.asm.TypedefStructure;

import org.xvm.runtime.Frame;
import org.xvm.runtime.ObjectHandle;
import org.xvm.runtime.OpSupport;
import org.xvm.runtime.TemplateRegistry;
import org.xvm.runtime.TypeComposition;

import org.xvm.runtime.template.xBoolean;
import org.xvm.runtime.template.xOrdered;

import org.xvm.util.Severity;

import static org.xvm.util.Handy.readIndex;
import static org.xvm.util.Handy.writePackedLong;


/**
 * A TypeConstant that represents a type that is defined by some other structure within the module.
 * Specifically, the definition pointed to by this TypeConstant can be any one of:
 * <p/>
 * <ul>
 * <li>{@link ModuleConstant} for a module</li>
 * <li>{@link PackageConstant} for a package</li>
 * <li>{@link ClassConstant} for a class</li>
 * <li>{@link TypedefConstant} for a typedef</li>
 * <li>{@link PropertyConstant} for a class' type parameter</li>
 * <li>{@link RegisterConstant} for a method's type parameter</li>
 * <li>{@link ThisClassConstant} to indicate the auto-narrowing "this" class</li>
 * <li>{@link ParentClassConstant} for an auto-narrowing parent of an auto-narrowing class</li>
 * <li>{@link ChildClassConstant} for a named auto-narrowing child of an auto-narrowing class</li>
 * <li>{@link UnresolvedNameConstant} for a definition that has not been resolved at this point</li>
 * </ul>
 */
public class TerminalTypeConstant
        extends TypeConstant
    {
    // ----- constructors --------------------------------------------------------------------------

    /**
     * Constructor used for deserialization.
     *
     * @param pool   the ConstantPool that will contain this Constant
     * @param format the format of the Constant in the stream
     * @param in     the DataInput stream to read the Constant value from
     *
     * @throws IOException if an issue occurs reading the Constant value
     */
    public TerminalTypeConstant(ConstantPool pool, Format format, DataInput in)
            throws IOException
        {
        super(pool);

        m_iDef = readIndex(in);
        }

    /**
     * Construct a constant whose value is a data type.
     *
     * @param pool    the ConstantPool that will contain this Constant
     * @param constId a ModuleConstant, PackageConstant, or ClassConstant
     */
    public TerminalTypeConstant(ConstantPool pool, Constant constId)
        {
        super(pool);

        assert !(constId instanceof TypeConstant);

        switch (constId.getFormat())
            {
            case Module:
            case Package:
            case Class:
            case Typedef:
            case Property:
            case Register:
            case ThisClass:
            case ParentClass:
            case ChildClass:
            case UnresolvedName:
                break;

            default:
                throw new IllegalArgumentException("constant " + constId.getFormat()
                        + " is not a Module, Package, Class, Typedef, or formal type parameter");
            }

        m_constId = constId;
        }


    // ----- TypeConstant methods ------------------------------------------------------------------

    @Override
    public boolean isImmutabilitySpecified()
        {
        return false;
        }

    @Override
    public boolean isAccessSpecified()
        {
        return false;
        }

    @Override
    public Access getAccess()
        {
        return Access.PUBLIC;
        }

    @Override
    public boolean isParamsSpecified()
        {
        return false;
        }

    @Override
    public int getMaxParamsCount()
        {
        Constant constant = getDefiningConstant();
        switch (constant.getFormat())
            {
            case Module:
            case Package:
            case Property:
            case Register:
                return 0;

            case Class:
                {
                // examine the structure to determine if it represents a class or interface
                ClassStructure clz = (ClassStructure) ((ClassConstant) constant).getComponent();
                return clz.getTypeParams().size();
                }

            case ThisClass:
            case ParentClass:
            case ChildClass:
                {
                ClassStructure clz = (ClassStructure) ((PseudoConstant) constant)
                        .getDeclarationLevelClass().getComponent();
                return clz.getTypeParams().size();
                }

            default:
                throw new IllegalStateException("unexpected defining constant: " + constant);
            }
        }

    @Override
    public boolean isAnnotated()
        {
        return false;
        }

    @Override
    public boolean isSingleDefiningConstant()
        {
        Constant constId = ensureResolvedConstant();
        return constId.getFormat() != Format.Typedef ||
                getTypedefTypeConstant((TypedefConstant) constId).isSingleDefiningConstant();
        }

    @Override
    public Constant getDefiningConstant()
        {
        Constant constId = ensureResolvedConstant();
        return constId.getFormat() == Format.Typedef
                ? getTypedefTypeConstant((TypedefConstant) constId).getDefiningConstant()
                : constId;
        }

    /**
     * @return the underlying constant, resolving it if it is still unresolved and can be resolved
     *         at this point
     */
    protected Constant ensureResolvedConstant()
        {
        Constant constId = m_constId;

        // resolve any previously unresolved constant at this point
        if (constId instanceof ResolvableConstant)
            {
            Constant constResolved = ((ResolvableConstant) constId).getResolvedConstant();
            if (constResolved != null)
                {
                // note that this TerminalTypeConstant could not have previously been registered
                // with the pool because it was not resolved, so changing the reference to the
                // underlying constant is still safe at this point
                m_constId = constId = constResolved;
                }
            }

        return constId;
        }

    @Override
    public boolean isAutoNarrowing()
        {
        return ensureResolvedConstant().isAutoNarrowing();
        }

    @Override
    public TypeConstant resolveTypedefs()
        {
        Constant constId = getDefiningConstant();
        return constId instanceof TypedefConstant
                ? getTypedefTypeConstant((TypedefConstant) constId).resolveTypedefs()
                : this;
        }

    @Override
    public TypeConstant resolveGenerics(GenericTypeResolver resolver)
        {
        Constant constId = getDefiningConstant();
        return constId instanceof PropertyConstant
            ? resolver.resolveGenericType((PropertyConstant) constId)
            : this;
        }

    @Override
    public TypeConstant normalizeParametersInternal(List<TypeConstant> listParams)
        {
        Constant         constant = getDefiningConstant();
        IdentityConstant idClz;
        switch (constant.getFormat())
            {
            case Module:
            case Package:
            case Class:
                idClz = (IdentityConstant) constant;
                break;

            case ThisClass:
            case ParentClass:
            case ChildClass:
                idClz = ((PseudoConstant) constant).getDeclarationLevelClass();
                break;

            case Property:
            case Register:
                return this;

            default:
                throw new IllegalStateException("unexpected defining constant: " + constant);
            }

        // keep tuples as-is
        if (!isTuple())
            {
            // if the type is parameterized, but it is missing some of its parameters, then default
            // those type parameters to their constraint types
            ClassStructure struct = (ClassStructure) idClz.getComponent();
            Map<StringConstant, TypeConstant> mapClassParams = struct.getTypeParams();
            if (mapClassParams.size() > listParams.size())
                {
                // build a "merged" list of type parameters
                List<TypeConstant> listTypes = struct.normalizeParameters(listParams);
                if (listTypes != listParams)
                    {
                    TypeConstant[] aTypes = listTypes.toArray(new TypeConstant[listTypes.size()]);
                    return getConstantPool().ensureParameterizedTypeConstant(this, aTypes);
                    }
                }
            }

        return this;
        }

    @Override
    public TypeConstant resolveAutoNarrowing()
        {
        Constant constant = getDefiningConstant();
        switch (constant.getFormat())
            {
            case ThisClass:
            case ParentClass:
            case ChildClass:
                return ((PseudoConstant) constant).getDeclarationLevelClass().asTypeConstant();

            case UnresolvedName:
                throw new IllegalStateException("unexpected unresolved-name constant: " + constant);

            default:
                return this;
            }
        }

    @Override
    public TypeConstant inferAutoNarrowing(IdentityConstant constThisClass)
        {
        Constant constId = getDefiningConstant();
        if (constId.getFormat() == Format.Class)
            {
            ClassConstant constClass = (ClassConstant) constId;
            if (constThisClass.equals(constClass))
                {
                return getConstantPool().ensureThisTypeConstant(constClass, null);
                }

            // TODO: parents & children
            }
        return this;
        }

    @Override
    public boolean isTuple()
        {
        Constant constant = getDefiningConstant();
        switch (constant.getFormat())
            {
            case Module:
            case Package:
            case Property:
                return false;

            case Class:
                break;

            case Register:
                constant = getRegisterTypeConstant((RegisterConstant) constant);
                break;

            case ThisClass:
            case ParentClass:
            case ChildClass:
                constant = ((PseudoConstant) constant).getDeclarationLevelClass();
                break;

            default:
                // let's be tolerant to unresolved constants
                return false;
            }
        return constant.equals(getConstantPool().clzTuple());
        }

    @Override
    protected TypeInfo buildTypeInfo(ErrorListener errs)
        {
        Constant constant = getDefiningConstant();
        switch (constant.getFormat())
            {
            case Module:
            case Package:
            case Class:
                return super.buildTypeInfo(errs);

            case Property:
                return getPropertyTypeConstant((PropertyConstant) constant).buildTypeInfo(errs);

            case Register:
                return getRegisterTypeConstant((RegisterConstant) constant).buildTypeInfo(errs);

            case ThisClass:
            case ParentClass:
            case ChildClass:
                return ((PseudoConstant) constant).getDeclarationLevelClass().asTypeConstant()
                        .buildTypeInfo(errs);

            default:
                throw new IllegalStateException("unexpected defining constant: " + constant);
            }
        }

    @Override
    public boolean isOnlyNullable()
        {
        TypeConstant typeResolved = resolveTypedefs();
        return this == typeResolved
                ? ensureResolvedConstant().equals(getConstantPool().clzNullable())
                : typeResolved.isOnlyNullable();
        }

    @Override
    protected TypeConstant cloneSingle(TypeConstant type)
        {
        return this;
        }

    @Override
    protected TypeConstant unwrapForCongruence()
        {
        TypeConstant typeResolved = resolveTypedefs();
        return typeResolved == this
                ? this
                : typeResolved.unwrapForCongruence();
        }

    @Override
    public boolean extendsClass(IdentityConstant constClass)
        {
        Constant constant = getDefiningConstant();
        switch (constant.getFormat())
            {
            case Module:
            case Package:
            case Class:
                return ((ClassStructure) ((IdentityConstant) constant)
                        .getComponent()).extendsClass(constClass);

            case Property:
                return getPropertyTypeConstant((PropertyConstant) constant).extendsClass(constClass);

            case Register:
                return getRegisterTypeConstant((RegisterConstant) constant).extendsClass(constClass);

            case ThisClass:
            case ParentClass:
            case ChildClass:
                return ((ClassStructure) ((PseudoConstant) constant).getDeclarationLevelClass()
                        .getComponent()).extendsClass(constClass);

            default:
                throw new IllegalStateException("unexpected defining constant: " + constant);
            }
        }

    @Override
    public boolean isClassType()
        {
        Constant constant = getDefiningConstant();
        switch (constant.getFormat())
            {
            case Module:
            case Package:
                // these are always class types (not interface types)
                return true;

            case Class:
                {
                // examine the structure to determine if it represents a class or interface
                ClassStructure clz = (ClassStructure) ((ClassConstant) constant).getComponent();
                return clz.getFormat() != Component.Format.INTERFACE;
                }

            case Property:
                return getPropertyTypeConstant((PropertyConstant) constant).isClassType();

            case Register:
                return getRegisterTypeConstant((RegisterConstant) constant).isClassType();

            case ThisClass:
            case ParentClass:
            case ChildClass:
                {
                ClassStructure clz = (ClassStructure) ((PseudoConstant) constant)
                        .getDeclarationLevelClass().getComponent();
                return clz.getFormat() != Component.Format.INTERFACE;
                }

            default:
                throw new IllegalStateException("unexpected defining constant: " + constant);
            }
        }

    @Override
    public boolean isSingleUnderlyingClass(boolean fAllowInterface)
        {
        Constant constant = getDefiningConstant();
        switch (constant.getFormat())
            {
            case Module:
            case Package:
                // these are always class types (not interface types)
                return fAllowInterface;

            case Class:
                {
                ClassStructure clz = (ClassStructure) ((ClassConstant) constant).getComponent();
                return fAllowInterface || clz.getFormat() != Component.Format.INTERFACE;
                }

            case Property:
                return getPropertyTypeConstant((PropertyConstant) constant).
                    isSingleUnderlyingClass(fAllowInterface);

            case Register:
                return getRegisterTypeConstant((RegisterConstant) constant).
                    isSingleUnderlyingClass(fAllowInterface);

            case ThisClass:
            case ParentClass:
            case ChildClass:
                {
                ClassStructure clz = (ClassStructure) ((PseudoConstant) constant)
                        .getDeclarationLevelClass().getComponent();
                return fAllowInterface || clz.getFormat() != Component.Format.INTERFACE;
                }

            default:
                throw new IllegalStateException("unexpected defining constant: " + constant);
            }
        }

    @Override
    public IdentityConstant getSingleUnderlyingClass(boolean fAllowInterface)
        {
        Constant constant = getDefiningConstant();
        switch (constant.getFormat())
            {
            case Module:
            case Package:
                // these are always class types (not interface types)
                return (IdentityConstant) constant;

            case Class:
                if (!fAllowInterface)
                    {
                    // must not be an interface
                    assert (((ClassConstant) constant).getComponent()).getFormat() != Component.Format.INTERFACE;
                    }
                return (IdentityConstant) constant;

            case Property:
                return getPropertyTypeConstant((PropertyConstant) constant).
                    getSingleUnderlyingClass(fAllowInterface);

            case Register:
                return getRegisterTypeConstant((RegisterConstant) constant).
                    getSingleUnderlyingClass(fAllowInterface);

            case ParentClass:
            case ChildClass:
            case ThisClass:
                return ((PseudoConstant) constant).getDeclarationLevelClass();

            default:
                throw new IllegalStateException("unexpected defining constant: " + constant);
            }
        }

    @Override
    public boolean isExplicitClassIdentity(boolean fAllowParams)
        {
        Constant constant = getDefiningConstant();
        switch (constant.getFormat())
            {
            case Module:
            case Package:
            case Class:
            case ThisClass:
            case ParentClass:
            case ChildClass:
                return true;

            case Property:
            case Register:
                return false;

            default:
                throw new IllegalStateException("unexpected defining constant: " + constant);
            }
        }

    @Override
    public Component.Format getExplicitClassFormat()
        {
        Constant constant = getDefiningConstant();
        switch (constant.getFormat())
            {
            case Module:
                return Component.Format.MODULE;

            case Package:
                return Component.Format.PACKAGE;

            case Class:
                // get the class referred to and return its format
                return ((ClassConstant) constant).getComponent().getFormat();

            case ThisClass:
            case ParentClass:
            case ChildClass:
                // follow the indirection to the class structure
                return ((PseudoConstant) constant).getDeclarationLevelClass().getComponent().getFormat();

            case Property:
            case Register:
            default:
                throw new IllegalStateException("no class format for: " + constant);
            }
        }

    @Override
    public TypeConstant getExplicitClassInto()
        {
        Constant       constId = getDefiningConstant();
        ClassStructure structMixin;
        switch (constId.getFormat())
            {
            case Class:
                // get the class referred to and return its format
                structMixin = (ClassStructure) ((ClassConstant) constId).getComponent();
                break;

            case ThisClass:
            case ParentClass:
            case ChildClass:
                structMixin = (ClassStructure) ((PseudoConstant) constId).getDeclarationLevelClass().getComponent();
                break;

            default:
                throw new IllegalStateException("no class format for: " + constId);
            }

        if (structMixin == null || structMixin.getFormat() != Component.Format.MIXIN)
            {
            throw new IllegalStateException("mixin=" + structMixin);
            }

        Contribution contribInto = structMixin.findContribution(Composition.Into);
        if (contribInto != null)
            {
            return contribInto.getTypeConstant();
            }

        Contribution contribExtends = structMixin.findContribution(Composition.Extends);
        if (contribExtends != null)
            {
            return contribExtends.getTypeConstant().getExplicitClassInto();
            }

        return getConstantPool().typeObject();
        }

    @Override
    public boolean isConstant()
        {
        Constant constant = getDefiningConstant();
        switch (constant.getFormat())
            {
            case Property:
            case Register:
                return false;

            default:
                return true;
            }
        }

    @Override
    public <T extends TypeConstant> T findFirst(Class<T> clz)
        {
        return clz == getClass() ? (T) this : null;
        }

    /**
     * Dereference a typedef constant to find the type to which it refers.
     *
     * @param constTypedef  a typedef constant
     *
     * @return the type that the typedef refers to
     */
    public static TypeConstant getTypedefTypeConstant(TypedefConstant constTypedef)
        {
        return ((TypedefStructure) constTypedef.getComponent()).getType();
        }

    /**
     * Dereference a property constant that is used for a type parameter, to obtain the constraint
     * type of that type parameter.
     *
     * @param constProp the property constant for the property that holds the type parameter type
     *
     * @return the constraint type of the type parameter
     */
    public static TypeConstant getPropertyTypeConstant(PropertyConstant constProp)
        {
        // the type points to a property, which means that the type is a parameterized type;
        // the type of the property will be "Type<X>", so return X
        TypeConstant typeProp = ((PropertyStructure) constProp.getComponent()).getType();
        assert typeProp.isEcstasy("Type") && typeProp.isParamsSpecified();
        return typeProp.getParamTypesArray()[0];
        }

    /**
     * Dereference a register constant that is used for a type parameter, to obtain the constraint
     * type of that type parameter.
     *
     * @param constReg  the register constant for the register that holds the type parameter type
     *
     * @return the constraint type of the type parameter
     */
    public static TypeConstant getRegisterTypeConstant(RegisterConstant constReg)
        {
        // the type points to a register, which means that the type is a parameterized type;
        // the type of the register will be "Type<X>", so return X
        MethodConstant   constMethod = constReg.getMethod();
        int              nReg        = constReg.getRegister();
        TypeConstant[]   atypeParams = constMethod.getRawParams();
        assert atypeParams.length > nReg;
        TypeConstant     typeParam   = atypeParams[nReg];
        assert typeParam.isEcstasy("Type") && typeParam.isParamsSpecified();
        return typeParam.getParamTypesArray()[0];
        }


    // ----- type comparison support ---------------------------------------------------------------

    @Override
    public List<ContributionChain> collectContributions(
            TypeConstant typeLeft, List<TypeConstant> listRight, List<ContributionChain> chains)
        {
        if (this.equals(getConstantPool().typeObject()))
            {
            // Object doesn't have any contributions
            return chains;
            }

        Constant constIdRight = getDefiningConstant();

        if (typeLeft.isSingleDefiningConstant()
                && constIdRight.equals(typeLeft.getDefiningConstant()))
            {
            chains.add(new ContributionChain(new Contribution(Composition.Equal, null)));
            return chains;
            }

        Format format;
        switch (format = constIdRight.getFormat())
            {
            case Module:
            case Package:
                break;

            case Class:
                {
                ClassStructure clzRight = (ClassStructure)
                    ((IdentityConstant) constIdRight).getComponent();

                chains.addAll(typeLeft.collectClassContributions(
                    clzRight, listRight, new ArrayList<>()));
                break;
                }

            case Property:
                // scenarios we can handle here are:
                // 1. r-value (this) = T (formal parameter type), constrained by U (other formal type)
                //    l-value (that) = U (formal parameter type)
                //
                // 2. r-value (this) = T (formal parameter type), constrained by U (real type)
                //    l-value (that) = V (real type), where U "is a" V
                return getPropertyTypeConstant((PropertyConstant) constIdRight).
                    collectContributions(typeLeft, listRight, chains);

            case Register:
                return getRegisterTypeConstant((RegisterConstant) constIdRight).
                    collectContributions(typeLeft, listRight, chains);

            case ThisClass:
            case ParentClass:
            case ChildClass:
                {
                if (typeLeft.isSingleDefiningConstant())
                    {
                    Constant constIdLeft = typeLeft.getDefiningConstant();

                    if (constIdLeft.getFormat() == format
                        && ((PseudoConstant) constIdRight).
                                isCongruentWith((PseudoConstant) constIdLeft))
                        {
                        chains.add(new ContributionChain(new Contribution(Composition.Equal, null)));
                        break;
                        }
                    }

                ClassStructure clzRight = (ClassStructure)
                    ((PseudoConstant) constIdRight).getDeclarationLevelClass().getComponent();

                chains.addAll(typeLeft.collectClassContributions(
                    clzRight, listRight, new ArrayList<>()));
                break;
                }

            default:
                throw new IllegalStateException("unexpected constant: " + constIdRight);
            }
        return chains;
        }

    @Override
    protected List<ContributionChain> collectClassContributions(
            ClassStructure clzRight, List<TypeConstant> listRight, List<ContributionChain> chains)
        {
        Constant constIdLeft = getDefiningConstant();
        switch (constIdLeft.getFormat())
            {
            case Module:
            case Package:
                break;

            case Class:
                {
                ClassConstant constClzLeft = (ClassConstant) constIdLeft;
                if (constClzLeft.equals(getConstantPool().clzObject()))
                    {
                    // everything is considered to extend Object (even interfaces)
                    chains.add(new ContributionChain(
                        new Contribution(Composition.Extends, getConstantPool().typeObject())));
                    break;
                    }

                List<ContributionChain> chainsClz =
                    clzRight.collectContributions(constClzLeft, listRight, new ArrayList<>(), true);
                if (chainsClz.isEmpty())
                    {
                    ClassStructure clzLeft = (ClassStructure) constClzLeft.getComponent();
                    if (clzLeft.getFormat() == Component.Format.INTERFACE)
                        {
                        chains.add(new ContributionChain(
                            new Contribution(Composition.MaybeDuckType, null)));
                        }
                    }
                else
                    {
                    chains.addAll(chainsClz);
                    }
                break;
                }

            case Property:
            case Register:
                // r-value (that) is a real type; it cannot have a formal type contribution
                // (assigned to a formal type)
                break;

            case ThisClass:
            case ParentClass:
            case ChildClass:
                {
                ClassStructure clzLeft = (ClassStructure)
                    ((PseudoConstant) constIdLeft).getDeclarationLevelClass().getComponent();
                return clzLeft.getIdentityConstant().asTypeConstant().
                    collectClassContributions(clzRight, listRight, chains);
                }

            default:
                throw new IllegalStateException("unexpected constant: " + constIdLeft);
            }

        return chains;
        }

    @Override
    protected boolean validateContributionFrom(
            TypeConstant typeRight, Access accessLeft, ContributionChain chain)
        {
        // there is nothing that could change the result of "checkAssignableTo"
        return true;
        }

    @Override
    protected Set<SignatureConstant> isInterfaceAssignableFrom(
            TypeConstant typeRight, Access accessLeft, List<TypeConstant> listLeft)
        {
        Constant constIdLeft = getDefiningConstant();
        switch (constIdLeft.getFormat())
            {
            case Class:
                {
                IdentityConstant idLeft = (IdentityConstant) constIdLeft;
                ClassStructure clzLeft = (ClassStructure) idLeft.getComponent();

                assert clzLeft.getFormat() == Component.Format.INTERFACE;

                return clzLeft.isInterfaceAssignableFrom(typeRight, accessLeft, listLeft);
                }

            case Register:
                return getRegisterTypeConstant((RegisterConstant) constIdLeft).
                    isInterfaceAssignableFrom(typeRight, accessLeft, listLeft);

            case ThisClass:
            case ParentClass:
            case ChildClass:
                return ((PseudoConstant) constIdLeft).getDeclarationLevelClass().asTypeConstant().
                    isInterfaceAssignableFrom(typeRight, accessLeft, listLeft);

            default:
                throw new IllegalStateException("unexpected constant: " + constIdLeft);
            }
        }

    @Override
    public boolean containsSubstitutableMethod(SignatureConstant signature, Access access,
                                               List<TypeConstant> listParams)
        {
        Constant constIdThis = getDefiningConstant();
        switch (constIdThis.getFormat())
            {
            case Module:
            case Package:
            case Class:
                {
                IdentityConstant idThis  = (IdentityConstant) constIdThis;
                ClassStructure   clzThis = (ClassStructure) idThis.getComponent();

                return clzThis.containsSubstitutableMethod(signature, access, listParams);
                }

            case Register:
                return getRegisterTypeConstant((RegisterConstant) constIdThis).
                    containsSubstitutableMethod(signature, access, listParams);

            case ThisClass:
            case ParentClass:
            case ChildClass:
                return ((PseudoConstant) constIdThis).getDeclarationLevelClass().asTypeConstant().
                    containsSubstitutableMethod(signature, access, listParams);

            default:
                throw new IllegalStateException("unexpected constant: " + constIdThis);
            }
        }

    @Override
    public Usage checkConsumption(String sTypeName, Access access, List<TypeConstant> listParams)
        {
        Constant constIdThis = getDefiningConstant();
        switch (constIdThis.getFormat())
            {
            case Module:
            case Package:
            case Property: // Property does not consume
                return Usage.NO;

            case Class:
                {
                ClassStructure clzThis = (ClassStructure)
                    ((IdentityConstant) constIdThis).getComponent();

                Map<StringConstant, TypeConstant> mapFormal = clzThis.getTypeParams();

                listParams = clzThis.normalizeParameters(listParams);

                Iterator<TypeConstant> iterParams = listParams.iterator();
                Iterator<StringConstant> iterNames = mapFormal.keySet().iterator();

                while (iterParams.hasNext())
                    {
                    TypeConstant constParam = iterParams.next();
                    String sFormal = iterNames.next().getValue();

                    if (constParam.consumesFormalType(sTypeName, access)
                            && clzThis.producesFormalType(sFormal, access, listParams)
                        ||
                        constParam.producesFormalType(sTypeName, access)
                            && clzThis.consumesFormalType(sFormal, access, listParams))
                        {
                        return Usage.YES;
                        }
                    }
                return Usage.NO;
                }

            case Register:
                return getRegisterTypeConstant((RegisterConstant) constIdThis).
                    checkConsumption(sTypeName, access, listParams);

            case ThisClass:
            case ParentClass:
            case ChildClass:
                return ((PseudoConstant) constIdThis).getDeclarationLevelClass().asTypeConstant().
                    checkConsumption(sTypeName, access, listParams);

            default:
                throw new IllegalStateException("unexpected constant: " + constIdThis);
            }
        }

    @Override
    public Usage checkProduction(String sTypeName, Access access,
                                 List<TypeConstant> listParams)
        {
        Constant constIdThis = getDefiningConstant();
        switch (constIdThis.getFormat())
            {
            case Module:
            case Package:
                return Usage.NO;

            case Property:
                return Usage.valueOf(((PropertyConstant) constIdThis).getName().equals(sTypeName));

            case Class:
                {
                ClassStructure clzThis = (ClassStructure)
                    ((IdentityConstant) constIdThis).getComponent();

                Map<StringConstant, TypeConstant> mapFormal = clzThis.getTypeParams();

                listParams = clzThis.normalizeParameters(listParams);

                Iterator<TypeConstant> iterParams = listParams.iterator();
                Iterator<StringConstant> iterNames = mapFormal.keySet().iterator();

                while (iterParams.hasNext())
                    {
                    TypeConstant constParam = iterParams.next();
                    String sFormal = iterNames.next().getValue();

                    if (constParam.producesFormalType(sTypeName, access)
                            && clzThis.producesFormalType(sFormal, access, listParams)
                        ||
                        constParam.consumesFormalType(sTypeName, access)
                            && clzThis.consumesFormalType(sFormal, access, listParams))
                        {
                        return Usage.YES;
                        }
                    }

                return Usage.NO;
                }

            case Register:
                return getRegisterTypeConstant((RegisterConstant) constIdThis).
                    checkProduction(sTypeName, access, listParams);

            case ThisClass:
            case ParentClass:
            case ChildClass:
                return ((PseudoConstant) constIdThis).getDeclarationLevelClass().asTypeConstant().
                    checkProduction(sTypeName, access, listParams);

            default:
                throw new IllegalStateException("unexpected constant: " + constIdThis);
            }
        }

    // ----- run-time support ----------------------------------------------------------------------

    @Override
    public OpSupport getOpSupport(TemplateRegistry registry)
        {
        Constant constIdThis = getDefiningConstant();
        switch (constIdThis.getFormat())
            {
            case Module:
            case Package:
            case Class:
                return registry.getTemplate((IdentityConstant) constIdThis);

            case Register:
                return getRegisterTypeConstant((RegisterConstant) constIdThis).getOpSupport(registry);

            case ThisClass:
            case ParentClass:
            case ChildClass:
                return registry.getTemplate(((PseudoConstant) constIdThis).getDeclarationLevelClass());

            default:
                throw new IllegalStateException("unexpected defining constant: " + constIdThis);
            }
        }

    @Override
    public int callEquals(Frame frame, ObjectHandle hValue1, ObjectHandle hValue2, int iReturn)
        {
        if (hValue1 == hValue2)
            {
            return frame.assignValue(iReturn, xBoolean.TRUE);
            }

        TypeComposition clz = frame.f_context.f_templates.resolveClass(this);
        return clz.getTemplate().callEquals(frame, clz, hValue1, hValue2, iReturn);
        }

    @Override
    public int callCompare(Frame frame, ObjectHandle hValue1, ObjectHandle hValue2, int iReturn)
        {
        if (hValue1 == hValue2)
            {
            return frame.assignValue(iReturn, xOrdered.EQUAL);
            }

        TypeComposition clz = frame.f_context.f_templates.resolveClass(this);
        return clz.getTemplate().callCompare(frame, clz, hValue1, hValue2, iReturn);
        }


    // ----- Constant methods ----------------------------------------------------------------------

    @Override
    public Format getFormat()
        {
        return Format.TerminalType;
        }

    @Override
    public boolean containsUnresolved()
        {
        Constant constId = ensureResolvedConstant();
        if (constId.containsUnresolved())
            {
            return true;
            }
        if (getFormat() == Format.Typedef)
            {
            return getTypedefTypeConstant((TypedefConstant) constId).containsUnresolved();
            }
        return false;
        }

    @Override
    public Constant simplify()
        {
        // simplify the underlying constant
        Constant constId = ensureResolvedConstant().simplify();

        // store off the result
        m_constId = constId;

        // compile down all of the types that refer to typedefs so that they refer to the underlying
        // types instead
        if (constId instanceof TypedefConstant)
            {
            Component    typedef   = ((TypedefConstant) constId).getComponent();
            TypeConstant constType;
            if (typedef instanceof CompositeComponent)
                {
                List<Component> typedefs = ((CompositeComponent) typedef).components();
                constType = (TypeConstant) ((TypedefStructure) typedefs.get(0)).getType().simplify();
                for (int i = 1, c = typedefs.size(); i < c; ++i)
                    {
                    TypeConstant constTypeN = (TypeConstant) ((TypedefStructure) typedefs.get(i)).getType().simplify();
                    if (!constType.equals(constTypeN))
                        {
                        // typedef points to more than one type, conditionally, so just leave the
                        // typedef in place
                        return this;
                        }
                    }
                }
            else
                {
                constType = (TypeConstant) ((TypedefStructure) typedef).getType().simplify();
                }
            assert constType != null;
            return constType;
            }

        return this;
        }

    @Override
    public void forEachUnderlying(Consumer<Constant> visitor)
        {
        visitor.accept(ensureResolvedConstant());
        }

    @Override
    protected Object getLocator()
        {
        Constant constId = ensureResolvedConstant();
        return constId.getFormat() == Format.UnresolvedName
                ? null
                : constId;
        }

    @Override
    protected int compareDetails(Constant obj)
        {
        TerminalTypeConstant that = (TerminalTypeConstant) obj;
        Constant constThis = this.m_constId;
        if (constThis instanceof ResolvableConstant)
            {
            constThis = ((ResolvableConstant) constThis).unwrap();
            }
        Constant constThat = that.m_constId;
        if (constThat instanceof ResolvableConstant)
            {
            constThat = ((ResolvableConstant) constThat).unwrap();
            }
        return constThis.compareTo(constThat);
        }

    @Override
    public String getValueString()
        {
        return ensureResolvedConstant().getValueString();
        }


    // ----- XvmStructure methods ------------------------------------------------------------------

    @Override
    protected void disassemble(DataInput in)
            throws IOException
        {
        m_constId = getConstantPool().getConstant(m_iDef);
        }

    @Override
    protected void registerConstants(ConstantPool pool)
        {
        m_constId = pool.register(ensureResolvedConstant());
        }

    @Override
    protected void assemble(DataOutput out)
            throws IOException
        {
        out.writeByte(getFormat().ordinal());
        writePackedLong(out, ensureResolvedConstant().getPosition());
        }

    @Override
    public boolean validate(ErrorListener errs)
        {
        boolean fHalt = false;

        if (!isValidated())
            {
            fHalt |= super.validate(errs);

            Constant constant = getDefiningConstant();
            switch (constant.getFormat())
                {
                case Module:
                case Package:
                case Class:
                case Property:
                case Register:
                case ThisClass:
                case ParentClass:
                case ChildClass:
                    break;

                case UnresolvedName:
                default:
                    // this is basically an illegal state exception
                    fHalt |= log(errs, Severity.ERROR, VE_UNKNOWN, constant.getValueString()
                            + " (" + constant.getFormat() + ')');
                    break;
                }
            }

        return fHalt;
        }


    // ----- Object methods ------------------------------------------------------------------------

    @Override
    public int hashCode()
        {
        return ensureResolvedConstant().hashCode();
        }


    // ----- fields --------------------------------------------------------------------------------

    /**
     * During disassembly, this holds the index of the constant that defines this type.
     */
    private transient int m_iDef;

    /**
     * The class referred to. May be an IdentityConstant (ModuleConstant, PackageConstant,
     * ClassConstant, TypedefConstant, PropertyConstant), or a PseudoConstant (ThisClassConstant,
     * ParentClassConstant, ChildClassConstant, RegisterConstant, or UnresolvedNameConstant).
     */
    private Constant m_constId;
    }
