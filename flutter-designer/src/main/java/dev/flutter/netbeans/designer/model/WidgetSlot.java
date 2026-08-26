package dev.flutter.netbeans.designer.model;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** A single-valued or ordered list-valued named widget slot. */
public sealed interface WidgetSlot permits WidgetSlot.SingleSlot, WidgetSlot.ListSlot {
    int MAX_LIST_CHILDREN = 10_000;

    SlotCardinality cardinality();

    record SingleSlot(Optional<WidgetNode> child) implements WidgetSlot {
        public SingleSlot {
            Objects.requireNonNull(child, "child");
        }

        public static SingleSlot empty() {
            return new SingleSlot(Optional.empty());
        }

        public static SingleSlot of(WidgetNode child) {
            return new SingleSlot(Optional.of(Objects.requireNonNull(child, "child")));
        }

        @Override
        public SlotCardinality cardinality() {
            return SlotCardinality.SINGLE;
        }
    }

    record ListSlot(List<WidgetNode> children) implements WidgetSlot {
        public ListSlot {
            Objects.requireNonNull(children, "children");
            if (children.size() > MAX_LIST_CHILDREN) {
                throw new IllegalArgumentException(
                        "List slot cannot contain more than " + MAX_LIST_CHILDREN + " children");
            }
            children = List.copyOf(children);
        }

        @Override
        public SlotCardinality cardinality() {
            return SlotCardinality.LIST;
        }
    }
}
