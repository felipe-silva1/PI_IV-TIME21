/* =================================
   ELEMENTOS DA PÁGINA
================================= */

// Recupera todos os botões de exames
const botoesExame = document.querySelectorAll('[data-exame]');

// Elemento para exibir mensagens
const mensagemSelecao =
    document.getElementById('mensagem-selecao');


/* =================================
   CONFIGURAÇÃO DO ARMAZENAMENTO
================================= */

// Nome da chave utilizada no sessionStorage
const CHAVE_EXAME = 'exameSelecionado';

// Opções válidas
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

// Recupera o exame que já estava guardado
let exameSelecionado =
    sessionStorage.getItem(CHAVE_EXAME);

// Verifica se o valor armazenado é válido
if (!examesValidos.includes(exameSelecionado)) {
    exameSelecionado = null;
    sessionStorage.removeItem(CHAVE_EXAME);
}
/* =================================
   ATUALIZAR DESTAQUE DOS CARDS
================================= */

function atualizarSelecao() {

    botoesExame.forEach(botao => {

        // Verifica se este botão representa o exame escolhido
        const selecionado =
            botao.dataset.exame === exameSelecionado;

        // Adiciona ou remove a classe de destaque
        botao.classList.toggle(
            'selecionado',
            selecionado
        );

        // Atualiza a informação de acessibilidade
        botao.setAttribute(
            'aria-pressed',
            selecionado
        );

    });

    // Atualiza a mensagem apresentada ao usuário
    if (exameSelecionado) {

        mensagemSelecao.textContent =
            'Exame selecionado: ' +
            exameSelecionado.toUpperCase();

    } else {

        mensagemSelecao.textContent =
            'Selecione um exame para continuar.';

    }

}


/* =================================
   EVENTO DE SELEÇÃO
================================= */

botoesExame.forEach(botao => {

    botao.addEventListener('click', () => {

        // Identifica o exame correspondente ao botão
        exameSelecionado = botao.dataset.exame;

        // Guarda a escolha no navegador
        sessionStorage.setItem(
            CHAVE_EXAME,
            exameSelecionado
        );

        // Atualiza o destaque visual
        atualizarSelecao();

    });

});


/* =================================
   INICIALIZAÇÃO
================================= */

// Recupera e apresenta a seleção ao abrir a página
atualizarSelecao();
