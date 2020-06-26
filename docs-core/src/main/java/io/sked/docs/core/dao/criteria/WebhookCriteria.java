package io.sked.docs.core.dao.criteria;

import io.sked.docs.core.constant.WebhookEvent;

/**
 * Webhook criteria.
 *
 * @author bgamard 
 */
public class WebhookCriteria {
    /**
     * Webhook event.
     */
    private WebhookEvent event;

    public WebhookEvent getEvent() {
        return event;
    }

    public WebhookCriteria setEvent(WebhookEvent event) {
        this.event = event;
        return this;
    }
}
