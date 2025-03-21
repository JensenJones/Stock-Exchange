package org.jj.matchingEngine;

import org.jj.Match;
import org.jj.providers.TimestampProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class OrderBookSide {
    protected static final Logger LOGGER = LoggerFactory.getLogger(OrderBookSide.class);

    protected final List<OrdersAtPrice> ordersByPrice = new LinkedList<>();
    protected final Map<Integer, Node> idToNode = new HashMap<Integer, Node>();
    private final TimestampProvider timestampProvider;
    private final Comparator<Long> priceComparator;

    public OrderBookSide(TimestampProvider timestampProvider, Comparator<Long> priceComparator) {
        this.timestampProvider = timestampProvider;
        this.priceComparator = priceComparator;
    }

    public void addOrder(int id, long quantity, long quantityFilled, long price) {
        Node node = new Node(id, quantity, quantityFilled, price);
        idToNode.put(id, node);

        ListIterator<OrdersAtPrice> iterator = ordersByPrice.listIterator();

        boolean added = false;
        while (iterator.hasNext()) {
            OrdersAtPrice current = iterator.next();
            if (current.price == price) {
                current.add(node);
                return;
            } else if (priceComparator.compare(current.price, price) > 0){
                OrdersAtPrice newList = new OrdersAtPrice(node);
                iterator.previous();
                iterator.add(newList);
                return;
            }
        }

        if (!added) {
            ordersByPrice.add(new OrdersAtPrice(node));
        } else {
            LOGGER.error("ORDER NEVER ADDED TO ORDER BOOK | uuid = {} | quantity = {} | price = {} |", id, quantity, price);
        }
    }

    public boolean hasOrder(int id) {
        return idToNode.containsKey(id);
    }

    public boolean removeOrder(int id) {
        Node node = idToNode.remove(id);

        assert (node != null);
        ListIterator<OrdersAtPrice> iterator = ordersByPrice.listIterator();
        while (iterator.hasNext()) {
            OrdersAtPrice ordersAtPrice = iterator.next();
            if (ordersAtPrice.price == node.price) {
                ordersAtPrice.removeNode(node);
                if (ordersAtPrice.head == null) {
                    iterator.remove();
                }
                return true;
            }
        }
        return false;
    }

    public long matchOrder(int id, long quantity, long price) {
        long quantityTraded = 0;
        ListIterator<OrdersAtPrice> iterator = ordersByPrice.listIterator();

        while (iterator.hasNext()) {
            OrdersAtPrice ordersAtPrice = iterator.next();

            if (priceComparator.compare(ordersAtPrice.getPrice(), price) > 0) {
                break;
            }

            Node currentNode = ordersAtPrice.gethead();
            while (currentNode != null) {
                quantityTraded += trade(id, quantity - quantityTraded, currentNode);

                if (currentNode.getQuantityRemaining() == 0) {
                    idToNode.remove(currentNode.getId());
                    ordersAtPrice.removeNode(currentNode);
                    if (ordersAtPrice.gethead() == null) {
                        iterator.remove();
                    }
                }

                if (quantityTraded == quantity) {
                    break;
                }
                currentNode = currentNode.getNext();
            }
        }
        return quantityTraded;
    }

    public Boolean matchMustFillOrder(int id, long quantity, long price) {
        long fillable = 0;

        for (OrdersAtPrice ordersAtPrice : ordersByPrice) {
            if (priceComparator.compare(ordersAtPrice.getPrice(), price) > 0) {
                break;
            }

            Node currentNode = ordersAtPrice.gethead();

            while (currentNode != null) {
                fillable += currentNode.getQuantityRemaining();
                currentNode = currentNode.next;
            }

            if (fillable >= quantity) {
                break;
            }
        }

        if (fillable < quantity) {
            return false;
        }

        matchOrder(id, quantity, price);
        return true;
    }

    public AbstractMap.SimpleEntry<List<Long>, List<Long>> getFiveBestOrdersAndQuantitiesList() {
        List<Long> prices = new ArrayList<>();
        List<Long> quantities = new ArrayList<>();

        for (int i = 0; i < Math.min(ordersByPrice.size(), 5); i++) {
            OrdersAtPrice ordersAtPrice = ordersByPrice.get(i);
            prices.add(ordersAtPrice.getPrice());
            quantities.add(ordersAtPrice.getTotalBuyQuantity());
        }

        return new AbstractMap.SimpleEntry<>(prices, quantities);
    }

    protected long trade(int id, long quantityRemaining, Node current) {
        long tradeQuantity = Math.min(quantityRemaining, current.getQuantityRemaining());
        // TODO DO SOMETHING WITH THIS MATCH
        new Match(id, current.getId(), tradeQuantity, current.getPrice(), timestampProvider.getTimestamp());
        current.trade(tradeQuantity);
        return tradeQuantity;
    }

    protected static class OrdersAtPrice {

        private Node head;
        private Node tail;
        private final long price;

        public OrdersAtPrice(Node head) {
            this.head = head;
            this.tail = head;
            price = head.price;
        }

        public void add(Node node) {
            tail.next = node;
            node.prev = tail;
            tail = node;
        }

        public long getPrice() {
            return price;
        }

        // Ensures Head and Tail of OrdersAtPrice are kept when either head or tail node are removed
        public void removeNode(Node node) {
            if (head == node) {
                head = node.next;
            }
            if (tail == node) {
                tail = node.prev;
            }
            node.unlink();
        }

        public Node gethead() {
            return head;
        }

        public long getTotalBuyQuantity() {
            long total = 0;

            Node temp = head;
            while (temp != null) {
                total += temp.getQuantityRemaining();
                temp = temp.getNext();
            }

            return total;
        }
    }

    protected static class Node {
        private final int id;
        private final long quantity;
        private long quantityFilled = 0;
        private final long price;
        private Node next;
        private Node prev;

        public Node(int id, long quantity, long quantityFilled, long price) {
            this.quantity = quantity;
            this.price = price;
            this.quantityFilled = quantityFilled;
            this.id = id;
        }

        public void unlink() {
            if (next != null) {
                next.prev = prev;
            }
            if (prev != null) {
                prev.next = next;
            }
            prev = next = null;
        }

        public void fillOrder(long quantity) {
            quantityFilled += quantity;
        }

        public Node getNext() {
            return next;
        }

        public long getQuantityRemaining() {
            return quantity - quantityFilled;
        }

        public int getId() {
            return id;
        }

        public void trade(long tradingQuantity) {
            quantityFilled += tradingQuantity;
        }

        public long getPrice() {
            return price;
        }

        @Override
        public String toString() {
            return "Node{" +
                    "id=" + id +
                    ", quantity=" + quantity +
                    ", quantityFilled=" + quantityFilled +
                    ", price=" + price +
                    ", next=" + next +
                    ", prev=" + prev +
                    '}';
        }
    }
}
