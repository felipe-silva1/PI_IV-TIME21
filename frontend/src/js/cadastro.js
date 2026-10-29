const formCadastro = document.getElementById('form-cadastro');
const emailValido = v => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v);

function marcar(campos, ruim) {
  campos.forEach(c => c.setAttribute('aria-invalid', c === ruim ? 'true' : 'false'));
}


formCadastro.addEventListener('submit', async e => {
  e.preventDefault();
  const [nome, email, senha, conf] = formCadastro.querySelectorAll('input');
  const erro = document.getElementById('c-erro');
  const ok = document.getElementById('c-ok');
  ok.textContent = '';
  let msg = '', ruim = null;

  if (!nome.value.trim()) { msg = 'Informe seu nome.'; ruim = nome; }
  else if (!emailValido(email.value)) { msg = 'Digite um e-mail válido.'; ruim = email; }
  else if (senha.value.length < 8) { msg = 'A senha precisa ter pelo menos 8 caracteres.'; ruim = senha; }
  else if (senha.value !== conf.value) { msg = 'As senhas não são iguais.'; ruim = conf; }

  marcar([nome, email, senha, conf], ruim);
  erro.textContent = msg;
  if (ruim) return ruim.focus();

  
  const submit = formCadastro.querySelector('button[type="submit"]');
  submit.disabled = true;
  try {
    const response = await fetch(`${AUTH_API_URL}/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        nome: nome.value.trim(),
        email: email.value.trim(),
        senha: senha.value
      })
    });
    if (!response.ok) throw new Error(await mensagemDeErro(response));
    guardarSessao(await response.json());
    ok.textContent = 'Conta criada! Redirecionando…';
    location.href = 'escolha-exame.html';
  } catch (falha) {
    erro.textContent = falha.message || 'Não foi possível conectar à API.';
  } finally {
    submit.disabled = false;
  }
});
