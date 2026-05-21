package com.phonebiz.common;

import com.phonebiz.common.BusinessException;
import com.phonebiz.common.ErrorCode;

/**
 * Safe enum parsing that throws BusinessException instead of IllegalArgumentException.
 */
public final class EnumHelper {

    private EnumHelper() {}

    public static <E extends Enum<E>> E parse(Class<E> enumClass, String value) {
        try {
            return Enum.valueOf(enumClass, value.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException(ErrorCode.SYS_002, 
                "Invalid value '" + value + "' for enum " + enumClass.getSimpleName());
        }
    }
}
