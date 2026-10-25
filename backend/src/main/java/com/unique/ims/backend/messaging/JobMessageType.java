package com.unique.ims.backend.messaging;

/**
 * Routing keys for every message that can be published to the JobRunr broker.
 *
 * <p>This enum is the single source of truth for the messaging contract between
 * producers (REST controllers and domain services) and consumers
 * ({@code *JobConsumer} beans). Adding a value here automatically exposes it to
 * the manual-trigger endpoint of {@code BackgroundJobController}.</p>
 */
public enum JobMessageType {

    /** Full sweep of low-stock and expiry alerts across every product and batch. */
    ALERT_SCAN("Scan all inventory alerts", "Runs the complete low-stock + expiry scan", true),

    /** Low-stock re-check for a single product, published after stock leaves the warehouse. */
    LOW_STOCK_SCAN("Scan a single product for low stock", "Re-evaluates the reorder level of one product", true),

    /** Expiry re-check across every batch, published when a batch changes. */
    EXPIRY_SCAN("Scan stock batches for expiry", "Finds batches that expired or expire within 7 days", true),

    /**
     * Order confirmation follow-up after a sale is created or completed.
     * Produced by the sales workflow only — it needs a real order payload, so it
     * cannot be dispatched from the console.
     */
    ORDER_CONFIRMATION("Send order confirmation", "Notifies the customer that their order was registered", false),

    /** Stock level snapshot refresh after a purchase is received or a movement is recorded. */
    INVENTORY_SYNC("Refresh stock level snapshot", "Recomputes the current stock of one product", true),

    /** Daily inventory valuation digest. */
    DAILY_INVENTORY_REPORT("Daily inventory report", "Reports inventory value and low-stock exposure", true);

    private final String label;
    private final String description;
    private final boolean dispatchable;

    JobMessageType(String label, String description, boolean dispatchable) {
        this.label = label;
        this.description = description;
        this.dispatchable = dispatchable;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    /** Whether the console may publish this message on demand. */
    public boolean isDispatchable() {
        return dispatchable;
    }
}
