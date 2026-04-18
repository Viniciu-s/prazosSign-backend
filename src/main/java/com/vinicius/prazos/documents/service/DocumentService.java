package com.vinicius.prazos.documents.service;

import com.vinicius.prazos.auth.domain.entity.User;
import com.vinicius.prazos.auth.security.CustomUserDetailsService;
import com.vinicius.prazos.documents.domain.dto.DocumentMoveRequest;
import com.vinicius.prazos.documents.domain.dto.DocumentRequest;
import com.vinicius.prazos.documents.domain.dto.DocumentResponse;
import com.vinicius.prazos.documents.domain.entity.Document;
import com.vinicius.prazos.documents.domain.enums.DocumentStatus;
import com.vinicius.prazos.documents.repository.DocumentRepository;
import com.vinicius.prazos.groups.domain.entity.Group;
import com.vinicius.prazos.groups.repository.GroupRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DocumentService {

	private final DocumentRepository documentRepository;
	private final GroupRepository groupRepository;
	private final CustomUserDetailsService userDetailsService;

	public DocumentService(
		DocumentRepository documentRepository,
		GroupRepository groupRepository,
		CustomUserDetailsService userDetailsService
	) {
		this.documentRepository = documentRepository;
		this.groupRepository = groupRepository;
		this.userDetailsService = userDetailsService;
	}

	@Transactional(readOnly = true)
	public List<DocumentResponse> listDocuments(String email, String status) {
		User user = userDetailsService.loadDomainUserByEmail(email);
		DocumentStatus filter = parseStatus(status);

		List<Document> documents = filter == null
			? documentRepository.findAllByUserIdOrderByUpdatedAtDesc(user.getId())
			: documentRepository.findAllByUserIdAndStatusOrderByUpdatedAtDesc(user.getId(), filter);

		return documents.stream()
			.map(this::toResponse)
			.toList();
	}

	@Transactional(readOnly = true)
	public DocumentResponse getDocument(Long id, String email) {
		User user = userDetailsService.loadDomainUserByEmail(email);
		return toResponse(loadOwnedDocument(id, user.getId()));
	}

	@Transactional
	public DocumentResponse createDocument(String email, DocumentRequest request) {
		User user = userDetailsService.loadDomainUserByEmail(email);

		Document document = new Document();
		document.setUser(user);
		document.setTitle(normalizeTitle(request.title()));
		document.setContent(normalizeContent(request.content()));
		document.setStatus(DocumentStatus.RASCUNHO);

		return toResponse(documentRepository.save(document));
	}

	@Transactional
	public DocumentResponse updateDocument(Long id, String email, DocumentRequest request) {
		User user = userDetailsService.loadDomainUserByEmail(email);
		Document document = loadOwnedDocument(id, user.getId());

		if (document.getStatus() != DocumentStatus.RASCUNHO) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Documento enviado não pode ser editado");
		}

		document.setTitle(normalizeTitle(request.title()));
		document.setContent(normalizeContent(request.content()));

		return toResponse(documentRepository.save(document));
	}

	@Transactional
	public void deleteDocument(Long id, String email) {
		User user = userDetailsService.loadDomainUserByEmail(email);
		Document document = loadOwnedDocument(id, user.getId());
		documentRepository.delete(document);
	}

	@Transactional
	public DocumentResponse sendDocument(Long id, String email) {
		User user = userDetailsService.loadDomainUserByEmail(email);
		Document document = loadOwnedDocument(id, user.getId());

		if (document.getStatus() != DocumentStatus.RASCUNHO) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Documento já foi enviado");
		}

		document.setStatus(DocumentStatus.AGUARDANDO_ASSINATURA);
		return toResponse(documentRepository.save(document));
	}

	@Transactional
	public DocumentResponse moveDocument(Long id, String email, DocumentMoveRequest request) {
		User user = userDetailsService.loadDomainUserByEmail(email);
		Document document = loadOwnedDocument(id, user.getId());
		Group group = loadOwnedGroup(request.groupId(), user.getId());

		document.setGroup(group);
		return toResponse(documentRepository.save(document));
	}

	private Document loadOwnedDocument(Long id, Long userId) {
		return documentRepository.findByIdAndUserId(id, userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento não encontrado"));
	}

	private Group loadOwnedGroup(Long groupId, Long userId) {
		if (groupId == null) {
			return null;
		}

		return groupRepository.findByIdAndUserId(groupId, userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Grupo não encontrado"));
	}

	private DocumentStatus parseStatus(String status) {
		if (status == null || status.isBlank()) {
			return null;
		}

		try {
			return DocumentStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status do documento inválido");
		}
	}

	private String normalizeTitle(String title) {
		String normalizedTitle = title.trim();
		if (normalizedTitle.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Título do documento é obrigatório");
		}
		return normalizedTitle;
	}

	private String normalizeContent(String content) {
		if (content == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Conteúdo do documento é obrigatório");
		}
		return content;
	}

	private DocumentResponse toResponse(Document document) {
		Long groupId = document.getGroup() == null ? null : document.getGroup().getId();

		return new DocumentResponse(
			document.getId(),
			groupId,
			document.getTitle(),
			document.getContent(),
			document.getStatus(),
			document.getCreatedAt(),
			document.getUpdatedAt()
		);
	}
}
