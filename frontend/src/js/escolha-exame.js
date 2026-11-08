/* =================================
   ELEMENTOS DA PÁGINA
================================= */

const botoesExame = document.querySelectorAll('[data-exame]');
const mensagemSelecao = document.getElementById('mensagem-selecao');
const botaoContinuar = document.getElementById('btn-continuar');

/* =================================
   CONFIGURAÇÃO DO ARMAZENAMENTO
================================= */

const CHAVE_EXAME = 'exameSelecionado';
// Altere para 'rotina.html' caso a Issue #15 crie um arquivo com esse nome
const PAGINA_ROTINA = 'rotina.html'; // USADO DE EXEMPLO, IMPLEMENTAÇÃO DE ROTINA AINDA AUSENTE.

const examesValidos = [
    'enem',
    'fuvest',
    'unicamp',
    'unesp',
    'outros'
];

/* =================================
   RECUPERAR SELEÇÃO ANTERIOR
================================= */

let exameSelecionado = sessionStorage.getItem(CHAVE_EXAME);

if (!examesValidos.includes(exameSelecionado)) {
    exameSelecionado = null;
    sessionStorage.removeItem(CHAVE_EXAME);
}

/* =================================
   ATUALIZAR DESTAQUE DOS CARDS
================================= */

function atualizarSelecao() {
    botoesExame.forEach(botao => {
        const selecionado = botao.dataset.exame === exameSelecionado;
        botao.classList.toggle('selecionado', selecionado);
        botao.setAttribute('aria-pressed', selecionado);
    });

    if (exameSelecionado) {
        mensagemSelecao.textContent = 'Exame selecionado: ' + exameSelecionado.toUpperCase();
        botaoContinuar.disabled = false;
    } else {
        mensagemSelecao.textContent = 'Selecione um exame para continuar.';
        botaoContinuar.disabled = true;
    }
}

/* =================================
   EVENTOS
================================= */

botoesExame.forEach(botao => {
    botao.addEventListener('click', () => {
        exameSelecionado = botao.dataset.exame;
        sessionStorage.setItem(CHAVE_EXAME, exameSelecionado);
        atualizarSelecao();
    });
});

botaoContinuar.addEventListener('click', () => {
    if (!exameSelecionado) return;
    location.href = PAGINA_ROTINA;
});

/* =================================
   INICIALIZAÇÃO
================================= */

atualizarSelecao();