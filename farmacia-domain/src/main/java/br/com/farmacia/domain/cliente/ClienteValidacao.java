package br.com.farmacia.domain.cliente;

import br.com.farmacia.domain.cliente.exception.ClienteDadosInvalidosException;
import br.com.farmacia.domain.cliente.valueobject.EnderecoVO;

import java.time.LocalDate;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Regras de integridade dos dados cadastrais de cliente.
 *
 * <p>Alteração: {@link #NOME_MAX_LENGTH} passou de 150 para 100 caracteres
 * (migration V6, alinhado ao front e aos DTOs {@code ClienteInput}).</p>
 */
public final class ClienteValidacao {

    /** Limite da coluna {@code clientes.nome} — era 150 (V3), reduzido para 100 (V6). */
    public static final int NOME_MAX_LENGTH = 100;

    // D-07 corrigido: permite hífen simples entre partes do nome (ex: "Silva-Santos");
    // (?:-[\\p{L}]+)* rejeita múltiplos hífens consecutivos como "Jorge ---Macedo"
    private static final Pattern NOME_PESSOA = Pattern.compile(
        "^[\\p{L}]+(?:-[\\p{L}]+)*(?: [\\p{L}]+(?:-[\\p{L}]+)*)+$");

    // Alteração: regex alinhada ao front — TLD só letras (2-63); rejeita ex.: gmail.com.lixo...1.
    private static final Pattern EMAIL = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*\\.[a-zA-Z]{2,63}$");

    private static final LocalDate DATA_MINIMA = LocalDate.of(1900, 1, 1);

    private ClienteValidacao() {
    }

    public static void validarNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new ClienteDadosInvalidosException("Nome completo é obrigatório.");
        }
        String trimmed = nome.trim();
        String normalizado = normalizarNome(trimmed);
        if (!trimmed.equals(normalizado)) {
            throw new ClienteDadosInvalidosException(
                "Nome deve conter apenas letras e um espaço entre nome e sobrenome.");
        }
        if (!NOME_PESSOA.matcher(normalizado).matches()) {
            throw new ClienteDadosInvalidosException(
                "Nome deve conter apenas letras e um espaço entre nome e sobrenome.");
        }
        // Valida tamanho após normalização — mesma regra do @Size(max=100) na API.
        if (normalizado.length() > NOME_MAX_LENGTH) {
            throw new ClienteDadosInvalidosException(
                "Nome completo deve ter no máximo " + NOME_MAX_LENGTH + " caracteres.");
        }
    }

    public static void validarCpf(String cpf) {
        if (cpf == null || cpf.isBlank()) {
            throw new ClienteDadosInvalidosException("CPF é obrigatório.");
        }
        String digits = cpf.replaceAll("\\D", "");
        if (digits.length() != 11) {
            throw new ClienteDadosInvalidosException("CPF deve conter 11 dígitos.");
        }
        if (digits.chars().distinct().count() == 1) {
            throw new ClienteDadosInvalidosException("CPF inválido.");
        }
        int d1 = calcularDigitoCpf(digits, 9);
        int d2 = calcularDigitoCpf(digits, 10);
        if (d1 != Character.getNumericValue(digits.charAt(9))
            || d2 != Character.getNumericValue(digits.charAt(10))) {
            throw new ClienteDadosInvalidosException("CPF inválido.");
        }
    }

    public static void validarDataNascimento(LocalDate dataNascimento) {
        if (dataNascimento == null) {
            return;
        }
        LocalDate hoje = LocalDate.now();
        if (dataNascimento.isAfter(hoje)) {
            throw new ClienteDadosInvalidosException("Data de nascimento não pode ser futura.");
        }
        if (dataNascimento.isBefore(DATA_MINIMA)) {
            throw new ClienteDadosInvalidosException("Data de nascimento inválida.");
        }
        if (!temIdadeMinima(dataNascimento, 18)) {
            throw new ClienteDadosInvalidosException(
                "Cliente deve ter 18 anos ou mais para cadastro.");
        }
    }

    public static void validarDataNascimentoObrigatoria(LocalDate dataNascimento) {
        if (dataNascimento == null) {
            throw new ClienteDadosInvalidosException("Data de nascimento é obrigatória.");
        }
        validarDataNascimento(dataNascimento);
    }

    /** True se completou {@code idadeMin} anos (inclusive no dia do aniversário). */
    public static boolean temIdadeMinima(LocalDate dataNascimento, int idadeMin) {
        if (dataNascimento == null) {
            return false;
        }
        return !dataNascimento.plusYears(idadeMin).isAfter(LocalDate.now());
    }

    public static void validarTelefone(String telefone) {
        if (telefone == null || telefone.isBlank()) {
            return;
        }
        String digits = normalizarTelefone(telefone);
        if (digits.length() < 10 || digits.length() > 11) {
            throw new ClienteDadosInvalidosException("Telefone deve conter 10 ou 11 dígitos.");
        }
    }

    public static void validarTelefoneObrigatorio(String telefone) {
        if (telefone == null || telefone.isBlank()) {
            throw new ClienteDadosInvalidosException("Telefone é obrigatório.");
        }
        validarTelefone(telefone);
    }

    public static void validarEmail(String email) {
        if (email == null || email.isBlank()) {
            return;
        }
        if (email.chars().anyMatch(Character::isWhitespace)) {
            throw new ClienteDadosInvalidosException("E-mail não pode conter espaços.");
        }
        String normalizado = normalizarEmail(email);
        // Alteração: mesmas regras do front — formato universal com TLD alfabético.
        if (!emailFormatoUniversalValido(normalizado)) {
            throw new ClienteDadosInvalidosException("E-mail inválido.");
        }
    }

    /** RFC 5321 simplificado: rótulos de domínio válidos e TLD apenas letras (2–63). */
    private static boolean emailFormatoUniversalValido(String email) {
        if (email.length() > 254) {
            return false;
        }
        if (!EMAIL.matcher(email).matches()) {
            return false;
        }
        int at = email.indexOf('@');
        if (at < 1) {
            return false;
        }
        String local = email.substring(0, at);
        String domain = email.substring(at + 1);
        // Pontos consecutivos ou nas extremidades invalidam o endereço.
        if (local.startsWith(".") || local.endsWith(".") || local.contains("..")) {
            return false;
        }
        return !domain.startsWith(".") && !domain.endsWith(".") && !domain.contains("..");
    }

    public static void validarEmailObrigatorio(String email) {
        if (email == null || email.isBlank()) {
            throw new ClienteDadosInvalidosException("E-mail é obrigatório.");
        }
        validarEmail(email);
    }

    public static void validarSexo(String sexo) {
        if (sexo == null || sexo.isBlank()) {
            throw new ClienteDadosInvalidosException("Sexo é obrigatório.");
        }
    }

    public static void validarEnderecoObrigatorio(EnderecoVO endereco) {
        if (endereco == null || endereco.getLogradouro() == null || endereco.getLogradouro().isBlank()) {
            throw new ClienteDadosInvalidosException("Logradouro é obrigatório.");
        }
        String numero = endereco.getNumero();
        if (numero != null && !numero.isBlank()) {
            if (!numero.matches("[a-zA-Z0-9]{1,8}")) {
                throw new ClienteDadosInvalidosException(
                    "Apenas letras e dígitos, máximo 8 caracteres.");
            }
            if (!numero.matches(".*\\d.*")) {
                throw new ClienteDadosInvalidosException(
                    "Conter pelo menos um dígito (ex: 123, 45A).");
            }
        }
        if (endereco.getBairro() == null || endereco.getBairro().isBlank()) {
            throw new ClienteDadosInvalidosException("Bairro é obrigatório.");
        }
        String cep = endereco.getCep() == null ? "" : endereco.getCep().replaceAll("\\D", "");
        if (cep.isBlank()) {
            throw new ClienteDadosInvalidosException("CEP é obrigatório.");
        }
        if (cep.length() != 8) {
            throw new ClienteDadosInvalidosException("CEP deve conter 8 dígitos.");
        }
        if (endereco.getUf() == null || endereco.getUf().isBlank()) {
            throw new ClienteDadosInvalidosException("UF é obrigatória.");
        }
        if (endereco.getCidade() == null || endereco.getCidade().isBlank()) {
            throw new ClienteDadosInvalidosException("Cidade é obrigatória.");
        }
    }

    public static String normalizarTelefone(String telefone) {
        if (telefone == null) {
            return "";
        }
        return telefone.replaceAll("\\D", "");
    }

    public static String normalizarEmail(String email) {
        if (email == null) {
            return "";
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public static String normalizarNome(String nome) {
        if (nome == null) {
            return null;
        }
        return nome.trim()
            .replaceAll("[^\\p{L}\\s-]", "") // D-07: mantém hífen para nomes compostos como "Silva-Santos"
            .replaceAll("\\s+", " ");
    }

    private static int calcularDigitoCpf(String digits, int length) {
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += Character.getNumericValue(digits.charAt(i)) * (length + 1 - i);
        }
        int rest = sum % 11;
        return rest < 2 ? 0 : 11 - rest;
    }
}
