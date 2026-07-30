package com.example.honorcitizen.domain.bulk.entity;

import com.example.honorcitizen.common.entity.BaseTimeEntity;
import com.example.honorcitizen.common.enums.BulkOrderStatus;
import com.example.honorcitizen.common.enums.CardType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "bulk_orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BulkOrder extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CardType cardType;

    @Column(nullable = false)
    private int totalCount;

    @Column(length = 255)
    private String originalFilename;

    @Column(length = 500)
    private String zipPath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BulkOrderStatus status = BulkOrderStatus.DRAFT;

    public static BulkOrder create(Long userId, int totalCount, String originalFilename, CardType cardType) {
        BulkOrder order = new BulkOrder();
        order.userId = userId;
        order.totalCount = totalCount;
        order.originalFilename = originalFilename;
        order.cardType = cardType;
        order.status = BulkOrderStatus.DRAFT;
        return order;
    }
}
