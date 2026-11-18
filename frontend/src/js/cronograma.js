(() => {
    const API = 'http://localhost:8080/api/cronogramas';
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

    function elemento(tag, classe, texto) {
        const no = document.createElement(tag);
        if (classe) no.className = classe;
        if (texto !== undefined) no.textContent = texto;
        return no;
    }

    function criarBloco(bloco) {
        const item = elemento('li', 'cronograma-bloco');
        item.classList.toggle('concluido', bloco.concluido === true);

        const status = bloco.concluido ? 'Concluido' : 'Pendente';
        item.append(
            elemento('strong', '', bloco.materia),
            elemento('small', '', `${bloco.duracaoMin} min · ${status}`)
        );

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

    carregarSemana();
})();