package my.edu.wix1002.goldenhour.model;

public class Outlet {
    private String outletCode;
    private String outletName;

    public Outlet(String outletCode, String outletName) {
        this.outletCode = outletCode;
        this.outletName = outletName;
    }

    // Getters
    public String getOutletCode() { return outletCode; }
    public String getOutletName() { return outletName; }

    @Override
    public String toString() {
        return "Outlet{" + outletCode + ": " + outletName + "}";
    }
}
