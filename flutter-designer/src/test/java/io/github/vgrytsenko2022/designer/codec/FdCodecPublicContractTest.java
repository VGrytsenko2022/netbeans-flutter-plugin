package io.github.vgrytsenko2022.designer.codec;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FdCodecPublicContractTest {

    @Test
    void originalBytesOwnDefensiveCopiesAtBothApiBoundaries() throws Exception {
        byte[] callerOwned = {1, 2, 3};
        OriginalFdBytes original = OriginalFdBytes.copyOf(callerOwned, limitsWithMaximumBytes(3));

        callerOwned[0] = 99;
        byte[] returned = original.copyBytes();
        returned[1] = 88;

        assertAll(
                () -> assertEquals(3, original.size()),
                () -> assertArrayEquals(new byte[]{1, 2, 3}, original.copyBytes()),
                () -> assertTrue(original.contentEquals(new byte[]{1, 2, 3})),
                () -> assertFalse(original.contentEquals(callerOwned)),
                () -> assertFalse(original.contentEquals(null)),
                () -> assertEquals(
                        OriginalFdBytes.copyOf(new byte[]{1, 2, 3}, limitsWithMaximumBytes(3)),
                        original),
                () -> assertEquals(
                        OriginalFdBytes.copyOf(new byte[]{1, 2, 3}, limitsWithMaximumBytes(3)).hashCode(),
                        original.hashCode()),
                () -> assertNotEquals(
                        OriginalFdBytes.copyOf(new byte[]{1, 2, 4}, limitsWithMaximumBytes(3)),
                        original));
    }

    @Test
    void originalBytesAcceptTheExactBoundaryAndRejectBeforeCopyingPastIt() throws Exception {
        FdCodecLimits limits = limitsWithMaximumBytes(8);
        byte[] exact = new byte[8];

        assertEquals(8, OriginalFdBytes.copyOf(exact, limits).size());

        byte[] onePast = Arrays.copyOf(exact, 9);
        FdInputLimitException failure = assertThrows(
                FdInputLimitException.class,
                () -> OriginalFdBytes.copyOf(onePast, limits));
        assertAll(
                () -> assertEquals(8, failure.maximumBytes()),
                () -> assertEquals(9, failure.actualBytes()),
                () -> assertTrue(failure.getMessage().contains("9 bytes")),
                () -> assertTrue(failure.getMessage().contains("8 bytes")));
    }

    @Test
    void codecLimitsRejectNonPositiveAndIncoherentPolicies() {
        FdCodecLimits defaults = FdCodecLimits.defaults();
        assertAll(
                () -> assertTrue(defaults.maxDocumentBytes() > 0),
                () -> assertTrue(defaults.maxJsonTokens() > 0),
                () -> assertTrue(defaults.maxDiagnostics() > 0),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> limits(0, 32, 32, 16, 8, 8, 100)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> limits(8, 32, 32, 16, 8, 8, 0)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> limits(8, 32, 16, 17, 8, 8, 100)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> limits(8, 7, 32, 16, 8, 7, 100)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> limits(8, 7, 32, 16, 7, 8, 100)));
    }

    @Test
    void recursionAndSchemaLimitsMayBeTightenedButNotRelaxedPastSafeCaps() {
        FdCodecLimits defaults = FdCodecLimits.defaults();
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> replace(defaults, "maxWidgetDepth",
                                FdCodecLimits.DEFAULT_MAX_WIDGET_DEPTH + 1)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> replace(defaults, "maxExtensionNestingDepth",
                                FdCodecLimits.DEFAULT_MAX_EXTENSION_NESTING_DEPTH + 1)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> replace(defaults, "maxListChildren",
                                FdCodecLimits.DEFAULT_MAX_LIST_CHILDREN + 1)));
    }

    @Test
    void everyIndividualLimitMustBeStrictlyPositive() throws Exception {
        FdCodecLimits defaults = FdCodecLimits.defaults();
        RecordComponent[] components = FdCodecLimits.class.getRecordComponents();
        Class<?>[] parameterTypes = Arrays.stream(components)
                .map(RecordComponent::getType)
                .toArray(Class<?>[]::new);
        Constructor<FdCodecLimits> constructor =
                FdCodecLimits.class.getDeclaredConstructor(parameterTypes);
        Object[] valid = new Object[components.length];
        for (int index = 0; index < components.length; index++) {
            valid[index] = components[index].getAccessor().invoke(defaults);
        }

        for (int index = 0; index < components.length; index++) {
            Object[] invalid = valid.clone();
            if (components[index].getType() == long.class) {
                invalid[index] = 0L;
            } else {
                invalid[index] = 0;
            }
            String componentName = components[index].getName();
            InvocationTargetException failure = assertThrows(
                    InvocationTargetException.class,
                    () -> constructor.newInstance(invalid),
                    componentName);
            assertTrue(failure.getCause() instanceof IllegalArgumentException,
                    componentName + " must reject zero");
        }
    }

    @Test
    void codecRetainsTheExactLimitsInstance() {
        FdCodecLimits limits = limitsWithMaximumBytes(32);
        assertEquals(limits, new FdDocumentCodec(limits).limits());
    }

    private static FdCodecLimits limitsWithMaximumBytes(int maximumBytes) {
        return limits(maximumBytes, 32, 32, 16, 8, 8, 100);
    }

    private static FdCodecLimits limits(
            int maximumBytes,
            int jsonDepth,
            int stringUtf16Units,
            int stringCodePoints,
            int widgetDepth,
            int extensionDepth,
            long jsonTokens) {
        return new FdCodecLimits(
                maximumBytes,
                jsonDepth,
                jsonTokens,
                32,
                stringUtf16Units,
                stringCodePoints,
                32,
                32,
                widgetDepth,
                32,
                16,
                16,
                16,
                16,
                extensionDepth,
                64,
                32,
                32,
                8);
    }

    private static FdCodecLimits replace(
            FdCodecLimits source,
            String componentName,
            int replacement) throws ReflectiveOperationException {
        RecordComponent[] components = FdCodecLimits.class.getRecordComponents();
        Class<?>[] parameterTypes = Arrays.stream(components)
                .map(RecordComponent::getType)
                .toArray(Class<?>[]::new);
        Object[] arguments = new Object[components.length];
        for (int index = 0; index < components.length; index++) {
            arguments[index] = components[index].getName().equals(componentName)
                    ? replacement
                    : components[index].getAccessor().invoke(source);
        }
        try {
            return FdCodecLimits.class.getDeclaredConstructor(parameterTypes)
                    .newInstance(arguments);
        } catch (InvocationTargetException failure) {
            if (failure.getCause() instanceof IllegalArgumentException invalid) {
                throw invalid;
            }
            throw failure;
        }
    }
}
