package com.vinicius.prazos.signatures.domain.entity;

import com.vinicius.prazos.documents.domain.entity.Document;
import com.vinicius.prazos.signatures.domain.enums.SignerStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "signers")
public class Signer {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "document_id", nullable = false)
	private Document document;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private String email;

	@Column(name = "signing_order")
	private Integer signingOrder;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private SignerStatus status;

	@Column(nullable = false, unique = true)
	private String token;

	@Column(name = "token_expires_at", nullable = false)
	private Instant tokenExpiresAt;

	@Column(name = "viewed_at")
	private Instant viewedAt;

	@Column(name = "signed_at")
	private Instant signedAt;

	@Column(name = "signature_value", columnDefinition = "text")
	private String signatureValue;

	@Column(name = "signature_hash")
	private String signatureHash;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@PrePersist
	void prePersist() {
		Instant now = Instant.now();
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	void preUpdate() {
		updatedAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Document getDocument() {
		return document;
	}

	public void setDocument(Document document) {
		this.document = document;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Integer getSigningOrder() {
		return signingOrder;
	}

	public void setSigningOrder(Integer signingOrder) {
		this.signingOrder = signingOrder;
	}

	public SignerStatus getStatus() {
		return status;
	}

	public void setStatus(SignerStatus status) {
		this.status = status;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public Instant getTokenExpiresAt() {
		return tokenExpiresAt;
	}

	public void setTokenExpiresAt(Instant tokenExpiresAt) {
		this.tokenExpiresAt = tokenExpiresAt;
	}

	public Instant getViewedAt() {
		return viewedAt;
	}

	public void setViewedAt(Instant viewedAt) {
		this.viewedAt = viewedAt;
	}

	public Instant getSignedAt() {
		return signedAt;
	}

	public void setSignedAt(Instant signedAt) {
		this.signedAt = signedAt;
	}

	public String getSignatureValue() {
		return signatureValue;
	}

	public void setSignatureValue(String signatureValue) {
		this.signatureValue = signatureValue;
	}

	public String getSignatureHash() {
		return signatureHash;
	}

	public void setSignatureHash(String signatureHash) {
		this.signatureHash = signatureHash;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}
}
