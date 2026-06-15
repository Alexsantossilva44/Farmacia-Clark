import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Search, UserPlus, RotateCcw, User, MapPin, Phone } from 'lucide-react'
import { useEffect, useRef, useState, type RefObject } from 'react'
import {
  ApiError,
  atualizarCliente,
  cadastrarCliente,
  fetchClientePorCpf,
  verificarContatoCliente,
} from '@/lib/api'
import { traduzirErroApi } from '@/lib/erros'
import { formatCpfDisplay, onlyDigits, UFS_BR } from '@/lib/cadastro-options'
import { resolverUfPorMunicipio, validarCidadeObrigatoria, validarUfEndereco, municipioPertenceAUf } from '@/lib/cidades-uf-br'
import {
  dataBrParaIso,
  dataIsoParaBr,
  sanitizeNomePessoa,
  validarCpf,
  validarDataNascimentoBr,
  validarEmail,
  validarNomePessoa,
  validarTelefone,
  validarLogradouro,
  validarBairro,
  validarCep,
  validarSexo,
  normalizarEmail,
  sanitizeEmailInput,
  formatTelefoneDisplay,
} from '@/lib/validacao-cliente'
import { focarPrimeiroErro } from '@/lib/validacao-formulario'

/**
 * Cadastro e edição de clientes (aba Clientes em /cadastros).
 *
 * Evoluções recentes no front:
 * - Endereço: UF e Cidade obrigatórios (validarUfEndereco / validarCidadeObrigatoria).
 * - Submit: validarFormulario marca todos os erros e focarPrimeiroErro leva ao primeiro campo.
 * - CPF inválido ou duplicado: exibe erro, aguarda 3s e limpa o campo para nova digitação.
 * - Observações: placeholder orienta registro de alergias (campo continua opcional).
 * - Nome completo: maxLength 100 + sanitizeNomePessoa (antes 150 só no banco/API).
 */
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { Select } from '@/components/ui/Select'
import { CidadePorUfSelect } from '@/components/ui/CidadePorUfSelect'
import { DataNascimentoInput } from '@/components/ui/DataNascimentoInput'
import { Card } from '@/components/ui/Card'
import { Badge } from '@/components/ui/Badge'
import type { ClienteAtualizacaoInput, ClienteInput } from '@/types/cadastro'
import type { Cliente, Endereco } from '@/types/cliente'

const MSG_TELEFONE_DUPLICADO = 'Telefone já cadastrado em outro cliente.'
const MSG_EMAIL_DUPLICADO = 'E-mail já cadastrado em outro cliente.'
const MSG_CPF_DUPLICADO = 'CPF já cadastrado. Use Buscar por CPF para editar.'
const CPF_RESET_DELAY_MS = 3000
const REQUIRED_MSG_DELAY_MS = 2000

type CampoClienteForm =
  | 'nome'
  | 'cpf'
  | 'dataNascimento'
  | 'sexo'
  | 'telefone'
  | 'email'
  | 'logradouro'
  | 'numero'
  | 'bairro'
  | 'cep'
  | 'uf'
  | 'cidade'

/** Valores não padronizados impedem o autofill do Chrome ("Gerenciar informações pessoais"). */
const AC = {
  nome: 'farmacia-cliente-nome',
  cpf: 'farmacia-cliente-cpf',
  data: 'farmacia-cliente-data',
  telefone: 'farmacia-cliente-telefone',
  email: 'farmacia-cliente-email',
  endereco: 'farmacia-cliente-endereco',
  clinico: 'farmacia-cliente-clinico',
} as const

const inputCompact = 'py-2.5 text-sm'

const textareaClass =
  'w-full px-3 py-2.5 rounded-xl glass bg-white/[0.03] border border-white/10 text-sm text-white placeholder:text-white/30 focus:outline-none focus:ring-2 focus:ring-mint/40 resize-none'

const SEXO_OPCOES = [
  { value: 'M', label: 'Masculino' },
  { value: 'F', label: 'Feminino' },
  { value: 'O', label: 'Outro' },
]

const emptyEndereco = (): Endereco => ({
  logradouro: '',
  numero: '',
  complemento: '',
  bairro: '',
  cidade: '',
  uf: '',
  cep: '',
})

const emptyCliente = (): ClienteInput => ({
  nome: '',
  cpf: '',
  telefone: '',
  email: '',
  sexo: '',
  dataNascimento: '',
  endereco: emptyEndereco(),
  alergias: '',
  observacoes: '',
})

function formatCepDisplay(cep: string): string {
  const d = onlyDigits(cep)
  if (d.length <= 5) return d
  return `${d.slice(0, 5)}-${d.slice(5, 8)}`
}

function buildEnderecoPayload(endereco?: Endereco): Endereco | undefined {
  if (!endereco) return undefined
  const trimmed = {
    logradouro: endereco.logradouro?.trim() || undefined,
    numero: endereco.numero?.trim() || undefined,
    complemento: endereco.complemento?.trim() || undefined,
    bairro: endereco.bairro?.trim() || undefined,
    cidade: endereco.cidade?.trim() || undefined,
    uf: endereco.uf?.trim() || undefined,
    cep: onlyDigits(endereco.cep ?? '') || undefined,
  }
  const hasAny = Object.values(trimmed).some(Boolean)
  return hasAny ? trimmed : undefined
}

function buildClientePayload(form: ClienteInput): ClienteInput {
  const dataIso = form.dataNascimento?.includes('/')
    ? dataBrParaIso(form.dataNascimento)
    : form.dataNascimento

  return {
    ...form,
    nome: sanitizeNomePessoa(form.nome).trim(),
    cpf: onlyDigits(form.cpf),
    telefone: form.telefone?.trim() ? onlyDigits(form.telefone) : undefined,
    email: form.email?.trim() ? normalizarEmail(form.email) : undefined,
    sexo: form.sexo || undefined,
    dataNascimento: dataIso || undefined,
    alergias: form.alergias?.trim() || undefined,
    observacoes: form.observacoes?.trim() || undefined,
    endereco: buildEnderecoPayload(form.endereco),
  }
}

function clienteToForm(data: Cliente): ClienteInput {
  return {
    nome: data.nome,
    cpf: data.cpf,
    telefone: data.telefone ?? '',
    email: sanitizeEmailInput(data.email ?? ''),
    dataNascimento: data.dataNascimento ? dataIsoParaBr(data.dataNascimento) : '',
    sexo: data.sexo ?? '',
    alergias: data.alergias ?? '',
    observacoes: data.observacoes ?? '',
    endereco: {
      ...emptyEndereco(),
      ...data.endereco,
      uf:
        data.endereco?.uf
        || resolverUfPorMunicipio(data.endereco?.cidade ?? '')
        || '',
    },
  }
}

function patchEndereco(form: ClienteInput, patch: Partial<Endereco>): ClienteInput {
  return {
    ...form,
    endereco: { ...emptyEndereco(), ...form.endereco, ...patch },
  }
}

function sexoLabel(sexo?: string): string {
  return SEXO_OPCOES.find((o) => o.value === sexo)?.label ?? '—'
}

function temDadosDeCliente(form: ClienteInput): boolean {
  return Boolean(
    form.telefone?.trim()
    || form.email?.trim()
    || form.endereco?.cidade?.trim()
    || form.endereco?.bairro?.trim()
    || form.endereco?.logradouro?.trim()
    || form.alergias?.trim()
    || form.observacoes?.trim(),
  )
}

function formularioNovoComCpf(cpf: string, manter?: Partial<ClienteInput>): ClienteInput {
  return {
    ...emptyCliente(),
    cpf,
    nome: manter?.nome ?? '',
    dataNascimento: manter?.dataNascimento ?? '',
    sexo: manter?.sexo ?? '',
  }
}

export function ClientesCadastroTab() {
  const qc = useQueryClient()
  const [cpfBusca, setCpfBusca] = useState('')
  const [clienteId, setClienteId] = useState<string | null>(null)
  const [form, setForm] = useState<ClienteInput>(emptyCliente())
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [fieldErrors, setFieldErrors] = useState<{
    nome?: string
    cpf?: string
    dataNascimento?: string
    telefone?: string
    email?: string
    sexo?: string
    logradouro?: string
    numero?: string
    bairro?: string
    cep?: string
    uf?: string
    cidade?: string
  }>({})
  const [camposTocados, setCamposTocados] = useState<Partial<Record<CampoClienteForm, boolean>>>({})
  const [cpfEmUso, setCpfEmUso] = useState(false)
  const [cpfTravandoFoco, setCpfTravandoFoco] = useState(false)
  const [dataNascimentoTravandoFoco, setDataNascimentoTravandoFoco] = useState(false)
  const [numeroTravandoFoco, setNumeroTravandoFoco] = useState(false)
  const [telefoneTravandoFoco, setTelefoneTravandoFoco] = useState(false)
  const [telefoneEmUso, setTelefoneEmUso] = useState(false)
  const [emailEmUso, setEmailEmUso] = useState(false)
  const [verificandoCpf, setVerificandoCpf] = useState(false)
  const [verificandoContato, setVerificandoContato] = useState(false)
  const [cpfClienteCarregado, setCpfClienteCarregado] = useState<string | null>(null)
  const [formKey, setFormKey] = useState(0)
  const [bloquearAutofillInicial, setBloquearAutofillInicial] = useState(true)
  const telefoneInputRef = useRef<HTMLInputElement>(null)
  const emailInputRef = useRef<HTMLInputElement>(null)
  const cpfInputRef = useRef<HTMLInputElement>(null)
  const dataNascimentoInputRef = useRef<HTMLInputElement>(null)
  const numeroInputRef = useRef<HTMLInputElement>(null)
  const camposTocadosRef = useRef<Partial<Record<CampoClienteForm, boolean>>>({})
  const cpfResetTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const emailResetTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const dataNascimentoResetTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const numeroResetTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const telefoneResetTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const sexoResetTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const nomeResetTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const logradouroResetTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const bairroResetTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const cepResetTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const ufResetTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const cpfValidacaoRef = useRef(0)

  const isEdicao = clienteId !== null

  useEffect(() => {
    camposTocadosRef.current = camposTocados
  }, [camposTocados])

  useEffect(() => () => {
    if (cpfResetTimerRef.current) clearTimeout(cpfResetTimerRef.current)
    if (emailResetTimerRef.current) clearTimeout(emailResetTimerRef.current)
    if (dataNascimentoResetTimerRef.current) clearTimeout(dataNascimentoResetTimerRef.current)
    if (numeroResetTimerRef.current) clearTimeout(numeroResetTimerRef.current)
    if (telefoneResetTimerRef.current) clearTimeout(telefoneResetTimerRef.current)
    if (sexoResetTimerRef.current) clearTimeout(sexoResetTimerRef.current)
    if (nomeResetTimerRef.current) clearTimeout(nomeResetTimerRef.current)
    if (logradouroResetTimerRef.current) clearTimeout(logradouroResetTimerRef.current)
    if (bairroResetTimerRef.current) clearTimeout(bairroResetTimerRef.current)
    if (cepResetTimerRef.current) clearTimeout(cepResetTimerRef.current)
    if (ufResetTimerRef.current) clearTimeout(ufResetTimerRef.current)
  }, [])

  useEffect(() => {
    if (isEdicao) setBloquearAutofillInicial(false)
  }, [isEdicao])

  useEffect(() => {
    const email = form.email ?? ''
    const limpo = sanitizeEmailInput(email)
    if (email !== limpo) {
      setForm((prev) => ({ ...prev, email: limpo }))
    }
  }, [form.email])

  const readOnlyAntiAutofill = bloquearAutofillInicial && !isEdicao

  function liberarCamposDoFormulario() {
    setBloquearAutofillInicial(false)
  }

  function marcarCampoTocado(campo: CampoClienteForm) {
    setCamposTocados((prev) => ({ ...prev, [campo]: true }))
  }

  function marcarTodosCamposTocados() {
    const todos: CampoClienteForm[] = [
      'nome',
      'cpf',
      'dataNascimento',
      'sexo',
      'telefone',
      'email',
      'logradouro',
      'numero',
      'bairro',
      'cep',
      'uf',
      'cidade',
    ]
    setCamposTocados(Object.fromEntries(todos.map((c) => [c, true])))
  }

  function deveValidarCampo(campo: CampoClienteForm): boolean {
    return Boolean(camposTocados[campo])
  }

  function limparErrosCamposAdjacentesAoCpf() {
    const tocados = camposTocadosRef.current
    setFieldErrors((prev) => {
      const next = { ...prev }
      if (!tocados.dataNascimento) next.dataNascimento = undefined
      if (!tocados.sexo) next.sexo = undefined
      return next
    })
  }

  function erroCampo(campo: CampoClienteForm): string | undefined {
    if (!deveValidarCampo(campo)) return undefined
    return fieldErrors[campo]
  }

  function resetValidacaoFormulario() {
    cancelarLimpezaCpfAgendada()
    cancelarLimpezaEmailAgendada()
    cancelarLimpezaDataNascimentoAgendada()
    cancelarLimpezaNumeroAgendada()
    cancelarLimpezaTelefoneAgendada()
    setCamposTocados({})
    setFieldErrors({})
  }

  function cancelarLimpezaCpfAgendada() {
    if (cpfResetTimerRef.current) {
      clearTimeout(cpfResetTimerRef.current)
      cpfResetTimerRef.current = null
    }
    setCpfTravandoFoco(false)
  }

  function cancelarLimpezaNumeroAgendada() {
    if (numeroResetTimerRef.current) {
      clearTimeout(numeroResetTimerRef.current)
      numeroResetTimerRef.current = null
    }
    setNumeroTravandoFoco(false)
  }

  function limparNumeroParaNovaEntrada() {
    setForm((prev) => ({ ...prev, endereco: { ...prev.endereco!, numero: '' } }))
    setFieldErrors((prev) => ({ ...prev, numero: undefined }))
    setCamposTocados((prev) => {
      const { numero: _n, ...rest } = prev
      return rest
    })
    setNumeroTravandoFoco(true)
    focarCampo(numeroInputRef)
  }

  function agendarLimpezaNumeroAposErro() {
    cancelarLimpezaNumeroAgendada()
    setNumeroTravandoFoco(true)
    focarCampo(numeroInputRef)
    numeroResetTimerRef.current = setTimeout(() => {
      numeroResetTimerRef.current = null
      limparNumeroParaNovaEntrada()
    }, CPF_RESET_DELAY_MS)
  }

  function cancelarLimpezaDataNascimentoAgendada() {
    if (dataNascimentoResetTimerRef.current) {
      clearTimeout(dataNascimentoResetTimerRef.current)
      dataNascimentoResetTimerRef.current = null
    }
    setDataNascimentoTravandoFoco(false)
  }

  function limparDataNascimentoParaNovaEntrada() {
    setDataNascimentoTravandoFoco(false)
    setForm((prev) => ({ ...prev, dataNascimento: '' }))
    setFieldErrors((prev) => ({ ...prev, dataNascimento: undefined }))
    setCamposTocados((prev) => {
      const { dataNascimento: _d, ...rest } = prev
      return rest
    })
    focarCampo(dataNascimentoInputRef)
  }

  function agendarLimpezaDataNascimentoAposErro(travarFoco = true) {
    cancelarLimpezaDataNascimentoAgendada()
    if (travarFoco) {
      setDataNascimentoTravandoFoco(true)
      focarCampo(dataNascimentoInputRef)
    }
    dataNascimentoResetTimerRef.current = setTimeout(() => {
      dataNascimentoResetTimerRef.current = null
      limparDataNascimentoParaNovaEntrada()
    }, CPF_RESET_DELAY_MS)
  }

  function cancelarLimpezaEmailAgendada() {
    if (emailResetTimerRef.current) {
      clearTimeout(emailResetTimerRef.current)
      emailResetTimerRef.current = null
    }
  }

  function limparEmailParaNovaEntrada() {
    setEmailEmUso(false)
    setForm((prev) => ({ ...prev, email: '' }))
    setFieldErrors((prev) => ({ ...prev, email: undefined }))
    setCamposTocados((prev) => {
      const { email: _email, ...rest } = prev
      return rest
    })
    focarCampo(emailInputRef)
  }

  function agendarLimpezaEmailAposErro() {
    cancelarLimpezaEmailAgendada()
    emailResetTimerRef.current = setTimeout(() => {
      emailResetTimerRef.current = null
      limparEmailParaNovaEntrada()
    }, CPF_RESET_DELAY_MS)
  }

  function cancelarLimpezaTelefoneAgendada() {
    if (telefoneResetTimerRef.current) {
      clearTimeout(telefoneResetTimerRef.current)
      telefoneResetTimerRef.current = null
    }
    setTelefoneTravandoFoco(false)
  }

  function limparTelefoneParaNovaEntrada() {
    setTelefoneEmUso(false)
    setTelefoneTravandoFoco(false)
    setForm((prev) => ({ ...prev, telefone: '' }))
    setFieldErrors((prev) => ({ ...prev, telefone: undefined }))
    setCamposTocados((prev) => {
      const { telefone: _t, ...rest } = prev
      return rest
    })
    focarCampo(telefoneInputRef)
  }

  function agendarLimpezaTelefoneAposErro(travarFoco = true) {
    cancelarLimpezaTelefoneAgendada()
    if (travarFoco) {
      setTelefoneTravandoFoco(true)
      focarCampo(telefoneInputRef)
    }
    telefoneResetTimerRef.current = setTimeout(() => {
      telefoneResetTimerRef.current = null
      limparTelefoneParaNovaEntrada()
    }, CPF_RESET_DELAY_MS)
  }

  function limparCpfParaNovaEntrada() {
    setCpfEmUso(false)
    setCpfTravandoFoco(false)
    setForm((prev) => ({ ...prev, cpf: '' }))
    setFieldErrors((prev) => ({ ...prev, cpf: undefined }))
    setCamposTocados((prev) => {
      const { cpf: _cpf, ...rest } = prev
      return rest
    })
    limparErrosCamposAdjacentesAoCpf()
    focarCampo(cpfInputRef)
  }

  function agendarLimpezaCpfAposErro(travarFoco = true) {
    cancelarLimpezaCpfAgendada()
    if (travarFoco) {
      setCpfTravandoFoco(true)
      focarCampo(cpfInputRef)
    }
    cpfResetTimerRef.current = setTimeout(() => {
      cpfResetTimerRef.current = null
      limparCpfParaNovaEntrada()
    }, CPF_RESET_DELAY_MS)
  }

  async function confirmarCpfCadastro(cpf: string): Promise<void> {
    if (isEdicao) return
    const digits = onlyDigits(cpf)
    if (digits.length !== 11) return

    const validacaoId = ++cpfValidacaoRef.current
    marcarCampoTocado('cpf')
    const cpfErr = validarCpf(cpf)
    if (cpfErr) {
      setFieldErrors((prev) => ({ ...prev, cpf: cpfErr }))
      limparErrosCamposAdjacentesAoCpf()
      agendarLimpezaCpfAposErro(false)
      return
    }

    const duplicado = await verificarCpfDisponivel(cpf)
    if (validacaoId !== cpfValidacaoRef.current) return
    if (duplicado) agendarLimpezaCpfAposErro()
  }

  async function verificarContatoDisponivel(
    telefone?: string,
    email?: string,
  ): Promise<{ telefoneEmUso: boolean; emailEmUso: boolean }> {
    const telefoneErr = telefone ? validarTelefone(telefone) : null
    const emailErr = email ? validarEmail(email) : null
    const telefoneInformado = telefone?.trim() && !telefoneErr
    const emailInformado = email?.trim() && !emailErr

    if (!telefoneInformado && !emailInformado) {
      setTelefoneEmUso(false)
      setEmailEmUso(false)
      return { telefoneEmUso: false, emailEmUso: false }
    }

    setVerificandoContato(true)
    try {
      const resultado = await verificarContatoCliente({
        telefone: telefoneInformado ? telefone : undefined,
        email: emailInformado ? email : undefined,
        excluirClienteId: clienteId ?? undefined,
      })

      const telOcupado = telefoneInformado ? !resultado.telefoneDisponivel : false
      const mailOcupado = emailInformado ? !resultado.emailDisponivel : false

      setTelefoneEmUso(telOcupado)
      setEmailEmUso(mailOcupado)
      if (telOcupado) marcarCampoTocado('telefone')
      if (mailOcupado) marcarCampoTocado('email')
      setFieldErrors((prev) => ({
        ...prev,
        telefone: telOcupado
          ? MSG_TELEFONE_DUPLICADO
          : prev.telefone === MSG_TELEFONE_DUPLICADO
            ? undefined
            : prev.telefone,
        email: mailOcupado
          ? MSG_EMAIL_DUPLICADO
          : prev.email === MSG_EMAIL_DUPLICADO
            ? undefined
            : prev.email,
      }))

      return { telefoneEmUso: telOcupado, emailEmUso: mailOcupado }
    } catch (err) {
      setError(traduzirErroApi(err))
      return { telefoneEmUso: true, emailEmUso: true }
    } finally {
      setVerificandoContato(false)
    }
  }

  function avancarCampo(atual: HTMLElement, form: HTMLElement) {
    const focusable = Array.from(
      form.querySelectorAll<HTMLElement>(
        'input:not([disabled]), button:not([disabled]), select:not([disabled])'
      )
    ).filter((el) => el.tabIndex !== -1)
    const idx = focusable.indexOf(atual)
    if (idx !== -1 && idx + 1 < focusable.length) focusable[idx + 1].focus()
  }

  function focarCampo(ref: RefObject<HTMLInputElement | null>) {
    requestAnimationFrame(() => {
      ref.current?.focus()
      ref.current?.select()
    })
  }

  function focoDirecionado(): boolean {
    return cpfTravandoFoco || dataNascimentoTravandoFoco || numeroTravandoFoco || telefoneTravandoFoco
  }

  function formularioPronto(): boolean {
    const e = form.endereco
    return (
      form.nome.trim().length > 0
      && onlyDigits(form.cpf).length === 11
      && (form.dataNascimento ?? '').trim().length > 0
      && (form.sexo ?? '').trim().length > 0
      && onlyDigits(form.telefone ?? '').length >= 10
      && (form.email ?? '').trim().length > 0
      && (e?.logradouro ?? '').trim().length > 0
      && (e?.bairro ?? '').trim().length > 0
      && onlyDigits(e?.cep ?? '').length === 8
      && (e?.uf ?? '').trim().length > 0
      && (e?.cidade ?? '').trim().length > 0
    )
  }

  function cpfBloqueado(): boolean {
    return cpfEmUso || fieldErrors.cpf === MSG_CPF_DUPLICADO
  }

  /** Data e sexo (após CPF) só liberados quando CPF estiver válido e não duplicado. */
  function cpfImpedeProximosCampos(): boolean {
    if (isEdicao) return false
    if (verificandoCpf || cpfBloqueado()) return true
    const digits = onlyDigits(form.cpf)
    if (digits.length > 0 && camposTocados.cpf && Boolean(fieldErrors.cpf)) return true
    if (digits.length === 11 && validarCpf(form.cpf)) return true
    return false
  }

  async function confirmarTelefone(refocar = false): Promise<boolean> {
    const telefoneErr = validarTelefone(form.telefone ?? '', true)
    if (telefoneErr) {
      marcarCampoTocado('telefone')
      setFieldErrors((prev) => ({ ...prev, telefone: telefoneErr }))
      agendarLimpezaTelefoneAposErro(false)
      return false
    }
    const contato = await verificarContatoDisponivel(form.telefone, undefined)
    if (contato.telefoneEmUso) {
      agendarLimpezaTelefoneAposErro(false)
      return false
    }
    setFieldErrors((prev) => ({ ...prev, telefone: undefined }))
    return true
  }

  async function confirmarEmail(refocar = false): Promise<boolean> {
    const email = sanitizeEmailInput(form.email ?? '')
    setForm((prev) => (prev.email === email ? prev : { ...prev, email }))
    const emailErr = validarEmail(email, true)
    if (emailErr) {
      marcarCampoTocado('email')
      setFieldErrors((prev) => ({ ...prev, email: emailErr }))
      focarCampo(emailInputRef)
      agendarLimpezaEmailAposErro()
      return false
    }
    const contato = await verificarContatoDisponivel(form.telefone, email)
    if (contato.emailEmUso) {
      agendarLimpezaEmailAposErro()
      return false
    }
    setFieldErrors((prev) => ({ ...prev, email: undefined }))
    return true
  }

  async function verificarCpfDisponivel(cpf: string): Promise<boolean> {
    const digits = onlyDigits(cpf)
    if (isEdicao || digits.length !== 11 || validarCpf(cpf)) {
      setCpfEmUso(false)
      return false
    }

    setVerificandoCpf(true)
    try {
      await fetchClientePorCpf(digits)
      setCpfEmUso(true)
      marcarCampoTocado('cpf')
      setFieldErrors((prev) => ({
        ...prev,
        cpf: MSG_CPF_DUPLICADO,
      }))
      limparErrosCamposAdjacentesAoCpf()
      focarCampo(cpfInputRef)
      return true
    } catch (err) {
      if (err instanceof ApiError && err.status === 404) {
        setCpfEmUso(false)
        setFieldErrors((prev) => {
          if (prev.cpf?.includes('já cadastrado')) {
            return { ...prev, cpf: undefined }
          }
          return prev
        })
        return false
      }
      return false
    } finally {
      setVerificandoCpf(false)
    }
  }

  function erroDataNascimento(): string | null {
    if (!form.dataNascimento?.trim()) {
      return 'Lembre-se: Campo Obrigatório.'
    }
    return validarDataNascimentoBr(form.dataNascimento)
  }

  /** Logradouro, bairro, CEP, UF e cidade — todos obrigatórios no cadastro completo. */
  function errosEnderecoObrigatorio(endereco = form.endereco) {
    const uf = endereco?.uf ?? ''
    const cidade = endereco?.cidade ?? ''
    return {
      logradouro: validarLogradouro(endereco?.logradouro ?? '') ?? undefined,
      bairro: validarBairro(endereco?.bairro ?? '') ?? undefined,
      cep: validarCep(endereco?.cep ?? '') ?? undefined,
      uf: validarUfEndereco(uf) ?? undefined,
      cidade: validarCidadeObrigatoria(cidade, uf) ?? undefined,
    }
  }

  function validarFormulario(): boolean {
    marcarTodosCamposTocados()
    const nomeErr = validarNomePessoa(form.nome)
    const cpfErr = isEdicao ? null : validarCpf(form.cpf)
    const dataErr = erroDataNascimento()
    const sexoErr = validarSexo(form.sexo ?? '')
    const telefoneErr = telefoneEmUso
      ? MSG_TELEFONE_DUPLICADO
      : validarTelefone(form.telefone ?? '', true)
    const emailErr = emailEmUso
      ? MSG_EMAIL_DUPLICADO
      : validarEmail(form.email ?? '', true)
    const enderecoErr = errosEnderecoObrigatorio()

    setFieldErrors({
      nome: nomeErr ?? undefined,
      cpf: cpfEmUso ? MSG_CPF_DUPLICADO : (cpfErr ?? undefined),
      dataNascimento: dataErr ?? undefined,
      sexo: sexoErr ?? undefined,
      telefone: telefoneErr ?? undefined,
      email: emailErr ?? undefined,
      ...enderecoErr,
    })

    const valido =
      !nomeErr
      && !cpfErr
      && !dataErr
      && !sexoErr
      && !telefoneErr
      && !emailErr
      && !enderecoErr.logradouro
      && !enderecoErr.bairro
      && !enderecoErr.cep
      && !enderecoErr.uf
      && !enderecoErr.cidade
      && !cpfEmUso
      && !telefoneEmUso
      && !emailEmUso

    // Centralizado em validacao-formulario.ts (antes: querySelector manual no form).
    if (!valido) {
      if (cpfEmUso) {
        focarCampo(cpfInputRef)
      } else {
        focarPrimeiroErro(document.querySelector('form.form-sem-autofill'))
      }
    }

    return valido
  }

  async function tentarCadastro() {
    setError('')
    if (!validarFormulario()) return

    const cpfJaCadastrado = await verificarCpfDisponivel(form.cpf)
    if (cpfJaCadastrado) return

    const contato = await verificarContatoDisponivel(form.telefone, form.email)
    if (contato.telefoneEmUso || contato.emailEmUso) return

    cadastroMutation.mutate()
  }

  async function tentarAtualizar() {
    setError('')
    if (!validarFormulario()) return

    const contato = await verificarContatoDisponivel(form.telefone, form.email)
    if (contato.telefoneEmUso || contato.emailEmUso) return

    updateMutation.mutate()
  }

  const buscarMutation = useMutation({
    mutationFn: () => fetchClientePorCpf(cpfBusca),
    onSuccess: (data) => {
      setClienteId(data.id)
      setCpfClienteCarregado(data.cpf)
      setForm(clienteToForm(data))
      setCpfEmUso(false)
      setTelefoneEmUso(false)
      setEmailEmUso(false)
      resetValidacaoFormulario()
      setBloquearAutofillInicial(false)
      setSuccess(`Cliente "${data.nome}" carregado.`)
      setError('')
    },
    onError: (err: unknown) => {
      setClienteId(null)
      setCpfClienteCarregado(null)
      if (err instanceof ApiError && err.status === 404) {
        const cpf = onlyDigits(cpfBusca)
        setForm(formularioNovoComCpf(cpf))
        resetValidacaoFormulario()
        setCpfEmUso(false)
        setTelefoneEmUso(false)
        setEmailEmUso(false)
        setError('')
        setSuccess('CPF não cadastrado. Preencha os dados para novo cliente.')
      } else {
        setError(traduzirErroApi(err))
        setSuccess('')
      }
    },
  })

  const cadastroMutation = useMutation({
    mutationFn: () => cadastrarCliente(buildClientePayload(form)),
    onSuccess: (data) => {
      setClienteId(data.id)
      setCpfClienteCarregado(data.cpf)
      setForm(clienteToForm(data))
      setCpfBusca(formatCpfDisplay(data.cpf))
      setCpfEmUso(false)
      setTelefoneEmUso(false)
      setEmailEmUso(false)
      resetValidacaoFormulario()
      setSuccess('Cliente cadastrado com sucesso.')
      setError('')
      qc.invalidateQueries({ queryKey: ['clientes'] })
    },
    onError: (err: unknown) => {
      setSuccess('')
      if (err instanceof ApiError && err.status === 409) {
        const tipo = (err.problem as { type?: string } | undefined)?.type ?? ''
        const detail = (err.problem?.detail ?? err.message).toLowerCase()
        if (tipo.includes('cpf-duplicado') || detail.includes('cpf')) {
          setCpfEmUso(true)
          marcarCampoTocado('cpf')
          setFieldErrors((prev) => ({
            ...prev,
            cpf: MSG_CPF_DUPLICADO,
          }))
          focarCampo(cpfInputRef)
          agendarLimpezaCpfAposErro()
        } else if (tipo.includes('telefone-duplicado') || detail.includes('telefone')) {
          setTelefoneEmUso(true)
          marcarCampoTocado('telefone')
          setFieldErrors((prev) => ({ ...prev, telefone: MSG_TELEFONE_DUPLICADO }))
        } else if (tipo.includes('email-duplicado') || detail.includes('e-mail') || detail.includes('email')) {
          setEmailEmUso(true)
          marcarCampoTocado('email')
          setFieldErrors((prev) => ({ ...prev, email: MSG_EMAIL_DUPLICADO }))
        }
      }
      setError(traduzirErroApi(err))
    },
  })

  const updateMutation = useMutation({
    mutationFn: () => {
      const payload: ClienteAtualizacaoInput = {
        ...buildClientePayload(form),
        ativo: true,
      }
      return atualizarCliente(clienteId!, payload)
    },
    onSuccess: (data) => {
      setForm(clienteToForm(data))
      setSuccess('Dados atualizados.')
      setError('')
    },
    onError: (err: unknown) => {
      setSuccess('')
      if (err instanceof ApiError && err.status === 409) {
        const detail = (err.problem?.detail ?? err.message).toLowerCase()
        if (detail.includes('telefone')) {
          setTelefoneEmUso(true)
          marcarCampoTocado('telefone')
          setFieldErrors((prev) => ({ ...prev, telefone: MSG_TELEFONE_DUPLICADO }))
        } else if (detail.includes('e-mail') || detail.includes('email')) {
          setEmailEmUso(true)
          marcarCampoTocado('email')
          setFieldErrors((prev) => ({ ...prev, email: MSG_EMAIL_DUPLICADO }))
        }
      }
      setError(traduzirErroApi(err))
    },
  })

  function limparFormulario() {
    setClienteId(null)
    setCpfClienteCarregado(null)
    setCpfBusca('')
    setForm(emptyCliente())
    setError('')
    setSuccess('')
    resetValidacaoFormulario()
    setCpfEmUso(false)
    setTelefoneEmUso(false)
    setEmailEmUso(false)
    setBloquearAutofillInicial(true)
    setFormKey((k) => k + 1)
  }

  function prepararNovoCadastroComCpf(cpf: string) {
    setClienteId(null)
    setCpfClienteCarregado(null)
    setCpfEmUso(false)
    setTelefoneEmUso(false)
    setEmailEmUso(false)
    resetValidacaoFormulario()
    setForm((prev) => formularioNovoComCpf(cpf, { nome: prev.nome, dataNascimento: prev.dataNascimento, sexo: prev.sexo }))
  }

  function sincronizarCpfBuscaNoForm() {
    const cpf = onlyDigits(cpfBusca)
    if (cpf.length !== 11 || isEdicao) return

    if (cpfClienteCarregado && cpf !== cpfClienteCarregado) {
      prepararNovoCadastroComCpf(cpf)
      return
    }

    setForm((prev) => {
      if (prev.cpf === cpf) return prev
      if (temDadosDeCliente(prev) && prev.cpf !== cpf) {
        return formularioNovoComCpf(cpf, { nome: prev.nome, dataNascimento: prev.dataNascimento, sexo: prev.sexo })
      }
      return { ...prev, cpf }
    })
  }

  const salvando =
    cadastroMutation.isPending
    || updateMutation.isPending
    || verificandoCpf
    || verificandoContato

  const proximosCamposBloqueados = cpfImpedeProximosCampos()

  return (
    <div className="h-full min-h-0 min-w-0 grid grid-cols-1 xl:grid-cols-[minmax(0,1fr)_240px] gap-5 lg:gap-6">
      {/* ── Coluna esquerda: formulário vertical ── */}
      <Card className="flex flex-col min-h-0 min-w-0 p-5 sm:p-6 lg:px-8 lg:py-6 order-2 xl:order-1">
        <div className="shrink-0 flex items-center justify-between gap-3 mb-3 pb-3 border-b border-white/10">
          <div>
            <h2 className="font-semibold text-base sm:text-lg">
              {isEdicao ? 'Editar cliente' : 'Cadastrar cliente'}
            </h2>
            <p className="text-xs text-[#8b9cb3] mt-0.5">
              Preencha os dados abaixo ou busque pelo CPF à direita. Lembre-se: para o Cliente ser Cadastrado é necessário preencher todos os campos obrigatórios.
            </p>
          </div>
          {isEdicao && (
            <Badge variant="sky">Em edição</Badge>
          )}
        </div>

        <form
          key={formKey}
          autoComplete="off"
          noValidate
          className="form-sem-autofill relative flex flex-1 flex-col min-h-0 min-w-0"
          onSubmit={(e) => e.preventDefault()}
          onKeyDown={(e) => {
            if (e.key !== 'Enter' || e.defaultPrevented) return
            const target = e.target as HTMLElement
            if (target.tagName === 'TEXTAREA' || target.tagName === 'BUTTON') return
            e.preventDefault()
            avancarCampo(target, e.currentTarget)
          }}
          onFocusCapture={(e) => {
            liberarCamposDoFormulario()
            if (cpfTravandoFoco && e.target !== cpfInputRef.current) {
              e.preventDefault()
              focarCampo(cpfInputRef)
            } else if (dataNascimentoTravandoFoco && e.target !== dataNascimentoInputRef.current) {
              e.preventDefault()
              focarCampo(dataNascimentoInputRef)
            } else if (numeroTravandoFoco && e.target !== numeroInputRef.current) {
              e.preventDefault()
              focarCampo(numeroInputRef)
            } else if (telefoneTravandoFoco && e.target !== telefoneInputRef.current) {
              e.preventDefault()
              focarCampo(telefoneInputRef)
            }
          }}
          onMouseDown={liberarCamposDoFormulario}
        >
          <div
            aria-hidden="true"
            className="absolute size-0 overflow-hidden opacity-0 pointer-events-none"
          >
            <input tabIndex={-1} autoComplete="username" name="prevent_autofill" />
            <input tabIndex={-1} type="password" autoComplete="current-password" name="prevent_autofill_pw" />
          </div>

        <div className="flex-1 min-h-0 min-w-0 overflow-y-auto overscroll-contain space-y-5 pr-0.5">
          <section className="min-w-0">
            <h3 className="text-[10px] uppercase tracking-widest text-[#8b9cb3] mb-2.5">
              Dados pessoais
            </h3>
            <div className="space-y-2.5">
              <Input
                label="Nome completo *"
                name="farmacia_nome_cliente"
                autoComplete={AC.nome}
                readOnly={readOnlyAntiAutofill}
                value={form.nome}
                onChange={(e) => {
                  // sanitizeNomePessoa já limita a 100 chars e remove caracteres inválidos.
                  const nome = sanitizeNomePessoa(e.target.value)
                  setForm({ ...form, nome })
                  if (deveValidarCampo('nome')) {
                    setFieldErrors((prev) => ({
                      ...prev,
                      nome: validarNomePessoa(nome) ?? undefined,
                    }))
                  }
                }}
                onBlur={() => {
                  if (focoDirecionado()) return
                  if (!sanitizeNomePessoa(form.nome).trim()) {
                    marcarCampoTocado('nome')
                    setFieldErrors((prev) => ({ ...prev, nome: 'Lembre-se: Campo Obrigatório.' }))
                    if (nomeResetTimerRef.current) clearTimeout(nomeResetTimerRef.current)
                    nomeResetTimerRef.current = setTimeout(() => {
                      nomeResetTimerRef.current = null
                      setFieldErrors((prev) => ({ ...prev, nome: undefined }))
                      setCamposTocados((prev) => { const { nome: _n, ...rest } = prev; return rest })
                    }, REQUIRED_MSG_DELAY_MS)
                    return
                  }
                  marcarCampoTocado('nome')
                  setFieldErrors((prev) => ({
                    ...prev,
                    nome: validarNomePessoa(form.nome) ?? undefined,
                  }))
                }}
                error={erroCampo('nome')}
                placeholder="Ex.: Jorge Macedo"
                maxLength={100} // alinhado a clientes.nome VARCHAR(100) e @Size(max=100) na API
                className={inputCompact}
              />
              <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-3 min-w-0">
                <Input
                  ref={cpfInputRef}
                  label="CPF *"
                  name="farmacia_cpf_cliente"
                  autoComplete={AC.cpf}
                  readOnly={readOnlyAntiAutofill}
                  value={formatCpfDisplay(form.cpf)}
                  onKeyDown={(e) => {
                    if (
                      e.key === 'Tab'
                      && !e.shiftKey
                      && (cpfImpedeProximosCampos() || verificandoCpf || cpfTravandoFoco)
                    ) {
                      e.preventDefault()
                    }
                    if (e.key === 'Enter' && !isEdicao && onlyDigits(form.cpf).length === 0) {
                      marcarCampoTocado('cpf')
                      setFieldErrors((prev) => ({ ...prev, cpf: 'Lembre-se: Campo Obrigatório.' }))
                      if (cpfResetTimerRef.current) clearTimeout(cpfResetTimerRef.current)
                      cpfResetTimerRef.current = setTimeout(() => {
                        cpfResetTimerRef.current = null
                        setFieldErrors((prev) => ({ ...prev, cpf: undefined }))
                        setCamposTocados((prev) => {
                          const { cpf: _cpf, ...rest } = prev
                          return rest
                        })
                      }, REQUIRED_MSG_DELAY_MS)
                    }
                  }}
                  onChange={(e) => {
                    cancelarLimpezaCpfAgendada()
                    cpfValidacaoRef.current += 1
                    const cpf = onlyDigits(e.target.value).slice(0, 11)
                    setCpfEmUso(false)
                    limparErrosCamposAdjacentesAoCpf()
                    setForm((prev) => {
                      if (
                        !isEdicao
                        && prev.cpf.length === 11
                        && cpf !== prev.cpf
                        && cpfClienteCarregado
                      ) {
                        setCpfClienteCarregado(null)
                        return formularioNovoComCpf(cpf, {
                          nome: prev.nome,
                          dataNascimento: prev.dataNascimento,
                          sexo: prev.sexo,
                        })
                      }
                      return { ...prev, cpf }
                    })
                    if (deveValidarCampo('cpf')) {
                      setFieldErrors((prev) => ({
                        ...prev,
                        cpf: validarCpf(cpf) ?? undefined,
                      }))
                    }
                    if (!isEdicao && cpf.length === 11) {
                      void confirmarCpfCadastro(cpf)
                    }
                  }}
                  onBlur={() => {
                    if (isEdicao) return
                    if (onlyDigits(form.cpf).length === 11) return
                    if (onlyDigits(form.cpf).length === 0) {
                      marcarCampoTocado('cpf')
                      setFieldErrors((prev) => ({ ...prev, cpf: 'Lembre-se: Campo Obrigatório.' }))
                      if (cpfResetTimerRef.current) clearTimeout(cpfResetTimerRef.current)
                      cpfResetTimerRef.current = setTimeout(() => {
                        cpfResetTimerRef.current = null
                        setFieldErrors((prev) => ({ ...prev, cpf: undefined }))
                        setCamposTocados((prev) => { const { cpf: _cpf, ...rest } = prev; return rest })
                      }, REQUIRED_MSG_DELAY_MS)
                      return
                    }
                    marcarCampoTocado('cpf')
                    const cpfErr = validarCpf(form.cpf)
                    setFieldErrors((prev) => ({ ...prev, cpf: cpfErr ?? undefined }))
                    if (cpfErr) {
                      limparErrosCamposAdjacentesAoCpf()
                      agendarLimpezaCpfAposErro(false)
                    }
                  }}
                  error={erroCampo('cpf')}
                  disabled={isEdicao}
                  className={`font-mono ${inputCompact}`}
                  placeholder="000.000.000-00"
                />
                <DataNascimentoInput
                  ref={dataNascimentoInputRef}
                  label="Data de nascimento *"
                  name="farmacia_data_nascimento"
                  autoComplete={AC.data}
                  readOnly={readOnlyAntiAutofill && !proximosCamposBloqueados}
                  disabled={proximosCamposBloqueados}
                  value={form.dataNascimento ?? ''}
                  onChange={(dataNascimento) => {
                    if (proximosCamposBloqueados) return
                    cancelarLimpezaDataNascimentoAgendada()
                    setForm({ ...form, dataNascimento })
                    const err = dataNascimento.trim()
                      ? validarDataNascimentoBr(dataNascimento) ?? undefined
                      : undefined
                    if (deveValidarCampo('dataNascimento')) {
                      setFieldErrors((prev) => ({ ...prev, dataNascimento: err }))
                    }
                  }}
                  onKeyDown={(e) => {
                    if (e.key !== 'Enter') return
                    if (!form.dataNascimento?.trim()) {
                      marcarCampoTocado('dataNascimento')
                      setFieldErrors((prev) => ({ ...prev, dataNascimento: 'Lembre-se: Campo Obrigatório.' }))
                      if (dataNascimentoResetTimerRef.current) clearTimeout(dataNascimentoResetTimerRef.current)
                      dataNascimentoResetTimerRef.current = setTimeout(() => {
                        dataNascimentoResetTimerRef.current = null
                        setFieldErrors((prev) => ({ ...prev, dataNascimento: undefined }))
                        setCamposTocados((prev) => { const { dataNascimento: _d, ...rest } = prev; return rest })
                      }, REQUIRED_MSG_DELAY_MS)
                      return
                    }
                    const err = erroDataNascimento()
                    if (err) {
                      e.preventDefault()
                      marcarCampoTocado('dataNascimento')
                      setFieldErrors((prev) => ({ ...prev, dataNascimento: err }))
                      agendarLimpezaDataNascimentoAposErro(false)
                    }
                  }}
                  onBlur={() => {
                    if (focoDirecionado() || proximosCamposBloqueados) return
                    if (!form.dataNascimento?.trim()) {
                      marcarCampoTocado('dataNascimento')
                      setFieldErrors((prev) => ({ ...prev, dataNascimento: 'Lembre-se: Campo Obrigatório.' }))
                      if (dataNascimentoResetTimerRef.current) clearTimeout(dataNascimentoResetTimerRef.current)
                      dataNascimentoResetTimerRef.current = setTimeout(() => {
                        dataNascimentoResetTimerRef.current = null
                        setFieldErrors((prev) => ({ ...prev, dataNascimento: undefined }))
                        setCamposTocados((prev) => { const { dataNascimento: _d, ...rest } = prev; return rest })
                      }, REQUIRED_MSG_DELAY_MS)
                      return
                    }
                    marcarCampoTocado('dataNascimento')
                    const err = erroDataNascimento() ?? undefined
                    setFieldErrors((prev) => ({ ...prev, dataNascimento: err }))
                    if (err) agendarLimpezaDataNascimentoAposErro(false)
                  }}
                  error={erroCampo('dataNascimento')}
                  className={inputCompact}
                />
                <Select
                  label="Sexo *"
                  value={form.sexo ?? ''}
                  disabled={proximosCamposBloqueados || readOnlyAntiAutofill}
                  onChange={(v) => {
                    if (proximosCamposBloqueados) return
                    setForm({ ...form, sexo: v })
                    if (deveValidarCampo('sexo')) {
                      setFieldErrors((prev) => ({
                        ...prev,
                        sexo: validarSexo(v) ?? undefined,
                      }))
                    }
                  }}
                  onBlur={() => {
                    if (focoDirecionado() || proximosCamposBloqueados) return
                    if (!(form.sexo ?? '').trim()) {
                      marcarCampoTocado('sexo')
                      setFieldErrors((prev) => ({ ...prev, sexo: 'Lembre-se: Campo Obrigatório.' }))
                      if (sexoResetTimerRef.current) clearTimeout(sexoResetTimerRef.current)
                      sexoResetTimerRef.current = setTimeout(() => {
                        sexoResetTimerRef.current = null
                        setFieldErrors((prev) => ({ ...prev, sexo: undefined }))
                        setCamposTocados((prev) => { const { sexo: _s, ...rest } = prev; return rest })
                      }, REQUIRED_MSG_DELAY_MS)
                      return
                    }
                    marcarCampoTocado('sexo')
                    setFieldErrors((prev) => ({
                      ...prev,
                      sexo: validarSexo(form.sexo ?? '') ?? undefined,
                    }))
                  }}
                  options={SEXO_OPCOES}
                  placeholder="Selecione…"
                  error={erroCampo('sexo')}
                  className={inputCompact}
                />
              </div>
            </div>
          </section>

          <section className="min-w-0">
            <h3 className="text-[10px] uppercase tracking-widest text-[#8b9cb3] mb-2.5">Contato</h3>
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-3 min-w-0">
              <Input
                ref={telefoneInputRef}
                label="Telefone / WhatsApp *"
                name="farmacia_telefone_cliente"
                autoComplete={AC.telefone}
                readOnly={readOnlyAntiAutofill}
                inputMode="numeric"
                value={formatTelefoneDisplay(form.telefone ?? '')}
                onChange={(e) => {
                  const telefone = onlyDigits(e.target.value).slice(0, 11)
                  setTelefoneEmUso(false)
                  setForm({ ...form, telefone })
                  setFieldErrors((prev) => ({
                    ...prev,
                    telefone: deveValidarCampo('telefone')
                      ? validarTelefone(telefone, true) ?? undefined
                      : undefined,
                  }))
                }}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' && !onlyDigits(form.telefone ?? '').length) {
                    marcarCampoTocado('telefone')
                    setFieldErrors((prev) => ({ ...prev, telefone: 'Lembre-se: Campo Obrigatório.' }))
                    if (telefoneResetTimerRef.current) clearTimeout(telefoneResetTimerRef.current)
                    telefoneResetTimerRef.current = setTimeout(() => {
                      telefoneResetTimerRef.current = null
                      setFieldErrors((prev) => ({ ...prev, telefone: undefined }))
                      setCamposTocados((prev) => { const { telefone: _t, ...rest } = prev; return rest })
                    }, REQUIRED_MSG_DELAY_MS)
                  }
                }}
                onBlur={() => {
                  if (focoDirecionado()) return
                  if (!onlyDigits(form.telefone ?? '').length) {
                    marcarCampoTocado('telefone')
                    setFieldErrors((prev) => ({ ...prev, telefone: 'Lembre-se: Campo Obrigatório.' }))
                    if (telefoneResetTimerRef.current) clearTimeout(telefoneResetTimerRef.current)
                    telefoneResetTimerRef.current = setTimeout(() => {
                      telefoneResetTimerRef.current = null
                      setFieldErrors((prev) => ({ ...prev, telefone: undefined }))
                      setCamposTocados((prev) => { const { telefone: _t, ...rest } = prev; return rest })
                    }, REQUIRED_MSG_DELAY_MS)
                    return
                  }
                  cancelarLimpezaTelefoneAgendada()
                  void confirmarTelefone()
                }}
                error={erroCampo('telefone')}
                placeholder="(11) 99999-9999"
                className={`font-mono ${inputCompact}`}
              />
              <Input
                ref={emailInputRef}
                label="E-mail *"
                type="text"
                inputMode="email"
                spellCheck={false}
                name="farmacia_email_cliente"
                autoComplete={AC.email}
                readOnly={readOnlyAntiAutofill}
                value={sanitizeEmailInput(form.email ?? '')}
                onKeyDown={(e) => {
                  if (e.key === ' ' || e.key === 'Spacebar') e.preventDefault()
                  if (e.key === 'Enter') {
                    const emailValue = sanitizeEmailInput(form.email ?? '')
                    if (!emailValue.trim()) {
                      marcarCampoTocado('email')
                      setFieldErrors((prev) => ({ ...prev, email: 'Lembre-se: Campo Obrigatório.' }))
                      if (emailResetTimerRef.current) clearTimeout(emailResetTimerRef.current)
                      emailResetTimerRef.current = setTimeout(() => {
                        emailResetTimerRef.current = null
                        setFieldErrors((prev) => ({ ...prev, email: undefined }))
                        setCamposTocados((prev) => { const { email: _email, ...rest } = prev; return rest })
                      }, REQUIRED_MSG_DELAY_MS)
                      return
                    }
                    e.preventDefault()
                    const target = e.currentTarget
                    const form = target.closest('form')
                    void confirmarEmail(true).then((valido) => {
                      if (valido && form) avancarCampo(target, form)
                    })
                  }
                }}
                onPaste={(e) => {
                  e.preventDefault()
                  const pasted = sanitizeEmailInput(e.clipboardData.getData('text'))
                  setEmailEmUso(false)
                  setForm({ ...form, email: pasted })
                  setFieldErrors((prev) => ({
                    ...prev,
                    email: deveValidarCampo('email')
                      ? validarEmail(pasted) ?? undefined
                      : undefined,
                  }))
                }}
                onChange={(e) => {
                  setEmailEmUso(false)
                  const email = sanitizeEmailInput(e.target.value)
                  setForm({ ...form, email })
                  setFieldErrors((prev) => ({
                    ...prev,
                    email: deveValidarCampo('email')
                      ? validarEmail(email, true) ?? undefined
                      : undefined,
                  }))
                }}
                onInput={(e) => {
                  const raw = e.currentTarget.value
                  const email = sanitizeEmailInput(raw)
                  if (raw !== email) {
                    e.currentTarget.value = email
                    setForm((prev) => ({ ...prev, email }))
                  }
                }}
                onBlur={() => {
                  if (focoDirecionado()) return
                  const emailValue = sanitizeEmailInput(form.email ?? '')
                  if (!emailValue.trim()) {
                    marcarCampoTocado('email')
                    setFieldErrors((prev) => ({ ...prev, email: 'Lembre-se: Campo Obrigatório.' }))
                    if (emailResetTimerRef.current) clearTimeout(emailResetTimerRef.current)
                    emailResetTimerRef.current = setTimeout(() => {
                      emailResetTimerRef.current = null
                      setFieldErrors((prev) => ({ ...prev, email: undefined }))
                      setCamposTocados((prev) => { const { email: _email, ...rest } = prev; return rest })
                    }, REQUIRED_MSG_DELAY_MS)
                    return
                  }
                  marcarCampoTocado('email')
                  void confirmarEmail()
                }}
                error={erroCampo('email')}
                placeholder="cliente@email.com"
                className={inputCompact}
              />
            </div>
          </section>

          <section className="min-w-0">
            <h3 className="text-[10px] uppercase tracking-widest text-[#8b9cb3] mb-2.5">Endereço</h3>
            <div className="space-y-2.5 min-w-0">
              <div className="grid grid-cols-1 sm:grid-cols-[minmax(0,1fr)_100px] gap-2.5 min-w-0">
                <Input
                  label="Logradouro *"
                  name="farmacia_logradouro"
                  autoComplete={AC.endereco}
                  readOnly={readOnlyAntiAutofill}
                  value={form.endereco?.logradouro ?? ''}
                  onChange={(e) => {
                    const logradouro = e.target.value
                    setForm(patchEndereco(form, { logradouro }))
                    if (deveValidarCampo('logradouro')) {
                      setFieldErrors((prev) => ({
                        ...prev,
                        logradouro: validarLogradouro(logradouro) ?? undefined,
                      }))
                    }
                  }}
                  onBlur={() => {
                    if (focoDirecionado()) return
                    if (!(form.endereco?.logradouro ?? '').trim()) {
                      marcarCampoTocado('logradouro')
                      setFieldErrors((prev) => ({ ...prev, logradouro: 'Lembre-se: Campo Obrigatório.' }))
                      if (logradouroResetTimerRef.current) clearTimeout(logradouroResetTimerRef.current)
                      logradouroResetTimerRef.current = setTimeout(() => {
                        logradouroResetTimerRef.current = null
                        setFieldErrors((prev) => ({ ...prev, logradouro: undefined }))
                        setCamposTocados((prev) => { const { logradouro: _l, ...rest } = prev; return rest })
                      }, REQUIRED_MSG_DELAY_MS)
                      return
                    }
                    marcarCampoTocado('logradouro')
                    setFieldErrors((prev) => ({
                      ...prev,
                      logradouro: validarLogradouro(form.endereco?.logradouro ?? '') ?? undefined,
                    }))
                  }}
                  error={erroCampo('logradouro')}
                  placeholder="Rua, avenida…"
                  className={inputCompact}
                />
                <Input
                  ref={numeroInputRef}
                  label="Nº"
                  name="farmacia_numero"
                  autoComplete={AC.endereco}
                  readOnly={readOnlyAntiAutofill}
                  maxLength={8}
                  value={form.endereco?.numero ?? ''}
                  onChange={(e) => {
                    cancelarLimpezaNumeroAgendada()
                    const numero = e.target.value.replace(/[^a-zA-Z0-9]/g, '').toUpperCase().slice(0, 8)
                    setForm(patchEndereco(form, { numero }))
                    if (deveValidarCampo('numero')) {
                      setFieldErrors((prev) => ({
                        ...prev,
                        numero: numero && !/\d/.test(numero)
                          ? 'Conter pelo menos um dígito (ex: 123, 45A).'
                          : undefined,
                      }))
                    }
                  }}
                  onKeyDown={(e) => {
                    if (e.key !== 'Enter') return
                    const numero = form.endereco?.numero ?? ''
                    const err = numero && !/\d/.test(numero)
                      ? 'Conter pelo menos um dígito (ex: 123, 45A).' : undefined
                    if (err) {
                      e.preventDefault()
                      marcarCampoTocado('numero')
                      setFieldErrors((prev) => ({ ...prev, numero: err }))
                      agendarLimpezaNumeroAposErro()
                    }
                  }}
                  onBlur={() => {
                    if (focoDirecionado()) return
                    marcarCampoTocado('numero')
                    const numero = form.endereco?.numero ?? ''
                    const err = numero && !/\d/.test(numero)
                      ? 'Conter pelo menos um dígito (ex: 123, 45A).' : undefined
                    setFieldErrors((prev) => ({ ...prev, numero: err }))
                    if (err) agendarLimpezaNumeroAposErro()
                  }}
                  error={erroCampo('numero')}
                  className={inputCompact}
                />
              </div>
              <div className="grid grid-cols-1 sm:grid-cols-[minmax(0,1fr)_minmax(0,1fr)_minmax(0,9rem)] gap-2.5 min-w-0">
                <Input
                  label="Complemento"
                  name="farmacia_complemento"
                  autoComplete={AC.endereco}
                  readOnly={readOnlyAntiAutofill}
                  placeholder="Ex: Casa, Apto 12"
                  value={form.endereco?.complemento ?? ''}
                  onChange={(e) =>
                    setForm(patchEndereco(form, { complemento: e.target.value }))
                  }
                  className={inputCompact}
                />
                <Input
                  label="Bairro *"
                  name="farmacia_bairro"
                  autoComplete={AC.endereco}
                  readOnly={readOnlyAntiAutofill}
                  value={form.endereco?.bairro ?? ''}
                  onChange={(e) => {
                    const bairro = e.target.value
                    setForm(patchEndereco(form, { bairro }))
                    if (deveValidarCampo('bairro')) {
                      setFieldErrors((prev) => ({
                        ...prev,
                        bairro: validarBairro(bairro) ?? undefined,
                      }))
                    }
                  }}
                  onBlur={() => {
                    if (focoDirecionado()) return
                    if (!(form.endereco?.bairro ?? '').trim()) {
                      marcarCampoTocado('bairro')
                      setFieldErrors((prev) => ({ ...prev, bairro: 'Lembre-se: Campo Obrigatório.' }))
                      if (bairroResetTimerRef.current) clearTimeout(bairroResetTimerRef.current)
                      bairroResetTimerRef.current = setTimeout(() => {
                        bairroResetTimerRef.current = null
                        setFieldErrors((prev) => ({ ...prev, bairro: undefined }))
                        setCamposTocados((prev) => { const { bairro: _b, ...rest } = prev; return rest })
                      }, REQUIRED_MSG_DELAY_MS)
                      return
                    }
                    marcarCampoTocado('bairro')
                    setFieldErrors((prev) => ({
                      ...prev,
                      bairro: validarBairro(form.endereco?.bairro ?? '') ?? undefined,
                    }))
                  }}
                  error={erroCampo('bairro')}
                  className={inputCompact}
                />
                <Input
                  label="CEP *"
                  name="farmacia_cep"
                  autoComplete={AC.endereco}
                  readOnly={readOnlyAntiAutofill}
                  value={formatCepDisplay(form.endereco?.cep ?? '')}
                  onChange={(e) => {
                    const cep = onlyDigits(e.target.value).slice(0, 8)
                    setForm(patchEndereco(form, { cep }))
                    if (deveValidarCampo('cep')) {
                      setFieldErrors((prev) => ({
                        ...prev,
                        cep: validarCep(cep) ?? undefined,
                      }))
                    }
                  }}
                  onBlur={() => {
                    if (focoDirecionado()) return
                    if (!onlyDigits(form.endereco?.cep ?? '')) {
                      marcarCampoTocado('cep')
                      setFieldErrors((prev) => ({ ...prev, cep: 'Lembre-se: Campo Obrigatório.' }))
                      if (cepResetTimerRef.current) clearTimeout(cepResetTimerRef.current)
                      cepResetTimerRef.current = setTimeout(() => {
                        cepResetTimerRef.current = null
                        setFieldErrors((prev) => ({ ...prev, cep: undefined }))
                        setCamposTocados((prev) => { const { cep: _c, ...rest } = prev; return rest })
                      }, REQUIRED_MSG_DELAY_MS)
                      return
                    }
                    marcarCampoTocado('cep')
                    setFieldErrors((prev) => ({
                      ...prev,
                      cep: validarCep(form.endereco?.cep ?? '') ?? undefined,
                    }))
                  }}
                  error={erroCampo('cep')}
                  className={`font-mono ${inputCompact}`}
                  placeholder="00000-000"
                />
              </div>
              {/* UF antes da cidade: ao trocar UF, cidade incompatível é limpa e revalidada. */}
              <div className="grid grid-cols-1 sm:grid-cols-[minmax(0,5.5rem)_minmax(0,1fr)] gap-2.5 min-w-0">
                <Select
                  label="UF *"
                  value={form.endereco?.uf ?? ''}
                  onChange={(v) => {
                    const cidadeAtual = form.endereco?.cidade ?? ''
                    const patch: Partial<Endereco> = { uf: v }
                    if (cidadeAtual && !municipioPertenceAUf(cidadeAtual, v)) {
                      patch.cidade = ''
                    }
                    setForm(patchEndereco(form, patch))
                    marcarCampoTocado('uf')
                    setFieldErrors((prev) => ({
                      ...prev,
                      uf: validarUfEndereco(v) ?? undefined,
                      ...(deveValidarCampo('cidade') || patch.cidade === ''
                        ? {
                            cidade:
                              validarCidadeObrigatoria(
                                patch.cidade === '' ? '' : cidadeAtual,
                                v,
                              ) ?? undefined,
                          }
                        : {}),
                    }))
                  }}
                  onBlur={() => {
                    if (focoDirecionado()) return
                    if (!(form.endereco?.uf ?? '').trim()) {
                      marcarCampoTocado('uf')
                      setFieldErrors((prev) => ({ ...prev, uf: 'Lembre-se: Campo Obrigatório.' }))
                      if (ufResetTimerRef.current) clearTimeout(ufResetTimerRef.current)
                      ufResetTimerRef.current = setTimeout(() => {
                        ufResetTimerRef.current = null
                        setFieldErrors((prev) => ({ ...prev, uf: undefined }))
                        setCamposTocados((prev) => { const { uf: _u, ...rest } = prev; return rest })
                      }, REQUIRED_MSG_DELAY_MS)
                      return
                    }
                    marcarCampoTocado('uf')
                    setFieldErrors((prev) => ({
                      ...prev,
                      uf: validarUfEndereco(form.endereco?.uf ?? '') ?? undefined,
                    }))
                  }}
                  options={UFS_BR.map((uf) => ({ value: uf, label: uf }))}
                  placeholder="UF"
                  searchable
                  error={erroCampo('uf')}
                  className={inputCompact}
                />
                <CidadePorUfSelect
                  label="Cidade *"
                  uf={form.endereco?.uf ?? ''}
                  value={form.endereco?.cidade ?? ''}
                  onChange={(cidade) => {
                    setForm(patchEndereco(form, { cidade }))
                    marcarCampoTocado('cidade')
                    setFieldErrors((prev) => ({
                      ...prev,
                      cidade:
                        validarCidadeObrigatoria(cidade, form.endereco?.uf ?? '') ?? undefined,
                    }))
                  }}
                  error={erroCampo('cidade')}
                  className={inputCompact}
                  disabled={readOnlyAntiAutofill}
                />
              </div>
            </div>
          </section>

          <section className="min-w-0">
            <h3 className="text-[10px] uppercase tracking-widest text-[#8b9cb3] mb-2.5">
              Informações clínicas
            </h3>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5 min-w-0">
              <div>
                <label className="block text-xs font-medium text-[#8b9cb3] mb-1.5">
                  Alergias
                </label>
                <textarea
                  name="farmacia_alergias"
                  autoComplete={AC.clinico}
                  readOnly={readOnlyAntiAutofill}
                  value={form.alergias ?? ''}
                  onChange={(e) => setForm({ ...form, alergias: e.target.value })}
                  rows={3}
                  className={textareaClass}
                  placeholder="Dipirona, penicilina…"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-[#8b9cb3] mb-1.5">
                  Observações
                </label>
                <textarea
                  name="farmacia_observacoes"
                  autoComplete={AC.clinico}
                  readOnly={readOnlyAntiAutofill}
                  value={form.observacoes ?? ''}
                  onChange={(e) => setForm({ ...form, observacoes: e.target.value })}
                  rows={3}
                  className={textareaClass}
                  // Exemplo de uso clínico — não substitui o campo Alergias acima.
                  placeholder="Cuidados com medicamentos alérgicos."
                />
              </div>
            </div>
          </section>
        </div>

        <div className="shrink-0 pt-3 mt-3 border-t border-white/10 flex flex-wrap gap-2">
          {isEdicao ? (
            <>
              <Button type="button" loading={salvando} disabled={salvando} onClick={tentarAtualizar}>
                Salvar alterações
              </Button>
              <Button type="button" variant="ghost" size="sm" onClick={limparFormulario}>
                <RotateCcw className="size-4" />
                Novo cliente
              </Button>
            </>
          ) : (
            <Button
              type="button"
              loading={salvando}
              disabled={salvando || !formularioPronto()}
              onClick={tentarCadastro}
            >
              Cadastrar cliente
            </Button>
          )}
        </div>
        </form>
      </Card>

      {/* ── Coluna direita: busca compacta + resumo ── */}
      <aside className="flex flex-col gap-3 min-h-0 order-1 xl:order-2 xl:w-[240px] xl:shrink-0">
        <Card className="p-4 shrink-0">
          <h2 className="text-sm font-semibold mb-3 flex items-center gap-2">
            <Search className="size-4 text-mint" />
            Buscar por CPF
          </h2>
          <Input
            label="CPF"
            name="farmacia_busca_cpf"
            autoComplete={AC.cpf}
            value={cpfBusca}
            onChange={(e) => {
              setCpfBusca(formatCpfDisplay(e.target.value))
              if (clienteId && onlyDigits(e.target.value).length === 11) {
                const cpf = onlyDigits(e.target.value)
                if (cpf !== cpfClienteCarregado) {
                  prepararNovoCadastroComCpf(cpf)
                }
              }
            }}
            onBlur={sincronizarCpfBuscaNoForm}
            onKeyDown={(e) => {
              if (e.key === 'Enter' && onlyDigits(cpfBusca).length === 11) {
                buscarMutation.mutate()
              }
            }}
            placeholder="000.000.000-00"
            className={`font-mono ${inputCompact}`}
          />
          <div className="flex flex-col gap-2 mt-3">
            <Button
              variant="secondary"
              size="sm"
              className="w-full"
              loading={buscarMutation.isPending}
              disabled={onlyDigits(cpfBusca).length !== 11}
              onClick={() => buscarMutation.mutate()}
            >
              <Search className="size-4" />
              Buscar
            </Button>
            <Button variant="ghost" size="sm" className="w-full" onClick={limparFormulario}>
              <UserPlus className="size-4" />
              Novo cadastro
            </Button>
          </div>
        </Card>

        <Card className="flex-1 min-h-0 flex flex-col p-4 overflow-hidden">
          {error && (
            <p className="text-xs text-coral mb-3 shrink-0 leading-relaxed">{error}</p>
          )}
          {success && (
            <p className="text-xs text-mint mb-3 shrink-0 leading-relaxed">{success}</p>
          )}

          {form.nome.trim() ? (
            <div className="space-y-3 overflow-y-auto min-h-0">
              <p className="text-[10px] uppercase tracking-widest text-[#8b9cb3]">
                {isEdicao ? 'Cliente selecionado' : 'Pré-visualização'}
              </p>
              <div className="glass rounded-xl p-3 space-y-2.5">
                <div className="flex items-start gap-2">
                  <User className="size-4 text-mint shrink-0 mt-0.5" />
                  <div className="min-w-0">
                    <p className="font-semibold text-sm truncate">{form.nome}</p>
                    <p className="text-xs font-mono text-[#8b9cb3]">
                      {formatCpfDisplay(form.cpf) || 'CPF pendente'}
                    </p>
                  </div>
                </div>
                {(form.telefone || form.email) && (
                  <div className="flex items-start gap-2 text-xs text-[#8b9cb3]">
                    <Phone className="size-3.5 shrink-0 mt-0.5" />
                    <span className="break-all">
                      {[
                        form.telefone ? formatTelefoneDisplay(form.telefone) : '',
                        form.email ? sanitizeEmailInput(form.email) : '',
                      ].filter(Boolean).join(' · ')}
                    </span>
                  </div>
                )}
                {form.endereco?.cidade && (
                  <div className="flex items-start gap-2 text-xs text-[#8b9cb3]">
                    <MapPin className="size-3.5 shrink-0 mt-0.5" />
                    <span>
                      {form.endereco.cidade}
                      {form.endereco.uf ? `/${form.endereco.uf}` : ''}
                    </span>
                  </div>
                )}
                <div className="flex flex-wrap gap-1.5 pt-1">
                  {form.sexo && (
                    <Badge variant="sky">{sexoLabel(form.sexo)}</Badge>
                  )}
                  {form.dataNascimento && !validarDataNascimentoBr(form.dataNascimento) && (
                    <Badge variant="mint">{form.dataNascimento}</Badge>
                  )}
                  {isEdicao && <Badge variant="amber">Cadastrado</Badge>}
                </div>
              </div>
            </div>
          ) : (
            <div className="flex-1 flex flex-col justify-center text-center px-2">
              <User className="size-10 text-[#8b9cb3]/30 mx-auto mb-3" />
              <p className="text-xs text-[#8b9cb3] leading-relaxed">
                Digite um CPF e clique em <strong className="text-white">Buscar</strong> para
                carregar um cliente existente, ou preencha o formulário à esquerda.
              </p>
            </div>
          )}
        </Card>
      </aside>
    </div>
  )
}
