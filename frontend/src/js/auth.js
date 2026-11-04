const AUTH_API_URL = 'http://localhost:8080/api/auth';
const AUTH_TOKEN_KEY = 'guiavest.sessionToken';

function guardarSessao(sessao) {
  sessionStorage.setItem(AUTH_TOKEN_KEY, sessao.token);
  sessionStorage.setItem('guiavest.usuarioId', sessao.usuarioId);
  sessionStorage.setItem('guiavest.nome', sessao.nome);
}

function limparSessao() {
  sessionStorage.removeItem(AUTH_TOKEN_KEY);
  sessionStorage.removeItem('guiavest.usuarioId');
  sessionStorage.removeItem('guiavest.nome');
}

async function requisicaoAutenticada(url, options = {}) {
  const headers = new Headers(options.headers || {});
  const token = sessionStorage.getItem(AUTH_TOKEN_KEY);
  if (token) headers.set('Authorization', `Bearer ${token}`);
  return fetch(url, { ...options, headers });
}

async function mensagemDeErro(response) {
  try {
    const body = await response.json();
    return body.message || 'Não foi possível concluir a operação.';
  } catch {
    return 'Não foi possível concluir a operação.';
  }
}

async function exigirSessao() {
  if (!sessionStorage.getItem(AUTH_TOKEN_KEY)) {
    location.replace('../login.html');
    return;
  }

  try {
    const response = await requisicaoAutenticada(`${AUTH_API_URL}/session`);
    if (response.status === 401) {
      limparSessao();
      location.replace('../login.html');
      return;
    }
    if (!response.ok) {
      throw new Error(await mensagemDeErro(response));
    }
  } catch (falha) {
    document.body.replaceChildren(
      Object.assign(document.createElement('p'), {
        role: 'alert',
        textContent: falha.message || 'Não foi possível validar sua sessão.'
      })
    );
  }
}
