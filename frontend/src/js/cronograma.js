/*Autor: Felipe Augusto dos Santos Silva RA: 25003353*/
(() => {
    const API = 'http://localhost:8080/api/cronogramas';
    const API_TAREFAS = 'http://localhost:8080/api/tarefas';
    const TURNOS = [
        ['MANHA', 'Manhã'],
        ['TARDE', 'Tarde'],
        ['NOITE', 'Noite']
    ];

    const anterior = document.querySelector('#semana-anterior');
    const proxima = document.querySelector('#semana-proxima');
    const gerar = document.querySelector('#gerar-cronograma');
    const recarregar = document.querySelector('#recarregar-cronograma');
    const periodo = document.querySelector('#cronograma-periodo');
    const mensagem = document.querySelector('#cronograma-mensagem');
    const grade = document.querySelector('#cronograma-semana');
    const dialogoEdicao = document.querySelector('#dialogo-editar-tarefa');
    const formEdicao = document.querySelector('#form-editar-tarefa');
    const editarId = document.querySelector('#editar-tarefa-id');
    const editarData = document.querySelector('#editar-tarefa-data');
    const editarTurno = document.querySelector('#editar-tarefa-turno');
    const editarMateria = document.querySelector('#editar-tarefa-materia');
    const editarDuracao = document.querySelector('#editar-tarefa-duracao');
    const editarDescricao = document.querySelector('#editar-tarefa-descricao');
    const editarConcluido = document.querySelector('#editar-tarefa-concluido');
    const editarMensagem = document.querySelector('#editar-tarefa-mensagem');
    const cancelarEdicao = document.querySelector('#cancelar-edicao-tarefa');

    const formatoPeriodo = new Intl.DateTimeFormat('pt-BR', {
        day: 'numeric',
        month: 'short',
        year: 'numeric'
    });

    const formatoDia = new Intl.DateTimeFormat('pt-BR', {
        weekday: 'short',
        day: '2-digit',
        month: '2-digit'
    });

    function somarDias(data,quantidade) {
        const resultado = new Date(data);
        resultado.setDate(resultado.getDate() + quantidade);
        return resultado;
    }

    function dataISO(data) {
        const mes = String(data.getMonth() + 1).padStart(2, '0');
        const dia = String(data.getDate()).padStart(2, '0');
        return `${data.getFullYear()}-${mes}-${dia}`;
    }

    const hoje = new Date();
    let segunda = somarDias(hoje, -((hoje.getDay() + 6) %7));
    let salvamentosPendentes = 0;

    function elemento(tag, classe, texto) {
        const no = document.createElement(tag);
        if (classe) no.className = classe;
        if (texto !== undefined) no.textContent = texto;
        return no;
    }

    async function atualizarTarefa(id, dados) {
        const resposta = await requisicaoAutenticada(
            `${API_TAREFAS}/${encodeURIComponent(id)}`,
            {
                method: 'PATCH',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(dados)
            }
        );

        if (resposta.status === 401) {
            limparSessao();
            location.replace('../login.html');
            throw new Error('Sessão expirada.');
        }

        if (!resposta.ok) {
            const erro = await mensagemDeErro(resposta).catch(
                () => 'Não foi possível atualizar a tarefa.'
            );
            throw new Error(erro);
        }

        return resposta.json();
    }

    function abrirEdicao(bloco) {
        editarId.value = bloco.id;
        editarData.value = bloco.data;
        editarTurno.value = bloco.turno;
        editarMateria.value = bloco.materia;
        editarDuracao.value = bloco.duracaoMin;
        editarDescricao.value = bloco.descricao ?? '';
        editarConcluido.checked = bloco.concluido === true;
        editarMensagem.textContent = '';
        dialogoEdicao.showModal();
    }

    function criarBloco(bloco) {
        const item = elemento('li', 'cronograma-bloco cronograma-bloco-arrastavel');
        item.draggable = true;
        item.dataset.tarefaId = bloco.id;
        const rotulo = elemento('label', 'cronograma-bloco-rotulo');
        const checkbox = elemento('input', 'cronograma-bloco-checkbox');
        const detalhes = elemento('span', 'cronograma-bloco-detalhes');
        const materia = elemento('strong', '', bloco.materia);
        const estado = elemento('small');
        const aviso = elemento('small', 'cronograma-bloco-aviso');
        const descricao = elemento('small', 'cronograma-bloco-descricao',
            bloco.descricao ? bloco.descricao : 'Sem descrição');
        const acoes = elemento('div', 'cronograma-bloco-acoes');
        const editar = elemento('button', 'cronograma-bloco-editar', 'Editar');

        editar.type = 'button';
        editar.setAttribute('aria-label', `Editar ${bloco.materia}`);
        editar.addEventListener('click', () => abrirEdicao(bloco));

        checkbox.type = 'checkbox';
        aviso.setAttribute('role', 'status');

        let concluidoSalvo = bloco.concluido === true;

        function atualizarVisual() {
            const atrasado = !concluidoSalvo && bloco.data < dataISO(new Date());
            const status = concluidoSalvo ? 'Concluído' : atrasado ? 'Atrasado' : 'Pendente';

            checkbox.checked = concluidoSalvo;
            item.classList.toggle('concluido', concluidoSalvo);
            item.classList.toggle('atrasado', atrasado);
            estado.textContent = `${bloco.duracaoMin} min · ${status}`;
        }

        checkbox.addEventListener('change', async () => {
            const concluido = checkbox.checked;
            checkbox.disabled = true;
            aviso.textContent = 'Salvando…';

            salvamentosPendentes++;
            anterior.disabled = true;
            proxima.disabled = true;

            try {
                const id = encodeURIComponent(bloco.id);
                const resposta = await requisicaoAutenticada(
                    `${API_TAREFAS}/${id}/status`,
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
                    const erro = await mensagemDeErro(resposta).catch(
                        () => 'Não foi possível salvar o estado do bloco.'
                    );
                    throw new Error(erro);
                }

                const blocoAtualizado = await resposta.json();
                concluidoSalvo = blocoAtualizado.concluido === true;
                aviso.textContent = '';
            } catch (erro) {
                aviso.textContent = erro instanceof TypeError
                    ? 'Não foi possível confirmar a alteração. Tente novamente.'
                    : erro.message;
            } finally {
                atualizarVisual();
                checkbox.disabled = false;

                salvamentosPendentes--;
                anterior.disabled = salvamentosPendentes > 0;
                proxima.disabled = salvamentosPendentes > 0;
            }
        });

        detalhes.append(materia, estado);
        rotulo.append(checkbox, detalhes);
        acoes.append(editar);
        item.append(rotulo, descricao, acoes, aviso);

        item.addEventListener('dragstart', evento => {
            evento.dataTransfer.effectAllowed = 'move';
            evento.dataTransfer.setData('text/plain', bloco.id);
            item.classList.add('arrastando');
        });

        item.addEventListener('dragend', () => {
            item.classList.remove('arrastando');
            grade.querySelectorAll('.drag-sobre')
                .forEach(no => no.classList.remove('drag-sobre'));
        });

        atualizarVisual();
        return item;
    }

    function mostrarSemana(blocos) {
        const dias = document.createDocumentFragment();
        const dataDeHoje = dataISO(new Date());

        for (let indice = 0; indice < 7; indice++) {
            const data = somarDias(segunda, indice);
            const chave = dataISO(data);
            const dia = elemento('article', 'cronograma-dia');
            const titulo = elemento('h3');
            const rotulo = elemento('time', '', formatoDia.format(data));

            rotulo.dateTime = chave;
            if (chave === dataDeHoje) rotulo.setAttribute('aria-current', 'date');

            titulo.append(rotulo);
            dia.append(titulo);

            for (const [codigo, nome] of TURNOS) {
                const turno = elemento('section', 'cronograma-turno');
                turno.dataset.data = chave;
                turno.dataset.turno = codigo;
                turno.append(elemento('h4', '', nome));

                const blocosDoTurno = blocos.filter(
                    bloco => bloco.data === chave && bloco.turno === codigo
                );

                if (blocosDoTurno.length === 0) {
                    turno.append(elemento('small', '', 'Sem estudo previsto'));
                } else {
                    const lista = elemento('ul', 'cronograma-blocos');
                    lista.append(...blocosDoTurno.map(criarBloco));
                    turno.append(lista);
                }

                dia.append(turno);
            }

            dias.append(dia);
        }

        grade.replaceChildren(dias);
        grade.hidden = false;

        grade.querySelectorAll('.cronograma-turno').forEach(turno => {
            turno.addEventListener('dragenter', evento => {
                evento.preventDefault();
                turno.classList.add('drag-sobre');
            });

            turno.addEventListener('dragover', evento => {
                evento.preventDefault();
                evento.dataTransfer.dropEffect = 'move';
            });

            turno.addEventListener('dragleave', evento => {
                if (!turno.contains(evento.relatedTarget))
                    turno.classList.remove('drag-sobre');
            });

            turno.addEventListener('drop', async evento => {
                evento.preventDefault();
                turno.classList.remove('drag-sobre');

                const tarefaId = evento.dataTransfer.getData('text/plain');
                const bloco = blocos.find(item => item.id === tarefaId);
                if (!bloco || (
                    bloco.data === turno.dataset.data &&
                    bloco.turno === turno.dataset.turno
                )) return;

                try {
                    const atualizado = await atualizarTarefa(tarefaId, {
                        data: turno.dataset.data,
                        turno: turno.dataset.turno,
                        materia: bloco.materia,
                        duracaoMin: bloco.duracaoMin,
                        descricao: bloco.descricao ?? '',
                        concluido: bloco.concluido === true
                    });

                    const indice = blocos.findIndex(item => item.id === tarefaId);
                    if (indice >= 0) blocos[indice] = atualizado;
                    mostrarSemana(blocos);
                } catch (erro) {
                    mensagem.textContent = erro.message;
                }
            });
        });
    }

    async function carregarSemana(metodo = 'GET') {
        const gerando = metodo === 'POST';
        const rota = gerando ? `${API}/gerar` : API;
        const parametros = new URLSearchParams({ semana: dataISO(segunda) });

        periodo.textContent = formatoPeriodo.formatRange(segunda, somarDias(segunda, 6));
        mensagem.textContent = gerando ? 'Gerando cronograma…' : 'Carregando semana…';

        grade.hidden = true;
        gerar.hidden = true;
        recarregar.hidden = true;
        anterior.disabled = true;
        proxima.disabled = true;

        try {
            const resposta = await requisicaoAutenticada(`${rota}?${parametros}`, {
                method: metodo
            });

            if (resposta.status === 401) {
                limparSessao();
                location.replace('../login.html');
                return;
            }

            if (!gerando && resposta.status === 404) {
                mensagem.textContent = 'Esta semana ainda não tem cronograma.';
                gerar.hidden = false;
                return;
            }

            if (!resposta.ok) {
                const erro = await mensagemDeErro(resposta).catch(
                    () => 'Não foi possível consultar o cronograma.'
                );
                throw new Error(erro);
            }

            const cronograma = await resposta.json();
            mostrarSemana(cronograma.blocos ?? []);
            mensagem.textContent = gerando ? 'Cronograma gerado.' : '';
        } catch (erro) {
            mensagem.textContent = erro instanceof TypeError
                ? 'Não foi possível conectar à API. Tente novamente.'
                : erro.message;
            recarregar.hidden = false;
        } finally {
            anterior.disabled = false;
            proxima.disabled = false;
        }
    }

    anterior.addEventListener('click', () => {
        segunda = somarDias(segunda, -7);
        carregarSemana();
    });

    proxima.addEventListener('click', () => {
        segunda = somarDias(segunda, 7);
        carregarSemana();
    });

    gerar.addEventListener('click', () => carregarSemana('POST'));
    recarregar.addEventListener('click', () => carregarSemana());
    cancelarEdicao.addEventListener('click', () => dialogoEdicao.close());

    formEdicao.addEventListener('submit', async evento => {
        evento.preventDefault();
        const botaoSalvar = formEdicao.querySelector('[type="submit"]');
        botaoSalvar.disabled = true;
        editarMensagem.textContent = 'Salvando…';

        try {
            await atualizarTarefa(editarId.value, {
                data: editarData.value,
                turno: editarTurno.value,
                materia: editarMateria.value,
                duracaoMin: Number(editarDuracao.value),
                descricao: editarDescricao.value,
                concluido: editarConcluido.checked
            });

            dialogoEdicao.close();
            await carregarSemana();
        } catch (erro) {
            editarMensagem.textContent = erro.message;
        } finally {
            botaoSalvar.disabled = false;
        }
    });

    carregarSemana();
})();
