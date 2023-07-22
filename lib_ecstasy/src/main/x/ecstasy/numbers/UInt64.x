const UInt64
        extends UIntNumber
        default(0) {
    // ----- constants -----------------------------------------------------------------------------

    /**
     * The minimum value for an UInt64.
     */
    static IntLiteral MinValue = 0;

    /**
     * The maximum value for an UInt64.
     */
    static IntLiteral MaxValue =  0xFFFF_FFFF_FFFF_FFFF;


    // ----- Numeric funky interface ---------------------------------------------------------------

    @Override
    static conditional Int fixedBitLength() {
        return True, 64;
    }

    @Override
    static UInt64 zero() {
        return 0;
    }

    @Override
    static UInt64 one() {
        return 1;
    }

    @Override
    static conditional Range<UInt64> range() {
        return True, MinValue..MaxValue;
    }


    // ----- constructors --------------------------------------------------------------------------

    /**
     * Construct a 64-bit unsigned integer number from its bitwise machine representation.
     *
     * @param bits  an array of bit values that represent this number, ordered from left-to-right,
     *              Most Significant Bit (MSB) to Least Significant Bit (LSB)
     */
    @Override
    construct(Bit[] bits) {
        assert bits.size == 64;
        super(bits);
    }

    /**
     * Construct a 64-bit unsigned integer number from its network-portable representation.
     *
     * @param bytes  an array of byte values that represent this number, ordered from left-to-right,
     *               as they would appear on the wire or in a file
     */
    @Override
    construct(Byte[] bytes) {
        assert bytes.size == 8;
        super(bytes);
    }

    /**
     * Construct a 64-bit unsigned integer number from its `String` representation.
     *
     * @param text  an integer number, in text format
     */
    @Override
    construct(String text) {
        construct UInt64(new IntLiteral(text).toUInt64().bits);
    }


    // ----- properties ----------------------------------------------------------------------------

    @Override
    Signum sign.get() {
        return this == 0 ? Zero : Positive;
    }


    // ----- operations ----------------------------------------------------------------------------

    @Override
    @Op("+")
    UInt64 add(UInt64! n) {
        return this + n;
    }

    @Override
    @Op("-")
    UInt64 sub(UInt64! n) {
        return this - n;
    }

    @Override
    @Op("*")
    UInt64 mul(UInt64! n) {
        return this * n;
    }

    @Override
    @Op("/")
    UInt64 div(UInt64! n) {
        return this / n;
    }

    @Override
    @Op("%")
    UInt64 mod(UInt64! n) {
        return this % n;
    }

    @Override
    UInt64 pow(UInt64! n) {
        UInt64 result = 1;

        while (n-- > 0) {
            result *= this;
        }

        return result;
    }


    // ----- Sequential interface ------------------------------------------------------------------

    @Override
    conditional UInt64 next() {
        if (this < MaxValue) {
            return True, this + 1;
        }

        return False;
    }

    @Override
    conditional UInt64 prev() {
        if (this > MinValue) {
            return True, this - 1;
        }

        return False;
    }


    // ----- conversions ---------------------------------------------------------------------------

    @Override
    Int8 toInt8(Boolean checkBounds = False) {
        assert:bounds !checkBounds || this <= Int8.MaxValue;
        return new Int8(bits[bitLength-8 ..< bitLength]);
    }

    @Override
    Int16 toInt16(Boolean checkBounds = False) {
        assert:bounds !checkBounds || this <= Int16.MaxValue;
        return new Int16(bits[bitLength-16 ..< bitLength]);
    }

    @Override
    Int32 toInt32(Boolean checkBounds = False) {
        assert:bounds !checkBounds || this <= Int32.MaxValue;
        return new Int32(bits[bitLength-32 ..< bitLength]);
    }

    @Override
    Int64 toInt64(Boolean checkBounds = False) {
        assert:bounds !checkBounds || this <= Int64.MaxValue;
        return new Int64(bits);
    }

    @Auto
    @Override
    Int128 toInt128(Boolean checkBounds = False) = new Int128(new Bit[128](i -> i < 128-bitLength ? 0 : bits[i]));

    @Override
    UInt8 toUInt8(Boolean checkBounds = False) {
        assert:bounds !checkBounds || this <= UInt8.MaxValue;
        return new UInt8(bits[bitLength-8 ..< bitLength]);
    }

    @Override
    UInt16 toUInt16(Boolean checkBounds = False) {
        assert:bounds !checkBounds || this <= UInt16.MaxValue;
        return new UInt16(bits[bitLength-16 ..< bitLength]);
    }

    @Override
    UInt32 toUInt32(Boolean checkBounds = False) {
        assert:bounds !checkBounds || this <= UInt32.MaxValue;
        return new UInt32(bits[bitLength-32 ..< bitLength]);
    }

    @Override
    UInt64 toUInt64(Boolean checkBounds = False) = this;

    @Auto
    @Override
    UInt128 toUInt128(Boolean checkBounds = False) = new UInt128(new Bit[128](i -> i < 128-bitLength ? 0 : bits[i]));


    // ----- Stringable implementation -------------------------------------------------------------

    @Override
    Int estimateStringLength() {
        return calculateStringSize(this, sizeArray);
    }

    @Override
    Appender<Char> appendTo(Appender<Char> buf) {
        if (sign == Zero) {
            buf.add('0');
        } else {
            (UInt64 left, UInt64 digit) = this /% 10;
            if (left.sign != Zero) {
                left.appendTo(buf);
            }
            buf.add(Digits[digit.toInt64()]);
        }
        return buf;
    }

    // MaxValue = 18_446_744_073_709_551_615 (20 digits)
    private static UInt64[] sizeArray =
         [
         9, 99, 999, 9_999, 99_999, 999_999,
         9_999_999, 99_999_999, 999_999_999, 9_999_999_999, 99_999_999_999, 999_999_999_999,
         9_999_999_999_999, 99_999_999_999_999, 999_999_999_999_999,
         9_999_999_999_999_999, 99_999_999_999_999_999, 999_999_999_999_999_999,
         9_999_999_999_999_999_999, 18_446_744_073_709_551_615
         ];
}