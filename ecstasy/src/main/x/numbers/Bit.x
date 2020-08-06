const Bit
    implements Sequential
    default(0)
    {
    construct(IntLiteral literal)
        {
        assert literal == 0 || literal == 1;
        this.literal = literal;
        }

    private IntLiteral literal;

    IntLiteral toIntLiteral()
        {
        return literal;
        }

    Boolean toBoolean()
        {
        return literal == 1;
        }

    @Auto Byte toByte()
        {
        return literal.toByte();
        }

    @Auto Int toInt()
        {
        return literal.toInt();
        }

    @Auto UInt toUInt()
        {
        return literal.toUInt();
        }

    @Op("&")
    Bit and(Bit that)
        {
        return this.literal == 1 && that.literal == 1 ? 1 : 0;
        }

    @Op("|")
    Bit or(Bit that)
        {
        return this.literal == 1 || that.literal == 1 ? 1 : 0;
        }

    @Op("^")
    Bit xor(Bit that)
        {
        return this.literal == 1 ^^ that.literal == 1 ? 1 : 0;
        }

    @Op("~")
    @Op Bit not()
        {
        return literal == 1 ? 0 : 1;
        }


    // ----- Sequential interface ------------------------------------------------------------------

    @Override
    conditional Bit next()
        {
        if (this == 0)
            {
            return true, 1;
            }

        return false;
        }

    @Override
    conditional Bit prev()
        {
        if (this == 1)
            {
            return true, 0;
            }

        return false;
        }

    @Override
    Int stepsTo(Bit that)
        {
        return that - this;
        }

    @Override
    Bit skip(Int steps)
        {
        return switch (this, steps)
            {
            // TODO GG take the parenthesis off of these case statements to see the compiler NPE
            case (_,  0): this;
            case (0,  1): 1;
            case (1, -1): 0;
            default: throw new OutOfBounds($"Bit={this}, steps={steps}");
            };
        }


    // ----- Stringable ----------------------------------------------------------------------------

    @Override
    Int estimateStringLength()
        {
        return 1;
        }

    @Override
    Appender<Char> appendTo(Appender<Char> buf)
        {
        return buf.add(toBoolean() ? '1' : '0');
        }
    }
