package my.edu.wix1002.goldenhour.model;

import java.util.Map;
import java.util.HashMap;

public class Model {
    private String modelId;
    private double price;
    private Map<String, Integer> stockByOutlet;

    public Model(String modelId, double price) {
        this.modelId = modelId;
        this.price = price;
        this.stockByOutlet = new HashMap<>();
    }

    public void addStock(String outletCode, int quantity) {
        stockByOutlet.put(outletCode, quantity);
    }

    public String getModelId() { return modelId; }
    public double getPrice() { return price; }
    public Map<String, Integer> getStockByOutlet() { return stockByOutlet; }

    @Override
    public String toString() {
        return "Model{" + modelId + ", price=" + price + ", stock=" + stockByOutlet + "}";
    }
}

