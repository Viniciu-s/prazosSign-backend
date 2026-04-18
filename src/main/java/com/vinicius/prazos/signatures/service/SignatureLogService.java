package com.vinicius.prazos.signatures.service;

import com.vinicius.prazos.documents.domain.entity.Document;
import com.vinicius.prazos.signatures.domain.entity.SignatureLog;
import com.vinicius.prazos.signatures.domain.entity.Signer;
import com.vinicius.prazos.signatures.domain.enums.SignatureLogEvent;
import com.vinicius.prazos.signatures.repository.SignatureLogRepository;
import org.springframework.stereotype.Service;

@Service
public class SignatureLogService {

	private final SignatureLogRepository signatureLogRepository;

	public SignatureLogService(SignatureLogRepository signatureLogRepository) {
		this.signatureLogRepository = signatureLogRepository;
	}

	public void log(
		Document document,
		Signer signer,
		SignatureLogEvent event,
		String description,
		String ipAddress,
		String userAgent
	) {
		SignatureLog log = new SignatureLog();
		log.setDocument(document);
		log.setSigner(signer);
		log.setEvent(event);
		log.setDescription(description);
		log.setIpAddress(ipAddress);
		log.setUserAgent(userAgent);
		signatureLogRepository.save(log);
	}
}
