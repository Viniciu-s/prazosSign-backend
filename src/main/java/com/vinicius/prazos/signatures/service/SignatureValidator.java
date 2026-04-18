package com.vinicius.prazos.signatures.service;

import com.vinicius.prazos.documents.domain.entity.Document;
import com.vinicius.prazos.signatures.domain.entity.Signer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

@Component
public class SignatureValidator {

	public String generateSignatureHash(Document document, Signer signer) {
		if (signer.getSignatureValue() == null || signer.getSignedAt() == null) {
			throw new IllegalArgumentException("Assinatura ainda não foi capturada");
		}

		String payload = String.join(
			"|",
			String.valueOf(document.getId()),
			document.getTitle(),
			document.getContent(),
			signer.getEmail(),
			signer.getName(),
			signer.getSignatureValue(),
			signer.getSignedAt().toString()
		);

		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash);
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("Algoritmo de validação indisponível", exception);
		}
	}

	public boolean isValid(Document document, Signer signer) {
		if (signer.getSignatureHash() == null || signer.getSignatureValue() == null || signer.getSignedAt() == null) {
			return false;
		}

		if (signer.getTokenExpiresAt() != null && signer.getTokenExpiresAt().isBefore(Instant.now()) && signer.getSignedAt() == null) {
			return false;
		}

		return signer.getSignatureHash().equals(generateSignatureHash(document, signer));
	}
}
