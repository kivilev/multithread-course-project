package dev.sorokin.domain;

import dev.sorokin.service.converter.PaymentStatusConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@ToString(of = {"id", "paymentStatus", "customerId"})
public class OrderEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "address")
    private String address;

    @Column(name = "customer_id", nullable = false, updatable = false)
    Long customerId;

    @Column(name = "status", nullable = false)
    @Convert(converter = PaymentStatusConverter.class)
    private PaymentStatus paymentStatus;

    @Column(name = "client_estimate", nullable = false, precision = 19, scale = 2)
    private BigDecimal clientEstimate;

    @Column(name = "final_amount", precision = 19, scale = 2)
    private BigDecimal finalAmount;

    @Column(name = "authorized_amount", precision = 19, scale = 2)
    private BigDecimal authorizedAmount;

    @Column(name = "captured_amount", precision = 19, scale = 2)
    private BigDecimal capturedAmount;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "failure_code")
    private String failureCode;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
