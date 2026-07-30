package com.example.honorcitizen.domain.shipping.entity;

import com.example.honorcitizen.common.entity.BaseTimeEntity;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;
import java.util.Optional;

@Entity
@Table(name = "shipping_addresses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShippingAddress extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long applicationId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String recipientName;

    @Column(nullable = false)
    private String zipCode;

    @Column(nullable = false)
    private String address;

    private String addressDetail;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private boolean isLocked;

    public static ShippingAddress create(
            Long applicationId,
            Long userId,
            String recipientName,
            String zipCode,
            String address,
            String addressDetail,
            String email,
            String phone) {
        ShippingAddress shippingAddress = new ShippingAddress();
        shippingAddress.applicationId = applicationId;
        shippingAddress.userId = userId;
        shippingAddress.recipientName = recipientName;
        shippingAddress.zipCode = zipCode;
        shippingAddress.address = address;
        shippingAddress.addressDetail = addressDetail;
        shippingAddress.email = email;
        shippingAddress.phone = phone;
        shippingAddress.isLocked = false;
        return shippingAddress;
    }

    public void lock() {
        this.isLocked = true;
    }

    public boolean isOwnedBy(Long userId) {
        return Objects.equals(this.userId, userId);
    }

    public void update(String recipientName, String zipCode, String address, String addressDetail, String email, String phone) {
        Optional.of(this)
                .filter(shippingAddress -> !shippingAddress.isLocked)
                .orElseThrow(() -> new CustomException(ErrorCode.SHIPPING_LOCKED));
        this.recipientName = recipientName;
        this.zipCode = zipCode;
        this.address = address;
        this.addressDetail = addressDetail;
        this.email = email;
        this.phone = phone;
    }
}
