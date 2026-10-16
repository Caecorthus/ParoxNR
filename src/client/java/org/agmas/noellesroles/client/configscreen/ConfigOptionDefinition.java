package org.agmas.noellesroles.client.configscreen;

import net.minecraft.text.Text;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

public abstract class ConfigOptionDefinition<T> {
    private final String id;
    private final Text label;
    private final Text description;
    private final ConfigOptionType type;
    private final Function<ConfigScreenState, T> defaultValueGetter;

    protected ConfigOptionDefinition(
            String id,
            Text label,
            Text description,
            ConfigOptionType type,
            Function<ConfigScreenState, T> defaultValueGetter
    ) {
        this.id = id;
        this.label = label;
        this.description = description;
        this.type = type;
        this.defaultValueGetter = defaultValueGetter;
    }

    public String id() {
        return id;
    }

    public Text label() {
        return label;
    }

    public Text description() {
        return description;
    }

    public ConfigOptionType type() {
        return type;
    }

    public boolean matchesFilter(String filter) {
        if (filter == null || filter.isBlank()) {
            return true;
        }
        String loweredFilter = filter.toLowerCase(Locale.ROOT);
        return id.toLowerCase(Locale.ROOT).contains(loweredFilter)
                || label.getString().toLowerCase(Locale.ROOT).contains(loweredFilter)
                || description.getString().toLowerCase(Locale.ROOT).contains(loweredFilter);
    }

    public abstract T getValue(ConfigScreenState state);

    public abstract void setValue(ConfigScreenState state, T value);

    public T getDefaultValue(ConfigScreenState defaults) {
        return defaultValueGetter.apply(defaults);
    }

    public static ToggleOptionDefinition toggle(
            String id,
            Text label,
            Text description,
            Function<ConfigScreenState, Boolean> defaultValueGetter,
            Function<ConfigScreenState, Boolean> getter,
            BiConsumer<ConfigScreenState, Boolean> setter
    ) {
        return new ToggleOptionDefinition(id, label, description, defaultValueGetter, getter, setter);
    }

    public static NumberOptionDefinition number(
            String id,
            Text label,
            Text description,
            int minValue,
            int maxValue,
            Function<ConfigScreenState, Integer> defaultValueGetter,
            Function<ConfigScreenState, Integer> getter,
            BiConsumer<ConfigScreenState, Integer> setter
    ) {
        return new NumberOptionDefinition(id, label, description, minValue, maxValue, defaultValueGetter, getter, setter);
    }

    public static TextOptionDefinition text(
            String id,
            Text label,
            Text description,
            int maxLength,
            Text placeholder,
            Function<ConfigScreenState, String> defaultValueGetter,
            Function<ConfigScreenState, String> getter,
            BiConsumer<ConfigScreenState, String> setter
    ) {
        return new TextOptionDefinition(id, label, description, maxLength, placeholder, defaultValueGetter, getter, setter);
    }

    public static NumberInputOptionDefinition numberInput(
            String id,
            Text label,
            Text description,
            int maxDigits,
            Function<ConfigScreenState, Integer> defaultValueGetter,
            Function<ConfigScreenState, Integer> getter,
            BiConsumer<ConfigScreenState, Integer> setter
    ) {
        return new NumberInputOptionDefinition(id, label, description, maxDigits, defaultValueGetter, getter, setter);
    }

    public static <E> EnumOptionDefinition<E> enumeration(
            String id,
            Text label,
            Text description,
            List<E> values,
            Function<E, Text> valueTextFactory,
            Function<ConfigScreenState, E> defaultValueGetter,
            Function<ConfigScreenState, E> getter,
            BiConsumer<ConfigScreenState, E> setter
    ) {
        return new EnumOptionDefinition<>(id, label, description, values, valueTextFactory, defaultValueGetter, getter, setter);
    }

    public static final class ToggleOptionDefinition extends ConfigOptionDefinition<Boolean> {
        private final Function<ConfigScreenState, Boolean> getter;
        private final BiConsumer<ConfigScreenState, Boolean> setter;

        private ToggleOptionDefinition(
                String id,
                Text label,
                Text description,
                Function<ConfigScreenState, Boolean> defaultValueGetter,
                Function<ConfigScreenState, Boolean> getter,
                BiConsumer<ConfigScreenState, Boolean> setter
        ) {
            super(id, label, description, ConfigOptionType.TOGGLE, defaultValueGetter);
            this.getter = getter;
            this.setter = setter;
        }

        @Override
        public Boolean getValue(ConfigScreenState state) {
            return getter.apply(state);
        }

        @Override
        public void setValue(ConfigScreenState state, Boolean value) {
            setter.accept(state, value);
        }
    }

    public static final class NumberOptionDefinition extends ConfigOptionDefinition<Integer> {
        private final int minValue;
        private final int maxValue;
        private final Function<ConfigScreenState, Integer> getter;
        private final BiConsumer<ConfigScreenState, Integer> setter;

        private NumberOptionDefinition(
                String id,
                Text label,
                Text description,
                int minValue,
                int maxValue,
                Function<ConfigScreenState, Integer> defaultValueGetter,
                Function<ConfigScreenState, Integer> getter,
                BiConsumer<ConfigScreenState, Integer> setter
        ) {
            super(id, label, description, ConfigOptionType.NUMBER, defaultValueGetter);
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.getter = getter;
            this.setter = setter;
        }

        public int minValue() {
            return minValue;
        }

        public int maxValue() {
            return maxValue;
        }

        @Override
        public Integer getValue(ConfigScreenState state) {
            return getter.apply(state);
        }

        @Override
        public void setValue(ConfigScreenState state, Integer value) {
            setter.accept(state, Math.max(minValue, Math.min(maxValue, value)));
        }
    }

    public static final class NumberInputOptionDefinition extends ConfigOptionDefinition<Integer> {
        private final int maxDigits;
        private final Function<ConfigScreenState, Integer> getter;
        private final BiConsumer<ConfigScreenState, Integer> setter;

        private NumberInputOptionDefinition(
                String id,
                Text label,
                Text description,
                int maxDigits,
                Function<ConfigScreenState, Integer> defaultValueGetter,
                Function<ConfigScreenState, Integer> getter,
                BiConsumer<ConfigScreenState, Integer> setter
        ) {
            super(id, label, description, ConfigOptionType.NUMBER, defaultValueGetter);
            this.maxDigits = maxDigits;
            this.getter = getter;
            this.setter = setter;
        }

        public int maxDigits() {
            return maxDigits;
        }

        @Override
        public Integer getValue(ConfigScreenState state) {
            return getter.apply(state);
        }

        @Override
        public void setValue(ConfigScreenState state, Integer value) {
            setter.accept(state, value);
        }
    }

    public static final class TextOptionDefinition extends ConfigOptionDefinition<String> {
        private final int maxLength;
        private final Text placeholder;
        private final Function<ConfigScreenState, String> getter;
        private final BiConsumer<ConfigScreenState, String> setter;

        private TextOptionDefinition(
                String id,
                Text label,
                Text description,
                int maxLength,
                Text placeholder,
                Function<ConfigScreenState, String> defaultValueGetter,
                Function<ConfigScreenState, String> getter,
                BiConsumer<ConfigScreenState, String> setter
        ) {
            super(id, label, description, ConfigOptionType.TEXT, defaultValueGetter);
            this.maxLength = maxLength;
            this.placeholder = Objects.requireNonNullElseGet(placeholder, Text::empty);
            this.getter = getter;
            this.setter = setter;
        }

        public int maxLength() {
            return maxLength;
        }

        public Text placeholder() {
            return placeholder;
        }

        @Override
        public String getValue(ConfigScreenState state) {
            return getter.apply(state);
        }

        @Override
        public void setValue(ConfigScreenState state, String value) {
            setter.accept(state, value == null ? "" : value);
        }
    }

    public static final class EnumOptionDefinition<E> extends ConfigOptionDefinition<E> {
        private final List<E> values;
        private final Function<E, Text> valueTextFactory;
        private final Function<ConfigScreenState, E> getter;
        private final BiConsumer<ConfigScreenState, E> setter;

        private EnumOptionDefinition(
                String id,
                Text label,
                Text description,
                List<E> values,
                Function<E, Text> valueTextFactory,
                Function<ConfigScreenState, E> defaultValueGetter,
                Function<ConfigScreenState, E> getter,
                BiConsumer<ConfigScreenState, E> setter
        ) {
            super(id, label, description, ConfigOptionType.ENUM, defaultValueGetter);
            this.values = List.copyOf(values);
            this.valueTextFactory = valueTextFactory;
            this.getter = getter;
            this.setter = setter;
        }

        public List<E> values() {
            return values;
        }

        public Text getValueText(E value) {
            return valueTextFactory.apply(value);
        }

        @Override
        public E getValue(ConfigScreenState state) {
            return getter.apply(state);
        }

        @Override
        public void setValue(ConfigScreenState state, E value) {
            setter.accept(state, value);
        }
    }
}
