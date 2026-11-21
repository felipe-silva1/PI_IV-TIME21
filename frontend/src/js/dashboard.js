// Autor: Felipe Augusto dos Santos Silva RA: 25003353
(() =>{
    const API_TAREFAS = 'http://localhost:8080/api/tarefas';
    const TURNOS = {
        MANHA: 'Manhã',
        TARDE: 'Tarde',
        NOITE: 'Noite'
    };
    const API_CRONOGRAMAS = 'http://localhost:8080/api/cronogramas';
    const API_PROGRESSO = 'http://localhost:8080/api/progresso';
    const DIAS_SEMANA = ['Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb', 'Dom'];
    const ORDEM_TURNOS = ['MANHA', 'TARDE', 'NOITE'];

    const semanaDias = document.querySelector('#semana-dias');
    const semanaStatus = document.querySelector('#semana-status');
    const semanaRolagem = document.querySelector('#semana-rolagem');
    const semanaLegenda = document.querySelector('#semana-legenda');
    let consultaDaSemana;

    const lista = document.querySelector('#tarefas-lista');
    const status = document.querySelector('#tarefas-status');

    const progressoStatus = document.querySelector('#progresso-status');
    const progressoPercentual = document.querySelector('#progresso-percentual');
    const progressoBarra = document.querySelector('#progresso-barra');
    const progressoTarefas = document.querySelector('#progresso-tarefas');
    const progressoHoras = document.querySelector('#progresso-horas');
    let consultaDeProgresso;

    function dataLocalISO(data) {
        const mes = String(data.getMonth() + 1).padStart(2, '0');
        const dia = String(data.getDate()).padStart(2, '0');
        return `${data.getFullYear()}-${mes}-${dia}`;
    }

    function somarDias(data, quantidade) {
        const resultado = new Date(data);
        resultado.setDate(resultado.getDate() + quantidade);
        return resultado;
    }

    function formatarHoras(valor) {
        const numero = Number(valor ?? 0);
        return Number.isInteger(numero) ? `${numero}h` : `${numero.toFixed(1)}h`;
    }

    // Compartilha a gravação do status entre o checklist de hoje e a prévia semanal.
    function configurarCheckboxTarefa(tarefa, checkbox, atualizarVisual, erroAoSalvar) {
        let concluidaSalva = tarefa.concluido === true;
        checkbox.checked = concluidaSalva;

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
                // Restaura o último estado confirmado pela API quando a atualização falha.
                checkbox.checked = concluidaSalva;
                atualizarVisual();
                erroAoSalvar.textContent = erro instanceof TypeError
                    ? 'Não foi possível confirmar a alteração. Tente novamente.'
                    : erro.message;
                return;
            } finally {
                checkbox.disabled = false;
            }

            carregarTarefasDeHoje();
            carregarResumoDaSemana();
            carregarProgresso();
        });
    }

    // Monta um item do checklist de hoje com matéria, turno, status e tópicos.
    function criarItemTarefa(tarefa) {
        const item = document.createElement('li');
        item.className = 'painel-tarefa';

        const rotulo = document.createElement('label');
        rotulo.className = 'painel-tarefa-rotulo';

        const checkbox = document.createElement('input');
        checkbox.type = 'checkbox';
        checkbox.className = 'painel-tarefa-checkbox';
        configurarCheckboxTarefa(tarefa, checkbox, atualizarVIsual, erroAoSalvar);
        atualizarVisual();

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
            carregarProgresso();
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

    // Monta o cartão clicável da semana usando um checkbox que ocupa toda a área.
    function criarBlocoDaSemana(bloco, hojeISO) {
        const item = document.createElement('li');
        const rotulo = document.createElement('label');

        const checkbox = document.createElement('input');
        checkbox.type = 'checkbox';

        const detalhes = document.createElement('span');
        detalhes.className = 'painel-semana-detalhes';

        const erroAoSalvar = document.createElement('small');
        erroAoSalvar.setAttribute('role', 'status');

        const turno = TURNOS[bloco.turno] ?? bloco.turno;
        // Ao desmarcar um bloco, a data determina se ele volta a atrasado ou pendente.
        const estadoEmAberto = bloco.data < hojeISO ? 'atrasado' : 'pendente';
        const rotulos = {
            concluido: 'Concluído',
            atrasado: 'Atrasado',
            pendente: 'Pendente'
        };

        function atualizarVisual() {
            const estado = checkbox.checked ? 'concluido' : estadoEmAberto;
            item.className = `painel-semana-bloco ${estado}`;
            detalhes.textContent = ` — ${turno}, ${rotulos[estado]}`;
        }

        configurarCheckboxTarefa(bloco, checkbox, atualizarVisual, erroAoSalvar);
        atualizarVisual();

        rotulo.append(checkbox, ` ${bloco.materia}`, detalhes);
        item.append(rotulo, erroAoSalvar);
        return item;
    }

    function renderizarSemana(segunda, hojeISO, blocos) {
        let totalLinhas = 1;
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
            totalLinhas = Math.max(totalLinhas, blocosDoDia.length);
            lista.append(...blocosDoDia.map(bloco => criarBlocoDaSemana(bloco, hojeISO)));
            dia.append(titulo, lista);

            return dia;
        });

        semanaDias.style.setProperty('--linhas-semana', totalLinhas);
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

    function renderizarProgresso(dados) {
        const concluidas = Number(dados?.tarefasConcluidas ?? 0);
        const total = Number(dados?.totalTarefas ?? dados?.tarefasTotais ?? 0);
        const percentualBruto = dados?.percentualTarefasConcluidas
            ?? dados?.percentualConclusao
            ?? (total > 0 ? (concluidas / total) * 100 : 0);

        const percentual = Math.max(0, Math.min(100, Math.round(Number(percentualBruto) || 0)));

        const horasCumpridas = Number(dados?.horasCumpridas ?? 0);
        const horasPlanejadas = Number(dados?.horasPlanejadas ?? 0);
        const percentualHoras = Math.max(
            0,
            Math.min(100, Math.round(Number(dados?.percentualHorasCumpridas ?? 0) || 0))
        );
        if (progressoPercentual) {
            progressoPercentual.textContent = `${percentual}%`;
        }

        if (progressoBarra) {
            progressoBarra.max = 100;
            progressoBarra.value = percentual;
            progressoBarra.textContent = `${percentual}%`;
        }

        if (progressoTarefas) {
            progressoTarefas.textContent = `${concluidas} de ${total} tarefas concluídas`;
        }

        if (progressoHoras) {
            progressoHoras.textContent =
                `Horas: ${formatarHoras(horasCumpridas)} de ${formatarHoras(horasPlanejadas)} (${percentualHoras}%)`;
        }
    }

    async function carregarProgresso() {
        if (!progressoStatus) return;

        consultaDeProgresso?.abort();
        consultaDeProgresso = new AbortController();
        const { signal } = consultaDeProgresso;

        const hoje = new Date();
        const segunda = somarDias(hoje, -((hoje.getDay() + 6) % 7));
        const parametros = new URLSearchParams({ semana: dataLocalISO(segunda) });

        progressoStatus.textContent = 'Carregando progresso…';

        try {
            const resposta = await requisicaoAutenticada(
                `${API_PROGRESSO}?${parametros}`,
                { signal }
            );

            if (signal.aborted) return;

            if (resposta.status === 401) {
                limparSessao();
                location.replace('../login.html');
                return;
            }

            if (!resposta.ok) {
                const mensagem = await mensagemDeErro(resposta).catch(
                    () => 'Não foi possível carregar o progresso semanal.'
                );
                throw new Error(mensagem);
            }

            const dados = await resposta.json();
            if (signal.aborted) return;

            renderizarProgresso(dados);
            progressoStatus.textContent = '';
        } catch (erro) {
            if (signal.aborted) return;
            progressoStatus.textContent = erro instanceof TypeError
                ? 'Não foi possível conectar à API.'
                : erro.message;
        }
    }

    window.addEventListener('pageshow', (evento) => {
        if(evento.persisted) {
            carregarTarefasDeHoje();
            carregarResumoDaSemana();
            carregarProgresso();
        }
    });

    carregarTarefasDeHoje();
    carregarResumoDaSemana();
    carregarProgresso();
})();
