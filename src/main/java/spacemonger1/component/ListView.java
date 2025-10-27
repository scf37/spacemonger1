package spacemonger1.component;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public interface ListView<Id> {

    record Item<Id>(Id id, Icon icon, String text) {
        public Item {
            Objects.requireNonNull(id, "ID cannot be null");
            Objects.requireNonNull(text, "Text cannot be null");
        }
    }

    static <Id> ListView<Id> newInstance(
            List<Item<Id>> initialItems,
            int cellWidth,
            int cellHeight,
            Consumer<Item<Id>> onConfirmSelection) {
        Objects.requireNonNull(initialItems, "Initial items cannot be null");
        return new ListViewImpl<>(initialItems, cellWidth, cellHeight, onConfirmSelection);
    }

    JPanel component();
    List<Item<Id>> getItems();
    Item<Id> getSelectedItem();
}

class ListViewImpl<Id> extends JPanel implements ListView<Id> {
    private List<ListView.Item<Id>> items;
    private final JList<ListView.Item<Id>> list;
    private final DefaultListModel<ListView.Item<Id>> model;
    private final Consumer<ListView.Item<Id>> confirmListener;

    ListViewImpl(
            List<ListView.Item<Id>> initialItems,
            int cellWidth,
            int cellHeight,
            Consumer<ListView.Item<Id>> confirmListener) {

        super(new BorderLayout());
        this.items = List.copyOf(initialItems);
        this.confirmListener = confirmListener;
        this.model = new DefaultListModel<>();
        this.list = new JList<>(model);

        this.items.forEach(model::addElement);

        // Configure list
        this.list.setCellRenderer(new IconListCellRenderer());
        this.list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.list.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        this.list.setVisibleRowCount(0);
        this.list.setFixedCellWidth(cellWidth);
        this.list.setFixedCellHeight(cellHeight);

        // Handle confirmation: double-click OR Enter key
        if (confirmListener != null) {
            // Double-click
            this.list.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        confirmSelection();
                    }
                }
            });

            // Enter key
            String enterActionKey = "confirmSelection";
            this.list.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                    .put(KeyStroke.getKeyStroke("ENTER"), enterActionKey);
            this.list.getActionMap().put(enterActionKey, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    confirmSelection();
                }
            });
        }

        add(new JScrollPane(list), BorderLayout.CENTER);
    }

    private void confirmSelection() {
        ListView.Item<Id> selectedItem = list.getSelectedValue();
        if (selectedItem != null) {
            confirmListener.accept(selectedItem);
        }
    }

    @Override
    public JPanel component() {
        return this;
    }

    @Override
    public List<Item<Id>> getItems() {
        return items;
    }

    @Override
    public Item<Id> getSelectedItem() {
        return list.getSelectedValue();
    }

    private static class IconListCellRenderer extends JPanel implements ListCellRenderer<ListView.Item<?>> {
        private final JLabel iconLabel = new JLabel();
        private final JLabel textLabel = new JLabel();

        IconListCellRenderer() {
            setLayout(new BorderLayout());
            iconLabel.setHorizontalAlignment(JLabel.CENTER);
            textLabel.setHorizontalAlignment(JLabel.CENTER);
            add(iconLabel, BorderLayout.CENTER);
            add(textLabel, BorderLayout.SOUTH);
            setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends ListView.Item<?>> list,
                                                      ListView.Item<?> value, int index, boolean isSelected, boolean cellHasFocus) {

            iconLabel.setIcon(value.icon());
            textLabel.setText(value.text());

            Color background = isSelected ? list.getSelectionBackground() : list.getBackground();
            Color foreground = isSelected ? list.getSelectionForeground() : list.getForeground();

            setBackground(background);
            setForeground(foreground);
            iconLabel.setForeground(foreground);
            textLabel.setForeground(foreground);

            return this;
        }
    }
}