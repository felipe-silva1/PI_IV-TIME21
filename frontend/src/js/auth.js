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
  const body = await response.json();
  return body.message || 'Não foi possível concluir a operação.';
}
