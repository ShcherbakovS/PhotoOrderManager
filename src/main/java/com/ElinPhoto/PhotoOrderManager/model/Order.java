package com.ElinPhoto.PhotoOrderManager.model;

import lombok.Data;
import org.springframework.stereotype.Component;

@Component
@Data
public class Order {
    private String orderText;
    private String orderConfirmURL;
    private String orderFlag;

    public String getOrderFlag() {
        if (orderFlag != null) {
            return orderFlag;
        } else {
            return "null";

        }
    }
}
