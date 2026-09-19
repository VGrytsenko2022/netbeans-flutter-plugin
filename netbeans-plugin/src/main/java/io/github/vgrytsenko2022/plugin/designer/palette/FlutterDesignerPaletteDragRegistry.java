package io.github.vgrytsenko2022.plugin.designer.palette;

import io.github.vgrytsenko2022.designer.model.WidgetTypeId;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Per-Designer-view authority for short-lived Palette drag tokens.
 *
 * <p>The transferable contains only an opaque token. The authoritative widget
 * type remains in this JVM-local registry and can be consumed exactly once by
 * the owning view. Entries expire without a background task and the registry
 * refuses new entries at its configured bound.</p>
 */
public final class FlutterDesignerPaletteDragRegistry {
    static final String TOKEN_PREFIX = "nbfdnd:v1:";
    static final int TOKEN_LENGTH = TOKEN_PREFIX.length() + 36 + 1 + 36;
    static final Duration DEFAULT_TOKEN_LIFETIME = Duration.ofSeconds(30);
    static final int DEFAULT_MAX_OUTSTANDING = 64;
    private static final Duration MAX_TOKEN_LIFETIME = Duration.ofMinutes(5);
    private static final int ABSOLUTE_MAX_OUTSTANDING = 1_024;
    private static final int MAX_ID_ATTEMPTS = 16;

    private final UUID contextId;
    private final Clock clock;
    private final Duration tokenLifetime;
    private final int maxOutstanding;
    private final Supplier<UUID> dragIdSupplier;
    private final String contextPrefix;
    private final Map<UUID, Entry> entries = new LinkedHashMap<>();

    /** Creates one independent registry for a single active Designer view. */
    public FlutterDesignerPaletteDragRegistry() {
        this(
                UUID.randomUUID(),
                Clock.systemUTC(),
                DEFAULT_TOKEN_LIFETIME,
                DEFAULT_MAX_OUTSTANDING,
                UUID::randomUUID);
    }

    FlutterDesignerPaletteDragRegistry(
            UUID contextId,
            Clock clock,
            Duration tokenLifetime,
            int maxOutstanding,
            Supplier<UUID> dragIdSupplier) {
        this.contextId = Objects.requireNonNull(contextId, "contextId");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.tokenLifetime = requireTokenLifetime(tokenLifetime);
        this.maxOutstanding = requireMaximum(maxOutstanding);
        this.dragIdSupplier = Objects.requireNonNull(dragIdSupplier, "dragIdSupplier");
        contextPrefix = TOKEN_PREFIX + contextId + ':';
    }

    /**
     * Issues a bounded token for an authoritative widget type.
     *
     * @return empty when the non-expired registry is already full or a unique
     *     drag identifier cannot be allocated within the bounded attempt count
     */
    public synchronized Optional<String> issue(WidgetTypeId widgetType) {
        return issue(widgetType, false);
    }

    /**
     * Replaces every outstanding token owned by this view and issues the sole
     * token for the next Palette drag.
     *
     * <p>NetBeans' public Palette SPI exposes transferable customization but
     * no drag-end callback. Keeping one replace-on-start lease therefore makes
     * cancellation bounded and ensures that beginning another drag immediately
     * revokes any token retained by a canceled one. The remaining lease still
     * expires at the normal short deadline and is revoked with the view.</p>
     */
    public synchronized Optional<String> issueReplacingOutstanding(
            WidgetTypeId widgetType) {
        return issue(widgetType, true);
    }

    /**
     * Resolves one live token without consuming its one-shot authority.
     *
     * <p>This is the read-only half of two-phase Swing drop admission. Hover
     * and {@code PasteType} discovery may run repeatedly, so they must never
     * consume the token that the eventual drop commit needs. Malformed,
     * foreign, expired and revoked tokens remain indistinguishable.</p>
     */
    synchronized Optional<WidgetTypeId> resolve(String token) {
        Instant now = Objects.requireNonNull(clock.instant(), "clock.instant()");
        purgeExpired(now);
        Optional<UUID> dragId = localDragId(token);
        if (dragId.isEmpty()) {
            return Optional.empty();
        }
        Entry entry = entries.get(dragId.orElseThrow());
        if (entry == null || !now.isBefore(entry.expiresAt())) {
            return Optional.empty();
        }
        return Optional.of(entry.widgetType());
    }

    private Optional<String> issue(
            WidgetTypeId widgetType,
            boolean replaceOutstanding) {
        Objects.requireNonNull(widgetType, "widgetType");
        Instant now = Objects.requireNonNull(clock.instant(), "clock.instant()");
        purgeExpired(now);
        if (replaceOutstanding) {
            entries.clear();
        }
        if (entries.size() >= maxOutstanding) {
            return Optional.empty();
        }

        for (int attempt = 0; attempt < MAX_ID_ATTEMPTS; attempt++) {
            UUID dragId = Objects.requireNonNull(
                    dragIdSupplier.get(), "dragIdSupplier result");
            if (entries.containsKey(dragId)) {
                continue;
            }
            entries.put(dragId, new Entry(widgetType, now.plus(tokenLifetime)));
            return Optional.of(contextPrefix + dragId);
        }
        return Optional.empty();
    }

    /**
     * Consumes one token issued by this exact view.
     *
     * <p>Malformed, foreign, expired, revoked and already-consumed tokens are
     * rejected without affecting another live entry.</p>
     */
    public synchronized Optional<WidgetTypeId> consume(String token) {
        Instant now = Objects.requireNonNull(clock.instant(), "clock.instant()");
        purgeExpired(now);
        Optional<UUID> dragId = localDragId(token);
        if (dragId.isEmpty()) {
            return Optional.empty();
        }
        Entry entry = entries.remove(dragId.orElseThrow());
        if (entry == null || !now.isBefore(entry.expiresAt())) {
            return Optional.empty();
        }
        return Optional.of(entry.widgetType());
    }

    /**
     * Immediately invalidates one token when it belongs to this exact view.
     *
     * <p>Malformed and foreign-view values are rejected without changing any
     * live local entry. This exact-token operation is used by the AWT drag-end
     * lifecycle so an unrelated global drag can never revoke this view's
     * authority.</p>
     */
    public synchronized boolean revoke(String token) {
        Optional<UUID> dragId = localDragId(token);
        return dragId.isPresent() && entries.remove(dragId.orElseThrow()) != null;
    }

    /** Immediately invalidates every outstanding token owned by this view. */
    public synchronized void revokeAll() {
        entries.clear();
    }

    synchronized int outstandingCount() {
        purgeExpired(Objects.requireNonNull(clock.instant(), "clock.instant()"));
        return entries.size();
    }

    boolean hasLocalTokenShape(String token) {
        return token != null
                && token.length() == TOKEN_LENGTH
                && localDragId(token).isPresent();
    }

    private Optional<UUID> localDragId(String token) {
        if (token == null || !token.startsWith(contextPrefix)) {
            return Optional.empty();
        }
        String value = token.substring(contextPrefix.length());
        try {
            UUID parsed = UUID.fromString(value);
            return parsed.toString().equals(value)
                    ? Optional.of(parsed)
                    : Optional.empty();
        } catch (IllegalArgumentException malformed) {
            return Optional.empty();
        }
    }

    private void purgeExpired(Instant now) {
        Iterator<Entry> values = entries.values().iterator();
        while (values.hasNext()) {
            if (!now.isBefore(values.next().expiresAt())) {
                values.remove();
            }
        }
    }

    private static Duration requireTokenLifetime(Duration lifetime) {
        Objects.requireNonNull(lifetime, "tokenLifetime");
        if (lifetime.isZero()
                || lifetime.isNegative()
                || lifetime.compareTo(MAX_TOKEN_LIFETIME) > 0) {
            throw new IllegalArgumentException(
                    "tokenLifetime must be positive and no longer than "
                    + MAX_TOKEN_LIFETIME);
        }
        return lifetime;
    }

    private static int requireMaximum(int maximum) {
        if (maximum < 1 || maximum > ABSOLUTE_MAX_OUTSTANDING) {
            throw new IllegalArgumentException(
                    "maxOutstanding must be between 1 and "
                    + ABSOLUTE_MAX_OUTSTANDING);
        }
        return maximum;
    }

    private record Entry(WidgetTypeId widgetType, Instant expiresAt) {
        private Entry {
            Objects.requireNonNull(widgetType, "widgetType");
            Objects.requireNonNull(expiresAt, "expiresAt");
        }
    }
}
