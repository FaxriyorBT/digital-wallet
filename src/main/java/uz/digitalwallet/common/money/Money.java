package uz.digitalwallet.common.money;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * All amounts are kept with two decimal places. Banker's rounding (HALF_EVEN) avoids the
 * systematic upward drift that HALF_UP gives when many values are rounded.
 */
public final class Money {

    public static final int SCALE = 2;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;

    private Money() {
    }

    public static BigDecimal round(BigDecimal value) {
        return value.setScale(SCALE, ROUNDING);
    }

    public static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(SCALE);
    }
}
