const ROTINA_API_URL = 'http://localhost:8080/api/rotinas';
const CHAVE_EXAME = 'exameSelecionado';

const CATALOGO_MATERIAS = {
    ENEM: [
        'Matemática', 'Redação', 'Português e Literatura', 'Física',
        'Química', 'Biologia', 'História', 'Geografia', 'Filosofia e Sociologia', 'Inglês / Espanhol'
    ],
    FUVEST: [
        'Português e Obras Obrigatórias', 'Redação', 'Matemática', 'Física',
        'Química', 'Biologia', 'História', 'Geografia', 'Inglês'
    ],
    UNICAMP: [
        'Português e Literatura', 'Redação', 'Matemática', 'Física',
        'Química', 'Biologia', 'História', 'Geografia', 'Inglês', 'Interdisciplinares'
    ],
    UNESP: [
        'Português e Literatura', 'Redação', 'Matemática', 'Física',
        'Química', 'Biologia', 'História', 'Geografia', 'Filosofia e Sociologia', 'Inglês'
    ],
    OUTROS: [
        'Matemática', 'Português', 'Redação', 'Física',
        'Química', 'Biologia', 'História', 'Geografia', 'Inglês', 'Atualidades'
    ]
};

const formRotina = document.getElementById('form-rotina');
const botoesTurno = document.querySelectorAll('.btn-turno');
const containerMaterias = document.getElementById('lista-materias');
const exameAtualEl = document.getElementById('exame-atual');
const contadorTurnosEl = document.getElementById('contador-turnos');
const contadorMateriasEl = document.getElementById('contador-materias');
const erroEl = document.getElementById('rotina-erro');
const sucessoEl = document.getElementById('rotina-sucesso');
const btnSalvar = document.getElementById('btn-salvar-rotina');
const btnContinuar = document.getElementById('btn-continuar');
const btnVoltar = document.getElementById('btn-voltar');
const barraProgresso = document.getElementById('barra-progresso');
const etapas = document.querySelectorAll('.etapa');
const passos = document.querySelectorAll('.passo');
const botoesDia = document.querySelectorAll('.btn-dia');
const TURNOS = ['MANHA', 'TARDE', 'NOITE'];

let vestibularAtual = (sessionStorage.getItem(CHAVE_EXAME) || 'ENEM').toUpperCase();
if (!CATALOGO_MATERIAS[vestibularAtual]) {
    vestibularAtual = 'ENEM';
}

// Estado da seleção
const turnosSelecionados = new Set(); // formato "dia:turno", ex: "1:MANHA"
let materiasPrioritarias = [];        // array ordenado por prioridade
let etapaAtual = 1;
let salvando = false;

function limparMensagens() {
    erroEl.textContent = '';
    sucessoEl.textContent = '';
}

function irParaEtapa(numero) {
    const direcao = numero > etapaAtual ? 'entra-direita' : 'entra-esquerda';
    etapaAtual = numero;
    limparMensagens();

    etapas.forEach(secao => {
        const ativa = Number(secao.dataset.etapa) === numero;
        secao.hidden = !ativa;
        secao.classList.remove('entra-direita', 'entra-esquerda');
        if (ativa) {
            void secao.offsetWidth; // reinicia a animação
            secao.classList.add(direcao);
            secao.querySelector('h2').focus({ preventScroll: true });
        }
    });

    passos.forEach(passo => {
        const n = Number(passo.dataset.passo);
        passo.classList.toggle('ativo', n === numero);
        passo.classList.toggle('concluido', n < numero);
        if (n === numero) passo.setAttribute('aria-current', 'step');
        else passo.removeAttribute('aria-current');
    });

    barraProgresso.style.width = `${(numero / etapas.length) * 100}%`;
    btnVoltar.hidden = numero === 1;
    btnContinuar.hidden = numero !== 1;
    btnSalvar.hidden = numero !== etapas.length;
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

function atualizarBotoes() {
    btnContinuar.disabled = turnosSelecionados.size === 0;
    btnSalvar.disabled = salvando || materiasPrioritarias.length === 0;
}

function chaveTurno(diaSemana, turno) {
    return `${diaSemana}:${turno}`;
}

function renderizarMaterias() {
    const lista = CATALOGO_MATERIAS[vestibularAtual] || CATALOGO_MATERIAS.ENEM;
    // Inclui matérias que já estavam salvas no perfil mesmo se o usuário trocou de exame
    const todasMaterias = [...new Set([...lista, ...materiasPrioritarias])];

    containerMaterias.innerHTML = '';
    todasMaterias.forEach(materia => {
        const indice = materiasPrioritarias.indexOf(materia);
        const selecionada = indice !== -1;

        const btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'btn-materia' + (selecionada ? ' selecionada' : '');
        btn.setAttribute('aria-pressed', String(selecionada));
        btn.dataset.materia = materia;

        const nomeSpan = document.createElement('span');
        nomeSpan.textContent = materia;

        const ordemSpan = document.createElement('span');
        ordemSpan.className = 'ordem-prioridade';
        ordemSpan.textContent = selecionada ? `${indice + 1}º` : '+';

        btn.append(nomeSpan, ordemSpan);
        btn.addEventListener('click', () => alternarMateria(materia));
        containerMaterias.appendChild(btn);
    });

    const totalM = materiasPrioritarias.length;
    contadorMateriasEl.textContent = totalM === 1
        ? '1 matéria selecionada'
        : `${totalM} matérias selecionadas`;
    atualizarBotoes();
}

function atualizarTurnosNaTela() {
    botoesTurno.forEach(btn => {
        const chave = chaveTurno(Number(btn.dataset.dia), btn.dataset.turno);
        const ativo = turnosSelecionados.has(chave);
        btn.classList.toggle('selecionado', ativo);
        btn.setAttribute('aria-pressed', String(ativo));
    });

    const totalT = turnosSelecionados.size;
    contadorTurnosEl.textContent = totalT === 1
        ? '1 turno selecionado'
        : `${totalT} turnos selecionados`;
    atualizarBotoes();
}

function alternarMateria(materia) {
    limparMensagens();
    const idx = materiasPrioritarias.indexOf(materia);
    if (idx === -1) {
        materiasPrioritarias.push(materia);
    } else {
        materiasPrioritarias.splice(idx, 1);
    }
    renderizarMaterias();
}

botoesTurno.forEach(btn => {
    btn.addEventListener('click', () => {
        limparMensagens();
        const chave = chaveTurno(Number(btn.dataset.dia), btn.dataset.turno);
        if (turnosSelecionados.has(chave)) {
            turnosSelecionados.delete(chave);
        } else {
            turnosSelecionados.add(chave);
        }
        atualizarTurnosNaTela();
    });
});

botoesDia.forEach(btn => {
    btn.addEventListener('click', () => {
        limparMensagens();
        const dia = Number(btn.dataset.dia);
        const chaves = TURNOS.map(t => chaveTurno(dia, t));
        const todosMarcados = chaves.every(c => turnosSelecionados.has(c));
        chaves.forEach(c => (todosMarcados ? turnosSelecionados.delete(c) : turnosSelecionados.add(c)));
        atualizarTurnosNaTela();
    });
});

btnContinuar.addEventListener('click', () => {
    if (turnosSelecionados.size === 0) {
        erroEl.textContent = 'Selecione pelo menos um turno livre na semana.';
        return;
    }
    irParaEtapa(2);
});

btnVoltar.addEventListener('click', () => irParaEtapa(1));

async function carregarRotinaSalva() {
    exameAtualEl.textContent = vestibularAtual;
    renderizarMaterias();
    atualizarTurnosNaTela();

    try {
        const response = await requisicaoAutenticada(ROTINA_API_URL);
        if (response.status === 404 || response.status === 204) return;
        if (!response.ok) return;

        const rotina = await response.json();
        if (!rotina) return;

        // Se o estudante não acabou de trocar de exame na tela anterior, usa o vestibular salvo no banco
        if (!sessionStorage.getItem(CHAVE_EXAME) && rotina.vestibular) {
            vestibularAtual = rotina.vestibular.toUpperCase();
            sessionStorage.setItem(CHAVE_EXAME, vestibularAtual.toLowerCase());
            exameAtualEl.textContent = vestibularAtual;
        }

        if (Array.isArray(rotina.disponibilidade)) {
            turnosSelecionados.clear();
            rotina.disponibilidade.forEach(item => {
                turnosSelecionados.add(chaveTurno(item.diaSemana, item.turno));
            });
            atualizarTurnosNaTela();
        }

        if (Array.isArray(rotina.materiasPrioritarias)) {
            materiasPrioritarias = [...rotina.materiasPrioritarias];
            renderizarMaterias();
        }
    } catch {
        // Primeiro acesso ou API offline: mantém formulário limpo para preenchimento
    }
}

formRotina.addEventListener('submit', async e => {
    e.preventDefault();
    limparMensagens();

    if (turnosSelecionados.size === 0) {
        irParaEtapa(1);
        erroEl.textContent = 'Selecione pelo menos um turno livre na semana.';
        return;
    }

    if (etapaAtual === 1) {
        irParaEtapa(2);
        return;
    }

    if (materiasPrioritarias.length === 0) {
        erroEl.textContent = 'Selecione pelo menos uma matéria prioritária.';
        return;
    }

    const disponibilidade = Array.from(turnosSelecionados).map(chave => {
        const [diaSemana, turno] = chave.split(':');
        return { diaSemana: Number(diaSemana), turno };
    });

    salvando = true;
    atualizarBotoes();
    try {
        const response = await requisicaoAutenticada(ROTINA_API_URL, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                vestibular: vestibularAtual,
                disponibilidade,
                materiasPrioritarias
            })
        });

        if (!response.ok) {
            throw new Error(await mensagemDeErro(response));
        }

        sucessoEl.textContent = 'Rotina salva com sucesso!';
    } catch (falha) {
        erroEl.textContent = falha.message || 'Não foi possível salvar a rotina na API.';
    } finally {
        salvando = false;
        atualizarBotoes();
    }
});

carregarRotinaSalva();