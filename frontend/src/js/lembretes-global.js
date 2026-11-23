(() => {
    const API_TAREFAS = 'http://localhost:8080/api/tarefas';
    const TOKEN_KEY = 'guiavest.sessionToken';
    const USER_ID_KEY = 'guiavest.usuarioId';
    const HORARIOS = {
        MANHA: 5,
        TARDE: 14,
        NOITE: 18
    };

    const usuarioId = sessionStorage.getItem(USER_ID_KEY);
    if (!sessionStorage.getItem(TOKEN_KEY) || !usuarioId) return;

    const preferenciaKey = `guiavest.lembretes.siteAtivo.${usuarioId}`;
    if (localStorage.getItem(preferenciaKey) === null)
        localStorage.setItem(preferenciaKey, 'false');

    function dataISO(data) {
        const mes = String(data.getMonth() + 1).padStart(2, '0');
        const dia = String(data.getDate()).padStart(2, '0');
        return `${data.getFullYear()}-${mes}-${dia}`;
    }

    async function verificarLembretes() {
        if (localStorage.getItem(preferenciaKey) !== 'true' ||
            !('Notification' in window) ||
            Notification.permission !== 'granted' ||
            (!window.isSecureContext &&
                location.hostname !== 'localhost' &&
                location.hostname !== '127.0.0.1'))
            return;

        const agora = new Date();
        if (agora.getMinutes() > 4) return;
        const turno = Object.keys(HORARIOS).find(
            codigo => HORARIOS[codigo] === agora.getHours()
        );
        if (!turno) return;

        const data = dataISO(agora);
        const parametros = new URLSearchParams({ data });

        try {
            const resposta = await requisicaoAutenticada(`${API_TAREFAS}?${parametros}`);
            if (!resposta.ok) return;

            const tarefas = await resposta.json();
            for (const tarefa of tarefas) {
                if (tarefa.turno !== turno || tarefa.concluido === true) continue;

                const identificador = tarefa.id || `${tarefa.materia}-${tarefa.turno}`;
                const enviadoKey =
                    `guiavest.lembrete.enviado.${usuarioId}.${data}.${identificador}`;
                if (localStorage.getItem(enviadoKey)) continue;

                localStorage.setItem(enviadoKey, 'true');
                try {
                    const notificacao = new Notification('Hora de estudar', {
                        body: `${tarefa.materia} · ${tarefa.duracaoMin} min`,
                        tag: enviadoKey
                    });
                    notificacao.addEventListener('click', () => {
                        window.focus();
                        location.href = 'cronograma.html';
                    }, { once: true });
                } catch (erro) {
                    localStorage.removeItem(enviadoKey);
                    console.warn('Não foi possível exibir o lembrete do bloco.', erro);
                }
            }
        } catch (erro) {
            console.warn('Não foi possível consultar os blocos para os lembretes.', erro);
        }
    }

    verificarLembretes();
    window.setInterval(verificarLembretes, 15000);
    window.addEventListener('focus', verificarLembretes);
})();
