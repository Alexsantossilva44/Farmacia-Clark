package br.com.farmacia.domain.cliente.entity;

import br.com.farmacia.domain.cliente.ClienteValidacao;
import br.com.farmacia.domain.cliente.valueobject.EnderecoVO;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade de domínio: Cliente da farmácia.
 *
 * @author Alex Silva e Claude
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = "endereco")
public class Cliente {

    private UUID          id;
    private String        nome;
    private String        cpf;
    private LocalDate     dataNascimento;
    private String        sexo;
    private String        telefone;
    private String        email;
    private EnderecoVO    endereco;
    private String        alergias;
    private String        observacoes;
    private LocalDateTime dataCadastro;

    @Builder.Default
    private Boolean ativo = true;

    public boolean isMaiorDeIdade() {
        return ClienteValidacao.temIdadeMinima(dataNascimento, 18);
    }

    public void atribuirId(UUID id) {
        if (this.id != null) {
            throw new IllegalStateException("Identidade já atribuída ao cliente");
        }
        if (id == null) {
            throw new IllegalArgumentException("Id não pode ser nulo");
        }
        this.id = id;
    }

    /**
     * Atualiza dados cadastrais (valores já validados pela camada de aplicação).
     */
    public void atualizar(
            String nome,
            LocalDate dataNascimento,
            String sexo,
            String telefone,
            String email,
            EnderecoVO endereco,
            String alergias,
            String observacoes,
            Boolean ativo) {

        if (nome != null) this.nome = nome;
        if (dataNascimento != null) this.dataNascimento = dataNascimento;
        if (sexo != null) this.sexo = sexo;
        if (telefone != null) this.telefone = telefone;
        if (email != null) this.email = email;
        if (endereco != null) this.endereco = endereco;
        if (alergias != null) this.alergias = alergias;
        if (observacoes != null) this.observacoes = observacoes;
        if (ativo != null) this.ativo = ativo;
    }
}
