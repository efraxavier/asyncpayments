package com.example.asyncpayments.service;

import com.example.asyncpayments.entity.Transacao;
import com.example.asyncpayments.entity.Conta;
import com.example.asyncpayments.entity.ContaSincrona;
import com.example.asyncpayments.entity.TipoConta;
import com.example.asyncpayments.entity.TipoTransacao;
import com.example.asyncpayments.repository.ContaSincronaRepository;
import com.example.asyncpayments.repository.TransacaoRepository;
import com.example.asyncpayments.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransacaoService {

    private final TransacaoRepository transacaoRepository;
    private final UserRepository userRepository;
    private final ContaSincronaRepository contaSincronaRepository;

    public Transacao criarTransacao(Transacao transacao, String emailUsuarioOrigem) {
        transacao.setDataCriacao(LocalDateTime.now());
        transacao.setSincronizada(false); // Por padrão, transações não são sincronizadas
        return transacaoRepository.save(transacao);
    }

    public Long buscarIdUsuarioPorEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"))
                .getId();
    }

    public List<Transacao> listarTransacoes(String emailUsuario) {
        Long idUsuario = buscarIdUsuarioPorEmail(emailUsuario);
        return transacaoRepository.findByIdUsuarioOrigem(idUsuario);
    }

    @Transactional
    public Transacao realizarTransacaoSincrona(Long idUsuarioOrigem, Long idUsuarioDestino, Double valor) {
        // Verificar se o valor é válido
        if (valor <= 0) {
            throw new IllegalArgumentException("O valor da transação deve ser maior que zero.");
        }

        // Buscar contas dos usuários
        Conta contaOrigem = contaSincronaRepository.findByUserId(idUsuarioOrigem);
        Conta contaDestino = contaSincronaRepository.findByUserId(idUsuarioDestino);
    
        if (contaOrigem == null || contaDestino == null) {
            throw new IllegalArgumentException("Conta de origem ou destino não encontrada.");
        }

        // Verificar saldo suficiente
        if (contaOrigem.getSaldo() < valor) {
            throw new IllegalArgumentException("Saldo insuficiente na conta de origem.");
        }

        // Atualizar saldos
        contaOrigem.setSaldo(contaOrigem.getSaldo() - valor);
        contaDestino.setSaldo(contaDestino.getSaldo() + valor);

        // Salvar alterações nas contas
        if (contaOrigem instanceof ContaSincrona && contaDestino instanceof ContaSincrona) {
            contaSincronaRepository.save((ContaSincrona) contaOrigem);
            contaSincronaRepository.save((ContaSincrona) contaDestino);
        } else {
            throw new IllegalArgumentException("As contas não são do tipo ContaSincrona.");
        }

        // Criar e salvar a transação
        Transacao transacao = new Transacao();
        transacao.setIdUsuarioOrigem(idUsuarioOrigem);
        transacao.setIdUsuarioDestino(idUsuarioDestino);
        transacao.setValor(valor);
        transacao.setTipoTransacao(TipoTransacao.SINCRONA);
        transacao.setServicoPagamento("Stripe"); // Exemplo de serviço de pagamento
        transacao.setSincronizada(true);

        return transacaoRepository.save(transacao);
    }

    public Transacao realizarTransacaoAssincrona(String emailUsuarioOrigem, Long idUsuarioDestino, Double valor, String metodoConexao) {
        // Lógica para transações assíncronas
        Transacao transacao = new Transacao();
        transacao.setIdUsuarioOrigem(buscarIdUsuarioPorEmail(emailUsuarioOrigem));
        transacao.setIdUsuarioDestino(idUsuarioDestino);
        transacao.setValor(valor);
        transacao.setTipoTransacao(TipoTransacao.ASSINCRONA);
        transacao.setMetodoConexao(metodoConexao);
        transacao.setDataCriacao(LocalDateTime.now());
        transacao.setSincronizada(false); // Transações assíncronas não são sincronizadas inicialmente
        return transacaoRepository.save(transacao);
    }
}