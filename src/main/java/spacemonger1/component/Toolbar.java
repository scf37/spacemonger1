package spacemonger1.component;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import java.util.function.Supplier;

public interface Toolbar<Id> {
    void enable(Id id, boolean enabled);
    void toggle(Id id, boolean toggled);
    void reloadText();
    JComponent component();

    static <Id> Builder<Id> newBuilder() {
        return new Builder<>();
    }

    final class Builder<Id> {
        private final List<Item<Id>> items = new ArrayList<>();

        public Builder<Id> button(Id id, Icon icon, Supplier<String> text, Runnable onClick) {
            Objects.requireNonNull(id, "id");
            items.add(Item.button(id, icon, text, onClick));
            return this;
        }

        public Builder<Id> toggle(Id id, Icon icon, Supplier<String> text, Consumer<Boolean> onToggle) {
            Objects.requireNonNull(id, "id");
            items.add(Item.toggle(id, icon, text, onToggle));
            return this;
        }

        public Builder<Id> separator() {
            items.add(Item.separator());
            return this;
        }

        public Toolbar<Id> build() {
            ToolbarImpl<Id> impl = new ToolbarImpl<>();
            Map<Id, AbstractButton> buttons = new HashMap<>();

            for (Item<Id> item : items) {
                if (item.id == null) {
                    impl.addSeparator();
                    continue;
                }
                AbstractButton button = item.toggle
                    ? new JToggleButton(item.text.get())
                    : new JButton(item.text.get());

                Runnable prev = impl.reloadText;
                impl.reloadText = () -> { button.setText(item.text.get()); prev.run(); };

                button.setFocusable(false);
                if (item.icon != null) {
                    button.setIcon(item.icon);
                }

                // event wiring
                if (item.toggle && item.onToggle != null) {
                    button.addActionListener(e ->
                                                 item.onToggle.accept(button.getModel().isSelected())
                    );
                } else if (!item.toggle && item.onClick != null) {
                    button.addActionListener(e ->
                                                 item.onClick.run()
                    );
                }

                impl.add(button);
                buttons.put(item.id, button);
            }
            impl.setFloatable(false);

            impl.buttons = buttons;
            return impl;
        }

        // internal builder item holder
        private record Item<Id>(
            Id id,
            Icon icon,
            Supplier<String> text,
            boolean toggle,
            Runnable onClick,
            Consumer<Boolean> onToggle
        ) {
            static <Id> Item<Id> button(Id id, Icon icon, Supplier<String> text, Runnable onClick) {
                return new Item<>(id, icon, text, false, onClick, null);
            }
            static <Id> Item<Id> toggle(Id id, Icon icon, Supplier<String> text, Consumer<Boolean> onToggle) {
                return new Item<>(id, icon, text, true, null, onToggle);
            }

            public static <Id> Item<Id> separator() {
                return new Item<>(null, null, null, false, null, null);
            }
        }
    }
}

class ToolbarImpl<Id> extends JToolBar implements Toolbar<Id> {
    Map<Id, AbstractButton> buttons;
    Runnable reloadText = () -> { };

    @Override
    public void enable(Id id, boolean enabled) {
        AbstractButton btn = buttons.get(id);
        if (btn != null) btn.setEnabled(enabled);
    }

    @Override
    public void toggle(Id id, boolean toggled) {
        AbstractButton btn = buttons.get(id);
        if (btn instanceof JToggleButton) {
            btn.getModel().setSelected(toggled);
        }
    }

    @Override
    public void reloadText() {
        this.reloadText.run();
    }

    @Override
    public JComponent component() {
        return this;
    }
}
