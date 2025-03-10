package org.jj.Menus;

import net.miginfocom.swing.MigLayout;
import org.jetbrains.annotations.NotNull;
import org.jj.*;
import org.jj.Subscribers.TopOfBookSubscriberImpl;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class TradeMenu extends JFrame {
    private final JComboBox<String> productDropdown;
    private final JLabel selectedProductLabel;
    private static BuySell buySell;
    private final ClientProxy clientProxy;
    private static JTable orderBookTable;
    private final JLabel productQuantityOwned;
    private final JTextField quantityField;
    private final JTextField priceField;
    private final JComboBox<String> expiryDropdown;
    private String selectedProductSymbol;
    private TopOfBookSubscriberImpl topOfBookSubscriber;

    public TradeMenu() {
        this.clientProxy = ClientAccount.getInstance().getClientProxy();
        List<String> productSymbols = clientProxy.getTradingProductsList();

        setTitle("Trade Menu");
        setSize(700, 640); // Adjusted for compact UI
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        getContentPane().setBackground(new Color(30, 30, 30));
        setLayout(new MigLayout("insets 20", "[grow][grow]", "[]20[]20[]20[]"));

        // Title & Back Button (Same Row)
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setOpaque(false);

        JLabel title = new JLabel("Place Order");
        title.setFont(new Font("Arial", Font.BOLD, 40));
        title.setForeground(Color.WHITE);
        titlePanel.add(title, BorderLayout.WEST);

        JLabel productQuantityOwned = new JLabel();
        productQuantityOwned.setFont(new Font("Arial", Font.PLAIN, 24));
        productQuantityOwned.setForeground(new Color(225, 187, 6));
        productQuantityOwned.setHorizontalAlignment(SwingConstants.CENTER);
        this.productQuantityOwned = productQuantityOwned;
        titlePanel.add(productQuantityOwned, BorderLayout.CENTER);

        JButton backButton = createBackButton();
        titlePanel.add(backButton, BorderLayout.EAST);
        add(titlePanel, "span, growx, wrap");

        // Product Selection (Dropdown)
        List<String> productSymbolsList = new ArrayList<>();
        productSymbolsList.add("");
        productSymbolsList.addAll(productSymbols);
        productDropdown = new JComboBox<>(productSymbolsList.toArray(new String[0]));
        productDropdown.setFont(new Font("Arial", Font.BOLD, 16));
        productDropdown.setBackground(new Color(200, 200, 200));
        productDropdown.setForeground(Color.BLACK);

        productDropdown.addActionListener(e -> updateProductInfo());

        JLabel productDropdownLbl = new JLabel("Product: ");
        productDropdownLbl.setForeground(Color.WHITE);
        add (productDropdownLbl, "split 2, span");
        add(productDropdown, "growx, span, wrap");

        // Order Type: Buy / Sell Toggle Buttons
        JPanel orderTypePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 15));
        orderTypePanel.setOpaque(false);

        Color buyBtnBackgroundSecondaryColor = new Color(133, 229, 133);
        Color buyBtnBackgroundPrimaryColor = new Color(0, 200, 0);
        JToggleButton buyButton = createBuySellButton("Buy", buyBtnBackgroundPrimaryColor, buyBtnBackgroundSecondaryColor, BuySell.BUY);
        Color sellBtnBackgroundSecondaryColor = new Color(239, 154, 85);
        Color sellBtnBackgroundPrimaryColor = new Color(220, 80, 0);
        JToggleButton sellButton = createBuySellButton("Sell", sellBtnBackgroundPrimaryColor, sellBtnBackgroundSecondaryColor, BuySell.SELL);

        buyButton.addActionListener(event -> {
            buySell = BuySell.BUY;

            buyButton.setBackground(buyBtnBackgroundPrimaryColor);
            buyButton.repaint();

            Timer timer = new Timer(40, e -> {
                buyButton.setBackground(buyBtnBackgroundSecondaryColor);
                buyButton.repaint();

                sellButton.setBackground(sellBtnBackgroundPrimaryColor);
                sellButton.repaint();
            });

            timer.setRepeats(false);
            timer.start();
        });

        sellButton.addActionListener(event -> {
            buySell = BuySell.SELL;
            sellButton.setBackground(sellBtnBackgroundPrimaryColor);
            sellButton.repaint();

            Timer timer = new Timer(40, e -> {
                sellButton.setBackground(sellBtnBackgroundSecondaryColor);
                sellButton.repaint();

                buyButton.setBackground(buyBtnBackgroundPrimaryColor);
                buyButton.repaint();
            });

            timer.setRepeats(false);
            timer.start();
        });

        // Ensure only one button is selected
        ButtonGroup orderTypeGroup = new ButtonGroup();
        orderTypeGroup.add(buyButton);
        orderTypeGroup.add(sellButton);

        JLabel orderTypeLbl = new JLabel("Order Type:");
        orderTypeLbl.setForeground(Color.WHITE);
        orderTypePanel.add(orderTypeLbl);
        orderTypePanel.add(buyButton);
        orderTypePanel.add(sellButton);
        orderTypePanel.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));
        add(orderTypePanel, "span, wrap");

        // Amount & Price Fields
        JTextField quantityField = createInputField(0);
        JTextField priceField = createInputField(2);

        quantityField.setSize(new Dimension(500, 15));
        priceField.setSize(new Dimension(500, 15));

        JLabel quantityLbl = new JLabel("Quantity:");
        quantityLbl.setForeground(Color.WHITE);
        add(quantityLbl, "split 2, align left");
        add(quantityField, "align right, span, growx, wrap");

        JLabel priceLbl = new JLabel("Price:");
        priceLbl.setForeground(Color.WHITE);

        JButton confirmTradeButton = createConfirmTradeButton();

        this.quantityField = quantityField;
        this.priceField = priceField;
        add(quantityLbl, "align left");
        add(quantityField, "growx, pushx");
        add(confirmTradeButton, "spany 2, growy, wrap");  // Span vertically across 2 rows

        add(priceLbl, "align left");
        add(priceField, "growx, pushx, wrap");

        // Expiry Dropdown
        List<String> expiries = new ArrayList<>();
        expiries.add("");
        Arrays.stream(Expiry.values()).forEach(e -> expiries.add(e.toString()));
        JComboBox<String> expiryDropdown = new JComboBox<>(expiries.toArray(new String[0]));
        expiryDropdown.setForeground(Color.BLACK);
        expiryDropdown.setFont(new Font("Arial", Font.BOLD, 16));
        expiryDropdown.setBackground(new Color(200, 200, 200));

        JLabel expiryLbl = new JLabel("Expiry:");
        expiryLbl.setForeground(Color.WHITE);

        this.expiryDropdown = expiryDropdown;
        add(expiryLbl, "split 2, span");
        add(expiryDropdown, "growx, wrap");

        // Selected Product & Order Book Table (Right Panel)
        JPanel productInfoPanel = new JPanel(new BorderLayout());
        productInfoPanel.setOpaque(false);

        selectedProductLabel = new JLabel("Selected Product: -");
        selectedProductLabel.setFont(new Font("Arial", Font.BOLD, 18));
        selectedProductLabel.setForeground(Color.WHITE);
        productInfoPanel.add(selectedProductLabel, BorderLayout.NORTH);

        // Order Book Table (Structure in place, data will be filled dynamically)
        JScrollPane orderBookScrollPane = getjScrollPane();
        orderBookScrollPane.getViewport().setBackground(new Color(30, 30, 30));

        productInfoPanel.add(orderBookScrollPane, BorderLayout.CENTER);
        add(productInfoPanel, "span, growx, wrap");

        setVisible(true);
    }

    private @NotNull JButton createConfirmTradeButton() {
        JButton confirmTradeButton = new JButton("Confirm Trade");
        confirmTradeButton.setFont(new Font("Arial", Font.BOLD, 22));
        confirmTradeButton.setBackground(Color.ORANGE);
        confirmTradeButton.setForeground(Color.WHITE);
        confirmTradeButton.setFocusPainted(false);
        confirmTradeButton.setOpaque(true);
        confirmTradeButton.setContentAreaFilled(true);
        confirmTradeButton.setBorder(BorderFactory.createLineBorder(Color.RED, 3));
        confirmTradeButton.addActionListener(e -> makeTrade());

        confirmTradeButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                confirmTradeButton.setBackground(new Color(229, 126, 69));
                confirmTradeButton.repaint();
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                confirmTradeButton.setBackground(Color.ORANGE);
                confirmTradeButton.repaint();
            }
        });

        return confirmTradeButton;
    }

    private void makeTrade() {
        String productSymbol = (String) productDropdown.getSelectedItem();
        Expiry expiry;
        try {
            expiry = Expiry.valueOf((String) expiryDropdown.getSelectedItem());
        } catch (IllegalArgumentException e) {
            flashBackgroundColor(Color.red);
            return;
        }

        if (!priceField.getText().isEmpty() &&
            !quantityField.getText().isEmpty() &&
            !productSymbol.equals("") &&
            buySell != null) { // TODO on sell must check for ownership of stock
            double price = Double.parseDouble(priceField.getText());
            long quantity = Long.parseLong(quantityField.getText());

            if (buySell.equals(BuySell.SELL) && ClientAccount.getInstance().getQuantityOwned(productSymbol) < quantity) {
                flashBackgroundColor(Color.RED);
                return;
            }

            int orderId = clientProxy.createOrder(productSymbol, buySell, price, quantity, expiry);
            ClientAccount.getInstance().addOrderId(orderId);

            ClientAccount.getInstance().getOrders();

            setProductQuantityOwned(ClientAccount.getInstance().getQuantityOwned(productSymbol));

            flashBackgroundColor(Color.GREEN);
        } else {
            flashBackgroundColor(Color.RED);
        }

    }

    private void setProductQuantityOwned(Long quantity) {
        productQuantityOwned.setText(String.valueOf(quantity)); // TODO update with quantity owned
        productQuantityOwned.revalidate();
        productQuantityOwned.repaint();
    }

    private void flashBackgroundColor(Color color) {
        getContentPane().setBackground(color);
        getContentPane().repaint();

        Timer timer = new Timer(80, e -> {
            getContentPane().setBackground(new Color(30, 30, 30));
            getContentPane().repaint();
        });

        timer.setRepeats(false);
        timer.start();
    }

    private static JToggleButton createBuySellButton(String text, Color backgroundColor, Color backgroundColorOnMouseHover, BuySell buySell) {
        JToggleButton button = new JToggleButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 16));
        button.setBackground(backgroundColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(100, 38));
        button.setBorder(BorderFactory.createLineBorder(Color.WHITE, 0));
        button.setOpaque(true);
        button.setContentAreaFilled(true);

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(backgroundColorOnMouseHover);
                button.repaint();
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (buySell != TradeMenu.buySell) {
                    button.setBackground(backgroundColor);
                    button.repaint();
                }
            }
        });

        return button;
    }

    private @NotNull JScrollPane getjScrollPane() {
        String[] columnNames = {"Volume", "Bid", "Ask", "Volume"};

        DefaultTableModel model = new DefaultTableModel(columnNames, 0);

        JTable orderBookTable = new JTable(model);
        orderBookTable.setFont(new Font("Arial", Font.PLAIN, 16));
        orderBookTable.setBackground(new Color(70, 70, 70));
        orderBookTable.setForeground(Color.WHITE);
        orderBookTable.setRowHeight(30);

        TradeMenu.orderBookTable = orderBookTable;

        return new JScrollPane(orderBookTable);
    }

    private JButton createBackButton() {
        JButton button = new JButton("⬅ Back");
        button.setFont(new Font("Arial", Font.BOLD, 16));
        button.setForeground(Color.WHITE);
        button.setBackground(new Color(150, 0, 0));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(new Color(100, 0, 0), 2));
        button.setPreferredSize(new Dimension(100, 35));
        button.setOpaque(true);
        button.setContentAreaFilled(true);

        button.addActionListener(e -> {
            new ClientMain();
            dispose();
        });

        return button;
    }

    private JTextField createInputField(int allowedDecimalPlaces) {
        JTextField field = new NumericTextField(10, allowedDecimalPlaces);
        field.setFont(new Font("Arial", Font.PLAIN, 18));
        field.setBackground(Color.lightGray);
        field.setForeground(Color.BLACK);
        field.setBorder(BorderFactory.createLineBorder(new Color(100, 100, 100), 2));
        return field;
    }

    private void updateProductInfo() {
        if (selectedProductSymbol == productDropdown.getSelectedItem()) {
            return;
        }

        if (topOfBookSubscriber != null) {
            topOfBookSubscriber.unsubscribe();
        }

        selectedProductSymbol = (String) productDropdown.getSelectedItem();

        selectedProductLabel.setText("Selected Product: " + selectedProductSymbol);

        this.topOfBookSubscriber = new TopOfBookSubscriberImpl(clientProxy, selectedProductSymbol, this::onOrderBookChange);
    }

    private void onOrderBookChange(Service.OrderBook orderBook) {
        updateOrderBookTable(orderBook);
        setProductQuantityOwned(ClientAccount.getInstance().getQuantityOwned(selectedProductSymbol));
    }

    private static void updateOrderBookTable(Service.OrderBook orderBook) {
        DefaultTableModel tableModel = (DefaultTableModel) orderBookTable.getModel();
        tableModel.setRowCount(0);

        List<Long> buyQuantities = orderBook.getBuyQuantitiesList();
        List<Double> buyPrices = orderBook.getBuyPricesList();
        List<Double> sellPrices = orderBook.getSellPricesList();
        List<Long> sellQuantities = orderBook.getSellQuantitiesList();

        int maxRows = Math.min(Math.max(buyQuantities.size(), sellQuantities.size()), 5);

        for (int i = 0; i < maxRows; i++) {
            Object[] row = new Object[4];
            row[0] = (i < buyQuantities.size()) ? buyQuantities.get(i) : null;
            row[1] = (i < buyPrices.size()) ? buyPrices.get(i) : null;
            row[2] = (i < sellPrices.size()) ? sellPrices.get(i) : null;
            row[3] = (i < sellQuantities.size()) ? sellQuantities.get(i) : null;

            tableModel.addRow(row);
        }
    }
}