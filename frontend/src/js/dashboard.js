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
    const semanaLegenda = document.querySelector('#semana-legenda')


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

        const materia = document.createElement('strong');
        materia.textContent = tarefa.materia;

        const turno = document.createElement('small');
        turno.textContent = TURNOS[tarefa.turno] ?? tarefa.turno;

        item.append(materia, turno);

        const topicos = tarefa.topicos ?? [];
        if(topicos.length > 0) {
            const listaTopicos = document.createElement('ul');
            listaTopicos.className = 'painel-tarefa-topicos';

            for (const topicos of topicos) {
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
                    () => 'Não foi possível carregas as tarefas de hoje.'
                );
                throw new Error(mensagem);
            }

            const tarefas = await resposta.json();
            lista.replaceChildren(...tarefas.map(criarItemTarefa));
            status.textContent = '';
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
        const hoje = new Date();
        const hojeISO = dataLocalISO(hoje);
        const segunda = somarDias(hoje, -((hoje.getDay() + 6) % 7));
        const parametros = new URLSearchParams({ semana: dataLocalISO(segunda) });

        semanaStatus.textContent = 'Carregando cronograma…';

        try {
            const resposta = await requisicaoAutenticada(`${API_CRONOGRAMAS}?${parametros}`);

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
            renderizarSemana(segunda, hojeISO, cronograma.blocos ?? []);
            semanaStatus.textContent = '';
        } catch (erro) {
            semanaStatus.textContent = erro instanceof TypeError
                ? 'Não foi possível conectar à API.'
                : erro.message;
        }
    }

    carregarTarefasDeHoje();
    carregarResumoDaSemana();
})();