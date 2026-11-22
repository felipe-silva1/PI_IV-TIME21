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
    const API_ROTINAS = 'http://localhost:8080/api/rotinas';
    const DIAS_SEMANA = ['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb'];
    const dataCurta = new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: '2-digit' });
    const ORDEM_TURNOS = ['MANHA', 'TARDE', 'NOITE'];

    const semanaDias = document.querySelector('#semana-dias');
    const semanaStatus = document.querySelector('#semana-status');
    const semanaRolagem = document.querySelector('#semana-rolagem');
    const semanaLegenda = document.querySelector('#semana-legenda');
    const proximoEstudo = document.querySelector('#proximo-estudo');
    const proximoEstudoDetalhes = document.querySelector('#proximo-estudo-detalhes');
    let consultaDaSemana;

    const lista = document.querySelector('#tarefas-lista');
    const status = document.querySelector('#tarefas-status');

    const progressoStatus = document.querySelector('#progresso-status');
    const progressoPeriodo = document.querySelector('#progresso-periodo');
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

    function inicioDaSemana(data) {
        return somarDias(data, -((data.getDay() + 6) % 7));
    }

    async function buscarCriacaoDaRotina(signal) {
        const resposta = await requisicaoAutenticada(API_ROTINAS, { signal });
        if (!resposta.ok) return null;

        const rotina = await resposta.json();
        return rotina.criadoEm > 0 ? new Date(rotina.criadoEm) : null;
    }

    function formatarHoras(valor) {
        const numero = Number(valor ?? 0);
        return Number.isInteger(numero) ? `${numero}h` : `${numero.toFixed(1)}h`;
    }

    // Compartilha a gravação do status entre o checklist de hoje e a prévia do cronograma.
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

        configurarCheckboxTarefa(tarefa, checkbox, atualizarVisual, erroAoSalvar);
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

    // Monta o cartão clicável usando um checkbox que ocupa toda a área.
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
        const atrasado = bloco.data < hojeISO;

        function atualizarVisual() {
            const estado = checkbox.checked ? 'concluido' : atrasado ? 'atrasado' : 'pendente';
            const descricao = checkbox.checked ? 'Concluído' : atrasado ? 'Atrasado' : 'Pendente';
            item.className = `painel-semana-bloco ${estado}`;
            detalhes.textContent = ` — ${turno}, ${descricao}`;
        }

        configurarCheckboxTarefa(bloco, checkbox, atualizarVisual, erroAoSalvar);
        atualizarVisual();

        rotulo.append(checkbox, ` ${bloco.materia}`, detalhes);
        item.append(rotulo, erroAoSalvar);
        return item;
    }

    function renderizarSemana(inicio, hojeISO, blocos) {
        let totalLinhas = 1;
        const dias = Array.from({ length: 7 }, (_, indice) => {
            const diaDaPrevia = somarDias(inicio, indice);
            const data = dataLocalISO(diaDaPrevia);

            const dia = document.createElement('div');
            dia.className = 'painel-semana-dia';

            const titulo = document.createElement('h3');
            const rotulo = document.createElement('time');
            rotulo.dateTime = data;
            rotulo.textContent = `${DIAS_SEMANA[diaDaPrevia.getDay()]} ${diaDaPrevia.getDate()}`;
            rotulo.title = dataCurta.format(diaDaPrevia);

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

    async function buscarBlocosDaSemana(segunda, signal) {
        const parametros = new URLSearchParams({ semana: dataLocalISO(segunda) });
        const resposta = await requisicaoAutenticada(`${API_CRONOGRAMAS}?${parametros}`, { signal });

        if (signal.aborted) return null;
        if (resposta.status === 401) {
            limparSessao();
            location.replace('../login.html');
            return null;
        }
        if (resposta.status === 404) return [];
        if (!resposta.ok) {
            const mensagem = await mensagemDeErro(resposta).catch(
                () => 'Não foi possível carregar o cronograma da semana.'
            );
            throw new Error(mensagem);
        }

        const cronograma = await resposta.json();
        return cronograma.blocos ?? [];
    }

    function encontrarProximoEstudo(blocos, hojeISO) {
        return blocos.filter(bloco => !bloco.concluido && bloco.data >= hojeISO)
            .sort((a, b) => a.data.localeCompare(b.data)
                || ORDEM_TURNOS.indexOf(a.turno) - ORDEM_TURNOS.indexOf(b.turno))[0];
    }

    function renderizarProximoEstudo(proximo) {
        proximoEstudo.hidden = !proximo;
        if (proximo) {
            const [, mes, dia] = proximo.data.split('-');
            const turno = TURNOS[proximo.turno] ?? proximo.turno;
            proximoEstudoDetalhes.textContent = `${dia}/${mes} · ${proximo.materia} · ${turno}`;
        }
    }

    async function carregarResumoDaSemana() {
        consultaDaSemana?.abort();
        consultaDaSemana = new AbortController();
        const { signal } = consultaDaSemana;
        const hoje = new Date();
        const hojeISO = dataLocalISO(hoje);
        const segunda = inicioDaSemana(hoje);
        const segundaISO = dataLocalISO(segunda);

        semanaStatus.textContent = 'Carregando cronograma…';

        try {
            const [criacao, blocosDaSemana] = await Promise.all([
                buscarCriacaoDaRotina(signal).catch(() => null),
                buscarBlocosDaSemana(segunda, signal)
            ]);
            if (signal.aborted || blocosDaSemana === null) return;

            // A primeira prévia fica ancorada na criação até a próxima segunda-feira.
            const criacaoISO = criacao && dataLocalISO(criacao);
            const primeiraPrevia = criacaoISO > segundaISO && criacaoISO <= hojeISO;
            const inicio = primeiraPrevia ? criacao : segunda;
            const inicioISO = dataLocalISO(inicio);
            const fimISO = dataLocalISO(somarDias(inicio, 6));

            let blocos = blocosDaSemana;
            let falhaNaPrevia = false;
            if (primeiraPrevia || !encontrarProximoEstudo(blocosDaSemana, hojeISO)) {
                const blocosSeguintes = await buscarBlocosDaSemana(somarDias(segunda, 7), signal)
                    .catch(() => {
                        falhaNaPrevia = primeiraPrevia;
                        return [];
                    });
                if (signal.aborted || blocosSeguintes === null) return;
                blocos = blocos.concat(blocosSeguintes);
            }

            const blocosDaPrevia = blocos.filter(bloco => bloco.data >= inicioISO && bloco.data <= fimISO);
            if (blocosDaPrevia.length === 0) {
                semanaRolagem.hidden = true;
                semanaLegenda.hidden = true;
            } else {
                renderizarSemana(inicio, hojeISO, blocosDaPrevia);
            }
            semanaStatus.textContent = blocosDaPrevia.length > 0
                ? ''
                : 'Nenhum estudo previsto neste período.';
            if (falhaNaPrevia) {
                semanaStatus.textContent = 'Não foi possível carregar parte do cronograma.';
            }

            renderizarProximoEstudo(encontrarProximoEstudo(blocos, hojeISO));
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

        const segunda = inicioDaSemana(new Date());
        const parametros = new URLSearchParams({ semana: dataLocalISO(segunda) });
        progressoPeriodo.textContent =
            `(${dataCurta.format(segunda)} a ${dataCurta.format(somarDias(segunda, 6))})`;

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

    function atualizarNaViradaDoDia() {
        const agora = new Date();
        const amanha = somarDias(agora, 1);
        amanha.setHours(0, 0, 0, 0);

        setTimeout(() => {
            carregarTarefasDeHoje();
            carregarResumoDaSemana();
            carregarProgresso();
            atualizarNaViradaDoDia();
        }, amanha - agora);
    }

    carregarTarefasDeHoje();
    carregarResumoDaSemana();
    carregarProgresso();
    atualizarNaViradaDoDia();
})();
