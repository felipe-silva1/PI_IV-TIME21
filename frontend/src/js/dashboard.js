// Autor: Felipe Augusto dos Santos Silva RA: 25003353
(() =>{
    const API_TAREFAS = 'http://localhost:8080/api/tarefas';
    const TURNOS = {
        MANHA: 'Manhã',
        TARDE: 'Tarde',
        NOITE: 'Noite'
    };
    const API_CRONOGRAMAS = 'http://localhost:8080/api/cronogramas';
    const DIAS_SEMANA = ['Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb', 'Dom'];
    const ORDEM_TURNOS = ['MANHA', 'TARDE', 'NOITE'];

    const semanaDias = document.querySelector('#semana-dias');
    const semanaStatus = document.querySelector('#semana-status');
    const semanaRolagem = document.querySelector('#semana-rolagem');
    const semanaLegenda = document.querySelector('#semana-legenda');
    let consultaDaSemana;

    const lista = document.querySelector('#tarefas-lista');
    const status = document.querySelector('#tarefas-status');

    function dataLocalISO(data) {
        const mes = String(data.getMonth() + 1).padStart(2, '0');
        const dia = String(data.getDate()).padStart(2, '0');
        return `${data.getFullYear()}-${mes}-${dia}`;
    }

    function criarItemTarefa(tarefa) {
        const item = document.createElement('li');
        item.className = 'painel-tarefa';

        const rotulo = document.createElement('label');
        rotulo.className = 'painel-tarefa-rotulo';

        const checkbox = document.createElement('input');
        checkbox.type = 'checkbox';
        checkbox.className = 'painel-tarefa-checkbox';
        let concluidaSalva = tarefa.concluido === true;
        checkbox.checked = concluidaSalva;

        const detalhes = document.createElement('span');
        detalhes.className = 'painel-tarefa-detalhes';

        const materia = document.createElement('strong');
        materia.className = 'painel-tarefa-materia';
        materia.textContent = tarefa.materia;

        const turno = TURNOS[tarefa.turno] ?? tarefa.turno;
        const estado = document.createElement('small');

        function atualizarVisual() {
            item.classList.toggle('concluida', checkbox.checked);
            estado.textContent = `${turno} · ${checkbox.checked ? 'Concluída' : 'Pendente'}`;
        }

        const erroAoSalvar = document.createElement('small');
        erroAoSalvar.setAttribute('role', 'status');

        checkbox.addEventListener('change', async () => {
            const concluido = checkbox.checked;
            checkbox.disabled = true;
            erroAoSalvar.textContent = '';
            atualizarVisual();

            try {
                const tarefaId = encodeURIComponent(tarefa.id);
                const resposta = await requisicaoAutenticada(
                    `${API_TAREFAS}/${tarefaId}/status`,
                    {
                        method: 'PATCH',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ concluido })
                    }
                );

                if (resposta.status === 401) {
                    limparSessao();
                    location.replace('../login.html');
                    return;
                }

                if (!resposta.ok) {
                    const mensagem = await mensagemDeErro(resposta).catch(
                        () => 'Não foi possível salvar o estado da tarefa.'
                    );
                    throw new Error(mensagem);
                }

                const tarefaAtualizada = await resposta.json();
                concluidaSalva = tarefaAtualizada.concluido === true;
                checkbox.checked = concluidaSalva;
                atualizarVisual();
            } catch (erro) {
                checkbox.checked = concluidaSalva;
                atualizarVisual();
                erroAoSalvar.textContent = erro instanceof TypeError
                    ? 'Não foi possível confirmar a alteração. Tente novamente.'
                    : erro.message;
                return;
            } finally {
                checkbox.disabled = false;
            }

            carregarResumoDaSemana();
        });

        atualizarVisual();

        detalhes.append(materia, estado);
        rotulo.append(checkbox, detalhes);
        item.append(rotulo, erroAoSalvar);

        const topicos = tarefa.topicos ?? [];
        if (topicos.length > 0) {
            const listaTopicos = document.createElement('ul');
            listaTopicos.className = 'painel-tarefa-topicos';

            for (const topico of topicos) {
                const itemTopico = document.createElement('li');
                itemTopico.textContent = topico.nome;
                listaTopicos.append(itemTopico);
            }

            item.append(listaTopicos);
        }

        return item;
    }

    async function carregarTarefasDeHoje() {
        status.textContent = 'Carregando tarefas...';

        const parametros = new URLSearchParams({
            data: dataLocalISO(new Date())
        });

        try {
            const resposta = await requisicaoAutenticada(
                `${API_TAREFAS}?${parametros}`
            );

            if(resposta.status === 401) {
                limparSessao();
                location.replace('../login.html');
                return;
            }

            if(!resposta.ok) {
                const mensagem = await mensagemDeErro(resposta).catch(
                    () => 'Não foi possível carregar as tarefas de hoje.'
                );
                throw new Error(mensagem);
            }

            const tarefas = await resposta.json();
            lista.replaceChildren(...tarefas.map(criarItemTarefa));
            status.textContent = tarefas.length === 0
                ? 'Você não tem tarefas planejadas para hoje.'
                : '';
        } catch (erro) {
            status.textContent = erro instanceof TypeError
                ? 'Não foi possível conectar à API.'
                : erro.message;
        }
    }

    function somarDias(data, quantidade) {
        const resultado = new Date(data);
        resultado.setDate(resultado.getDate() + quantidade);
        return resultado;
    }

    function criarBlocoDaSemana(bloco, hojeISO) {
        let estado = 'pendente';

        if (bloco.concluido === true) {
            estado = 'concluido';
        } else if (bloco.data < hojeISO) {
            estado = 'atrasado';
        }

        const rotulos = {
            concluido: 'Concluído',
            atrasado: 'Atrasado',
            pendente: 'Pendente'
        };

        const item = document.createElement('li');
        item.className = `painel-semana-bloco ${estado}`;
        item.append(bloco.materia);

        const detalhes = document.createElement('span');
        detalhes.className = 'painel-semana-detalhes';
        detalhes.textContent = ` — ${TURNOS[bloco.turno] ?? bloco.turno}, ${rotulos[estado]}`;
        item.append(detalhes);

        return item;
    }

    function renderizarSemana(segunda, hojeISO, blocos) {
        const dias = DIAS_SEMANA.map((nome, indice) => {
            const data = dataLocalISO(somarDias(segunda, indice));

            const dia = document.createElement('div');
            dia.className = 'painel-semana-dia';

            const titulo = document.createElement('h3');
            const rotulo = document.createElement('time');
            rotulo.dateTime = data;
            rotulo.textContent = nome;

            if (data === hojeISO) {
                rotulo.setAttribute('aria-current', 'date');
            }

            titulo.append(rotulo);

            const lista = document.createElement('ul');
            const blocosDoDia = blocos
                .filter(bloco => bloco.data === data)
                .sort((a, b) => ORDEM_TURNOS.indexOf(a.turno) - ORDEM_TURNOS.indexOf(b.turno));

            lista.append(...blocosDoDia.map(bloco => criarBlocoDaSemana(bloco, hojeISO)));
            dia.append(titulo, lista);

            return dia;
        });

        semanaDias.replaceChildren(...dias);
        semanaRolagem.hidden = false;
        semanaLegenda.hidden = false;
    }

    async function carregarResumoDaSemana() {
        consultaDaSemana?.abort();
        consultaDaSemana = new AbortController();
        const { signal } = consultaDaSemana;
        const hoje = new Date();
        const hojeISO = dataLocalISO(hoje);
        const segunda = somarDias(hoje, -((hoje.getDay() + 6) % 7));
        const parametros = new URLSearchParams({ semana: dataLocalISO(segunda) });

        semanaStatus.textContent = 'Carregando cronograma…';

        try {
            const resposta = await requisicaoAutenticada(
                `${API_CRONOGRAMAS}?${parametros}`,
                { signal }
            );

            if (signal.aborted) return;

            if (resposta.status === 401) {
                limparSessao();
                location.replace('../login.html');
                return;
            }

            if (resposta.status === 404) {
                semanaStatus.textContent = 'O cronograma desta semana ainda não foi gerado.';
                return;
            }

            if (!resposta.ok) {
                const mensagem = await mensagemDeErro(resposta).catch(
                    () => 'Não foi possível carregar o cronograma da semana.'
                );
                throw new Error(mensagem);
            }

            const cronograma = await resposta.json();
            if (signal.aborted) return;
            renderizarSemana(segunda, hojeISO, cronograma.blocos ?? []);
            semanaStatus.textContent = '';
        } catch (erro) {
            if (signal.aborted) return;
            semanaStatus.textContent = erro instanceof TypeError
                ? 'Não foi possível conectar à API.'
                : erro.message;
        }
    }

    window.addEventListener('pageshow', (evento) => {
        if(evento.persisted) {
            carregarTarefasDeHoje();
            carregarResumoDaSemana();
        }
    });

    carregarTarefasDeHoje();
    carregarResumoDaSemana();
})();
