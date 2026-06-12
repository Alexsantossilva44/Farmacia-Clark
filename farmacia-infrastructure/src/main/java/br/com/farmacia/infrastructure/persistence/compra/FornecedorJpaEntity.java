package br.com.farmacia.infrastructure.persistence.compra;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "fornecedores")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FornecedorJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "razao_social", nullable = false, length = 150)
    private String razaoSocial;

    @Column(name = "nome_fantasia", length = 150)
    private String nomeFantasia;

    @Column(name = "cnpj", nullable = false, length = 14, unique = true)
    private String cnpj;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
