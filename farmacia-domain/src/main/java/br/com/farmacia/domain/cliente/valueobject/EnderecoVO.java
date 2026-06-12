package br.com.farmacia.domain.cliente.valueobject;

import lombok.*;

/**
 * Value Object de Endereço.
 * Imutável — equals/hashCode por valor, não por referência.
 *
 * @author Alex Silva e Claude
 */
@Getter
@Builder
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class EnderecoVO {
    private String logradouro;
    private String numero;
    private String complemento;
    private String bairro;
    private String cidade;
    private String uf;
    private String cep;

    public String formatado() {
        return String.format("%s, %s%s — %s/%s — CEP: %s",
            logradouro, numero,
            complemento != null ? " " + complemento : "",
            cidade, uf, cep);
    }
}
