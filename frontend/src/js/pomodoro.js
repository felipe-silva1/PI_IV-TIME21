//Autor: Felipe Augusto dos Santos Silva RA: 25003353
(() => {
    const API_POMODOROS = 'http://localhost:8080/api/pomodoros';
    const API_TAREFAS = 'http://localhost:8080/api/tarefas';
    const etapas = {
        foco: { nome: 'Foco', minutos: 25 },
        'pausa-curta': { nome: 'Pausa curta', minutos: 5 },
        'pausa-longa': { nome: 'Pausa longa', minutos: 15 }
    };

    const faseEl = document.querySelector('#pomodoro-fase');
    const tempoEl = document.querySelector('#pomodoro-tempo');
    const iniciar = document.querySelector('#pomodoro-iniciar');
    const reiniciar = document.querySelector('#pomodoro-reiniciar');
    const tarefaEl = document.querySelector('#pomodoro-tarefa');
    const registroEl = document.querySelector('#pomodoro-registro');
    const reenviar = document.querySelector('#pomodoro-reenviar');
    const usuarioId = sessionStorage.getItem('guiavest.usuarioId');

    if (!usuarioId || !sessionStorage.getItem(AUTH_TOKEN_KEY)) {
        location.replace('../login.html');
        return;
    }

    const prefixoSessoes = `guiavest.pomodoro.pendente.${usuarioId}.`;
    const pendentes = new Map();
    let enviando = false;

    let fase = 'foco';
    let focosConcluidos = 0;
    let restante = etapas[fase].minutos * 60_000;
    // Sem horário de término, a etapa está pronta para iniciar ou pausada.
    let termino = null;
    let intervalo;

    function voltarAoLogin() {
        limparSessao();
        location.replace('../login.html');
    }

    // O aluno pode vincular o foco a uma tarefa de hoje ou seguir em estudo livre.
    async function carregarTarefas() {
        const hoje = new Date();
        const mes = String(hoje.getMonth() + 1).padStart(2, '0');
        const dia = String(hoje.getDate()).padStart(2, '0');
        const data = `${hoje.getFullYear()}-${mes}-${dia}`;

        try {
            const resposta = await requisicaoAutenticada(`${API_TAREFAS}?data=${data}`);
            if (resposta.status === 401) {
                voltarAoLogin();
                return;
            }
            if (!resposta.ok) throw new Error();

            const tarefas = await resposta.json();
            for (const tarefa of tarefas) {
                tarefaEl.add(new Option(`${tarefa.materia} · ${tarefa.turno.toLowerCase()}`, tarefa.id));
            }
        } catch {
            tarefaEl.options[0].textContent = 'Estudo livre (tarefas indisponíveis)';
        }
    }

    // Cada sessão fica pendente até a API confirmar o registro.
    function registrarFoco(concluidoEm) {
        const sessao = {
            sessaoId: crypto.randomUUID(),
            tarefaId: tarefaEl.value || null,
            concluidoEm
        };
        pendentes.set(sessao.sessaoId, sessao);
        try {
            localStorage.setItem(prefixoSessoes + sessao.sessaoId, JSON.stringify(sessao));
        } catch {
            // A cópia em memória ainda permite tentar enviar nesta página.
        }
        enviarPendentes();
    }

    async function enviarPendentes() {
        if (enviando || pendentes.size === 0) return;
        enviando = true;
        reenviar.hidden = true;
        registroEl.textContent = 'Registrando tempo de foco…';

        for (const [id, sessao] of pendentes) {
            // A conta pode ter mudado em outra página da mesma aba.
            if (sessionStorage.getItem('guiavest.usuarioId') !== usuarioId) break;
            try {
                const resposta = await requisicaoAutenticada(API_POMODOROS, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(sessao)
                });
                if (resposta.status === 401) {
                    voltarAoLogin();
                    break;
                }
                if (!resposta.ok) continue;

                pendentes.delete(id);
                localStorage.removeItem(prefixoSessoes + id);
            } catch {
                // Um reenvio com o mesmo ID é seguro, inclusive após perda da resposta.
            }
        }

        enviando = false;
        reenviar.hidden = pendentes.size === 0;
        registroEl.textContent = pendentes.size === 0
            ? 'Tempo de foco registrado no progresso.'
            : 'Há tempo de foco pendente de registro.';
    }

    // Recupera os registros pendentes do usuário atual após reabrir a página.
    function restaurarPendentes() {
        try {
            for (const chave of Object.keys(localStorage)) {
                if (!chave.startsWith(prefixoSessoes)) continue;
                const sessao = JSON.parse(localStorage.getItem(chave));
                if (sessao && chave === prefixoSessoes + sessao.sessaoId)
                    pendentes.set(sessao.sessaoId, sessao);
            }
        } catch {
            // Continua com as sessões já carregadas quando o armazenamento falha.
        }
        enviarPendentes();
    }

    function renderizarTempo() {
        // Arredonda para cima para manter o último segundo visível até o fim da etapa.
        const segundos = Math.ceil(restante / 1000);
        const minutos = String(Math.floor(segundos / 60)).padStart(2, '0');
        const sobra = String(segundos % 60).padStart(2, '0');
        tempoEl.textContent = `${minutos}:${sobra}`;
    }

    function renderizarEtapa() {
        const rodada = fase === 'foco' ? focosConcluidos + 1 : focosConcluidos;
        // O CSS usa a fase do body para alternar a cor do fundo.
        document.body.dataset.fase = fase;
        faseEl.textContent = `${etapas[fase].nome} · rodada ${rodada} de 4`;
        iniciar.textContent = 'Iniciar';
        tarefaEl.disabled = false;
        renderizarTempo();
    }

    function pararContagem() {
        clearInterval(intervalo);
        termino = null;
    }

    function concluirEtapa() {
        // Guarda o término previsto, mesmo se o navegador atualizar o relógio com atraso.
        const concluidoEm = termino;
        pararContagem();

        // O quarto foco abre a pausa longa. Depois dela, começa uma nova série.
        if (fase === 'foco') {
            registrarFoco(concluidoEm);
            focosConcluidos++;
            fase = focosConcluidos === 4 ? 'pausa-longa' : 'pausa-curta';
        } else {
            if (fase === 'pausa-longa') focosConcluidos = 0;
            fase = 'foco';
        }

        // Prepara a próxima etapa para o usuário iniciar quando estiver pronto.
        restante = etapas[fase].minutos * 60_000;
        renderizarEtapa();
    }

    function atualizarRelogio() {
        if (termino === null) return;

        // O horário de término mantém a contagem correta quando a aba está em segundo plano.
        restante = Math.max(0, termino - Date.now());
        if (restante === 0) {
            concluirEtapa();
        } else {
            renderizarTempo();
        }
    }

    iniciar.addEventListener('click', () => {
        if (termino !== null) {
            atualizarRelogio();
            if (termino === null) return;

            pararContagem();
            iniciar.textContent = 'Continuar';
            return;
        }

        termino = Date.now() + restante;
        if (fase === 'foco') tarefaEl.disabled = true;
        iniciar.textContent = 'Pausar';
        intervalo = setInterval(atualizarRelogio, 250);
    });

    reiniciar.addEventListener('click', () => {
        pararContagem();
        // Reinicia o tempo da etapa atual e preserva as rodadas concluídas.
        restante = etapas[fase].minutos * 60_000;
        renderizarEtapa();
    });

    document.addEventListener('visibilitychange', () => {
        if (!document.hidden) atualizarRelogio();
    });

    reenviar.addEventListener('click', enviarPendentes);
    window.addEventListener('online', enviarPendentes);
    restaurarPendentes();
    carregarTarefas();
    renderizarEtapa();
})();